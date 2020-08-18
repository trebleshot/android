/*
 * Copyright (C) 2020 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.task;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.exception.ConnectionNotFoundException;
import com.genonbeta.TrebleShot.exception.DeviceNotFoundException;
import com.genonbeta.TrebleShot.exception.MemberNotFoundException;
import com.genonbeta.TrebleShot.exception.TransferNotFoundException;
import com.genonbeta.TrebleShot.fragment.FileListFragment;
import com.genonbeta.TrebleShot.object.*;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException;
import com.genonbeta.TrebleShot.util.CommonErrorHelper;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.Transfers;
import com.genonbeta.android.database.exception.ReconstructionFailedException;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.io.LocalDocumentFile;
import com.genonbeta.android.framework.io.StreamInfo;
import org.json.JSONObject;
import org.monora.coolsocket.core.response.Response;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.*;
import java.util.List;

import static com.genonbeta.TrebleShot.object.Identifier.from;

public class FileTransferTask extends AttachableAsyncTask<AttachedTaskListener>
{
    public static final String TAG = FileTransferTask.class.getSimpleName();

    // Static objects
    public ActiveConnection activeConnection;
    public Device device;
    public IndexOfTransferGroup index;
    public Transfer transfer;
    public TransferMember member;
    public List<DeviceAddress> addressList;
    public TransferItem.Type type;

    // Changing objects
    public TransferItem object;
    public DocumentFile currentFile;
    public long lastProcessingTime;
    public long currentBytes; // moving
    public long lastKnownBytes; // completedBytes of 2 secs ago
    public long completedBytes;
    public long timeStarted; // TODO: 14.03.2020 Define this when the task begins
    public int completedCount;

    // Informative objects
    public boolean recoverInterruptions = false;
    public int attemptsLeft = 2;

    private long mTimeTransactionSaved;
    private SQLiteDatabase mDatabase;

    @Override
    protected void onRun() throws TaskStoppedException
    {
        if (this.activeConnection == null)
            startTransferAsClient();
        else if (TransferItem.Type.OUTGOING.equals(type))
            handleTransferAsSender();
        else if (TransferItem.Type.INCOMING.equals(type))
            handleTransferAsReceiver();
    }

    private void broadcastTransferState(boolean isLast)
    {
        long time = System.currentTimeMillis();
        boolean delayReached = time - lastProcessingTime > AppConfig.DELAY_DEFAULT_NOTIFICATION;

        if (delayReached && !isLast) {
            this.lastProcessingTime = time;

            try {
                getNotificationHelper().notifyFileTransfer(this);

                TransferItem.Flag flag = TransferItem.Flag.IN_PROGRESS;
                flag.setBytesValue(this.currentBytes);

                if (TransferItem.Type.INCOMING.equals(this.type))
                    this.object.setFlag(flag);
                else if (TransferItem.Type.OUTGOING.equals(this.type))
                    this.object.putFlag(this.device.uid, flag);

                kuick().update(getDatabase(), this.object, this.transfer, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (delayReached || isLast)
            kuick().broadcast();
    }

    public static FileTransferTask createFrom(Kuick kuick, long transferId, String deviceId, TransferItem.Type type)
            throws TransferNotFoundException, DeviceNotFoundException, ConnectionNotFoundException,
            MemberNotFoundException
    {
        SQLiteDatabase db = kuick.getReadableDatabase();
        Device device = new Device(deviceId);

        try {
            kuick.reconstruct(db, device);
        } catch (ReconstructionFailedException e) {
            throw new DeviceNotFoundException(device);
        }

        Transfer transfer = new Transfer(transferId);

        try {
            kuick.reconstruct(db, transfer);
        } catch (ReconstructionFailedException e) {
            throw new TransferNotFoundException(transfer);
        }

        return createFrom(kuick, transfer, device, type);
    }

    public static FileTransferTask createFrom(Kuick kuick, Transfer transfer, Device device, TransferItem.Type type)
            throws MemberNotFoundException, ConnectionNotFoundException
    {
        SQLiteDatabase db = kuick.getReadableDatabase();
        TransferMember member = new TransferMember(transfer, device, type);

        try {
            kuick.reconstruct(db, member);
        } catch (ReconstructionFailedException e) {
            throw new MemberNotFoundException(member);
        }

        List<DeviceAddress> addressList = Transfers.getAddressListFor(kuick, device.uid);

        Log.d(TAG, "createFrom: deviceId=" + device.uid + " transferId=" + transfer.id + " adapter=");

        FileTransferTask task = new FileTransferTask();
        task.type = type;
        task.device = device;
        task.transfer = transfer;
        task.member = member;
        task.addressList = addressList;
        task.index = new IndexOfTransferGroup(transfer);

        return task;
    }

    @Override
    public void forceQuit()
    {
        super.forceQuit();

        try {
            if (activeConnection != null && activeConnection.getSocket() != null)
                activeConnection.getSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getCurrentContent()
    {
        return object == null ? super.getCurrentContent() : object.name;
    }

    private SQLiteDatabase getDatabase()
    {
        if (mDatabase == null)
            mDatabase = kuick().getWritableDatabase();
        return mDatabase;
    }

    @Override
    public Identity getIdentity()
    {
        return identityOf(this);
    }

    @Override
    public String getName(Context context)
    {
        return context.getString(R.string.text_transfer);
    }

    private void handleTransferAsReceiver()
    {
        boolean retry = false;

        try {
            Transfers.loadGroupInfo(getContext(), this.index, this.member);

            while (this.activeConnection.getSocket().isConnected()) {
                this.currentBytes = 0;
                if (isInterrupted())
                    break;

                try {
                    TransferItem object = Transfers.fetchFirstValidIncomingTransferItem(getContext(), this.transfer.id);

                    if (object == null) {
                        Log.d(TAG, "handleTransferAsReceiver(): Exiting because there is no pending file " +
                                "instance left");
                        break;
                    } else
                        Log.d(TAG, "handleTransferAsReceiver(): Starting to receive " + object);

                    this.object = object;
                    this.currentFile = FileUtils.getIncomingFile(getContext(), this.object, this.transfer);
                    StreamInfo streamInfo = StreamInfo.getStreamInfo(getContext(), this.currentFile.getUri());
                    this.currentBytes = this.currentFile.length();
                    broadcastTransferState(false);

                    {
                        JSONObject reply = new JSONObject()
                                .put(Keyword.TRANSFER_REQUEST_ID, this.object.id)
                                .put(Keyword.RESULT, true);

                        if (this.currentBytes > 0)
                            reply.put(Keyword.SKIPPED_BYTES, this.currentBytes);

                        Log.d(TAG, "handleTransferAsReceiver(): reply: " + reply.toString());
                        this.activeConnection.reply(reply.toString());
                    }

                    {
                        JSONObject response = this.activeConnection.receive().getAsJson();
                        Log.d(TAG, "handleTransferAsReceiver(): receive: " + response.toString());

                        if (!response.getBoolean(Keyword.RESULT)) {
                            if (response.has(Keyword.TRANSFER_JOB_DONE)
                                    && !response.getBoolean(Keyword.TRANSFER_JOB_DONE)) {
                                interrupt(true);
                                Log.d(TAG, "handleTransferAsReceiver(): Transfer should be closed, babe!");
                                break;
                            } else if (response.has(Keyword.FLAG)
                                    && Keyword.FLAG_GROUP_EXISTS.equals(response.getString(Keyword.FLAG))) {
                                if (response.has(Keyword.ERROR)
                                        && response.getString(Keyword.ERROR).equals(Keyword.ERROR_NOT_FOUND)) {
                                    this.object.setFlag(TransferItem.Flag.REMOVED);
                                    Log.d(TAG, "handleTransferAsReceiver(): Sender says it does not have the " +
                                            "file defined");
                                } else if (response.has(Keyword.ERROR)
                                        && response.getString(Keyword.ERROR).equals(Keyword.ERROR_NOT_ACCESSIBLE)) {
                                    this.object.setFlag(TransferItem.Flag.INTERRUPTED);
                                    Log.d(TAG, "handleTransferAsReceiver(): Sender says it can't open the file");
                                } else if (response.has(Keyword.ERROR)
                                        && response.getString(Keyword.ERROR).equals(Keyword.ERROR_UNKNOWN)) {
                                    this.object.setFlag(TransferItem.Flag.INTERRUPTED);
                                    Log.d(TAG, "handleTransferAsReceiver(): Sender says an unknown error occurred");
                                }
                            }
                        } else {
                            long sizeChanged = response.has(Keyword.SIZE_CHANGED) ? response.getLong(
                                    Keyword.SIZE_CHANGED) : 0;
                            boolean sizeActuallyChanged = sizeChanged > 0 && this.object.size != sizeChanged;

                            if (sizeActuallyChanged) {
                                Log.d(TAG, "handleTransferAsReceiver(): Sender says the file has a new size");
                                this.object.size = response.getLong(Keyword.SIZE_CHANGED);
                                boolean canContinue = this.currentBytes < 1;

                                this.activeConnection.reply(new JSONObject()
                                        .put(Keyword.RESULT, canContinue)
                                        .toString());

                                if (!canContinue) {
                                    Log.d(TAG, "handleTransferAsReceiver(): The change may broke the previous " +
                                            "file which has a length. Cannot take the risk.");
                                    this.object.setFlag(TransferItem.Flag.REMOVED);
                                    continue;
                                }

                                Log.d(TAG, "handleTransferAsReceiver(): receive: " +
                                        this.activeConnection.receive().getAsString());
                            }

                            OutputStream outputStream = null;
                            boolean completed = false;

                            try {
                                outputStream = streamInfo.openOutputStream();
                                int readLength;
                                byte[] buffer = new byte[AppConfig.BUFFER_LENGTH_DEFAULT];
                                ActiveConnection.Description description = activeConnection.readBegin();

                                while ((readLength = activeConnection.read(description)) != -1) {
                                    this.currentBytes += readLength;
                                    outputStream.write(buffer, 0, readLength);

                                    broadcastTransferState(false);

                                    if (isInterrupted()) {
                                        this.object.setFlag(TransferItem.Flag.INTERRUPTED);
                                        break;
                                    }
                                }

                                outputStream.flush();

                                completed = this.currentBytes == this.object.size;
                                this.object.setFlag(completed ? TransferItem.Flag.DONE
                                        : TransferItem.Flag.INTERRUPTED);

                                Log.d(TAG, "handleTransferAsSender(): File received " + this.object.name);
                            } catch (Exception e) {
                                e.printStackTrace();
                                interrupt(false);
                                this.object.setFlag(TransferItem.Flag.INTERRUPTED);
                            } finally {
                                if (outputStream != null)
                                    outputStream.close();
                            }

                            try {
                                if (completed) {
                                    this.completedBytes += this.currentBytes;
                                    this.completedCount++;

                                    if (this.currentFile.getParentFile() != null) {
                                        this.currentFile = FileUtils.saveReceivedFile(this.currentFile.getParentFile(),
                                                this.currentFile, this.object);

                                        Log.d(TAG, "handleTransferAsReceiver(): The file is "
                                                + this.currentFile.getUri().toString()
                                                + " and the name is " + this.object.file);

                                        getContext().sendBroadcast(new Intent(FileListFragment.ACTION_FILE_LIST_CHANGED)
                                                .putExtra(FileListFragment.EXTRA_FILE_PARENT,
                                                        this.currentFile.getParentFile().getUri())
                                                .putExtra(FileListFragment.EXTRA_FILE_NAME,
                                                        this.currentFile.getName()));
                                    }

                                    if (this.currentFile instanceof LocalDocumentFile
                                            && getMediaScanner().isConnected())
                                        getMediaScanner().scanFile(((LocalDocumentFile) this.currentFile)
                                                .getFile().getAbsolutePath(), this.object.mimeType);
                                }
                            } catch (Exception ignored) {
                                Log.e(TAG, "Error occurred during completion of the transfer");
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    retry = true;

                    if (!this.recoverInterruptions) {
                        Transfers.recoverIncomingInterruptions(getContext(), this.transfer.id);
                        this.recoverInterruptions = true;
                    }

                    break;
                } finally {
                    if (this.object != null) {
                        Log.d(TAG, "handleTransferAsReceiver(): Updating file instances to "
                                + this.object.getFlag().toString());
                        kuick().update(getDatabase(), this.object, this.transfer, null);
                    }
                }
            }

            try {
                DocumentFile savePath = FileUtils.getSavePath(getContext(), this.transfer);
                boolean areFilesDone = kuick().getFirstFromTable(getDatabase(),
                        Transfers.createIncomingSelection(this.transfer.id, TransferItem.Flag.DONE,
                                false)) == null;
                boolean jobDone = !isInterrupted() && areFilesDone;

                this.activeConnection.reply(new JSONObject()
                        .put(Keyword.RESULT, false)
                        .put(Keyword.TRANSFER_JOB_DONE, jobDone)
                        .toString());
                Log.d(TAG, "handleTransferAsReceiver(): reply: done ?? " + jobDone);

                if (!retry)
                    if (isInterruptedByUser()) {
                        Log.d(TAG, "handleTransferAsReceiver(): Removing notification an error is already " +
                                "notified");
                    } else if (isInterrupted()) {
                        getNotificationHelper().notifyReceiveError(this);
                        Log.d(TAG, "handleTransferAsReceiver(): Some files was not received");
                    } else if (this.completedCount > 0) {
                        getNotificationHelper().notifyFileReceived(this, savePath);
                        Log.d(TAG, "handleTransferAsReceiver(): Notify user");
                    }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            broadcastTransferState(true);

            Log.d(TAG, "We have exited");

            if (retry && this.attemptsLeft > 0 && !isInterruptedByUser()) {
                try {
                    startTransferAsClient();
                    this.attemptsLeft--;
                } catch (Exception e) {
                    Log.d(TAG, "handleTransferAsReceiver(): Restart is requested, but transfer" +
                            " instance failed to reconstruct");
                }
            }
        }
    }

    private void handleTransferAsSender()
    {
        try {
            Transfers.loadGroupInfo(getContext(), this.index, this.member);

            while (this.activeConnection.getSocket().isConnected()) {
                this.currentBytes = 0;
                Response response = this.activeConnection.receive();
                Log.d(TAG, "handleTransferAsSender(): receive: " + response.getAsString());
                JSONObject request = response.getAsJson();

                if (request.has(Keyword.RESULT) && !request.getBoolean(Keyword.RESULT)) {
                    if (request.has(Keyword.TRANSFER_JOB_DONE) && !request.getBoolean(Keyword.TRANSFER_JOB_DONE))
                        interrupt(true);

                    Log.d(TAG, "handleTransferAsSender(): Receiver notified that the transfer " +
                            "has stopped with interruption=" + isInterrupted());
                    return;
                } else if (isInterrupted()) {
                    this.activeConnection.reply(new JSONObject()
                            .put(Keyword.RESULT, false)
                            .put(Keyword.TRANSFER_JOB_DONE, false)
                            .toString());

                    Log.d(TAG, "handleTransferAsSender(): Exiting because the interruption has been triggered");

                    // Wait for the next response to ensure no error occurs.
                    continue;
                }

                try {
                    Log.d(TAG, "handleTransferAsSender(): " + this.type.toString());

                    this.object = new TransferItem(this.transfer.id, request.getInt(Keyword.TRANSFER_REQUEST_ID),
                            this.type);

                    kuick().reconstruct(getDatabase(), this.object);

                    this.currentFile = FileUtils.fromUri(getContext(), Uri.parse(this.object.file));
                    long fileSize = this.currentFile.length();
                    InputStream inputStream = getContext().getContentResolver()
                            .openInputStream(this.currentFile.getUri());

                    if (inputStream == null)
                        throw new FileNotFoundException("The input stream for the file has failed to open.");

                    broadcastTransferState(false);

                    if (request.has(Keyword.SKIPPED_BYTES)) {
                        long skippedBytes = request.getLong(Keyword.SKIPPED_BYTES);
                        long newPosition = inputStream.skip(skippedBytes);

                        Log.d(TAG, "handleTransferAsSender(): Has skipped bytes: " + skippedBytes);

                        if (skippedBytes > 0 && newPosition != skippedBytes) {
                            inputStream.close();
                            throw new IOException("Failed to skip bytes. The requested is " + skippedBytes
                                    + " and the result is " + newPosition);
                        }
                    }

                    JSONObject reply = new JSONObject()
                            .put(Keyword.RESULT, true);

                    if (fileSize != this.object.size) {
                        this.object.size = fileSize;

                        reply.put(Keyword.SIZE_CHANGED, fileSize);
                        this.activeConnection.reply(reply.toString());
                        Log.d(TAG, "handleTransferAsSender(): reply: " + reply.toString());

                        JSONObject validityOfChange = activeConnection.receive().getAsJson();

                        if (!validityOfChange.has(Keyword.RESULT) || !validityOfChange.getBoolean(Keyword.RESULT)) {
                            this.object.putFlag(this.device.uid, TransferItem.Flag.INTERRUPTED);
                            kuick().update(getDatabase(), this.object, this.transfer, null);
                            continue;
                        }
                    } else {
                        this.activeConnection.reply(reply.toString());
                        Log.d(TAG, "handleTransferAsSender(): reply: " + reply.toString());
                    }

                    this.object.putFlag(this.device.uid, TransferItem.Flag.IN_PROGRESS);
                    kuick().update(getDatabase(), this.object, this.transfer, null);

                    try {
                        boolean sizeExceeded = false;
                        int readLength;
                        ActiveConnection.Description description = this.activeConnection.writeBegin(0, fileSize);

                        while ((readLength = inputStream.read(description.buffer)) != -1) {
                            if (readLength > 0) {
                                broadcastTransferState(false);

                                this.currentBytes += readLength;
                                this.activeConnection.write(description, 0, readLength);
                            }

                            if (isInterrupted()) {
                                this.object.putFlag(this.device.uid, TransferItem.Flag.INTERRUPTED);
                                break;
                            }
                        }

                        this.activeConnection.writeEnd(description);

                        if (this.currentBytes == this.object.size) {
                            this.completedBytes += this.currentBytes;
                            this.completedCount++;
                            this.object.putFlag(this.device.uid, TransferItem.Flag.DONE);
                        } else if (sizeExceeded)
                            this.object.putFlag(this.device.uid, TransferItem.Flag.REMOVED);
                        else
                            this.object.putFlag(this.device.uid, TransferItem.Flag.INTERRUPTED);

                        kuick().update(getDatabase(), this.object, this.transfer, null);

                        Log.d(TAG, "handleTransferAsSender(): File sent " + this.object.name);
                    } catch (Exception e) {
                        e.printStackTrace();
                        interrupt(false);
                        this.object.putFlag(this.device.uid, TransferItem.Flag.INTERRUPTED);
                        kuick().update(getDatabase(), this.object, this.transfer, null);
                    } finally {
                        inputStream.close();
                    }
                } catch (ReconstructionFailedException e) {
                    Log.d(TAG, "handleTransferAsSender(): File not found");

                    this.activeConnection.reply(new JSONObject()
                            .put(Keyword.RESULT, false)
                            .put(Keyword.ERROR, Keyword.ERROR_NOT_FOUND)
                            .put(Keyword.FLAG, Keyword.FLAG_GROUP_EXISTS)
                            .toString());

                    this.object.putFlag(this.device.uid, TransferItem.Flag.REMOVED);
                    kuick().update(getDatabase(), this.object, this.transfer, null);
                } catch (FileNotFoundException | StreamCorruptedException e) {
                    Log.d(TAG, "handleTransferAsSender(): File is not accessible ? " + this.object.name);

                    this.activeConnection.reply(new JSONObject()
                            .put(Keyword.RESULT, false)
                            .put(Keyword.ERROR, Keyword.ERROR_NOT_ACCESSIBLE)
                            .put(Keyword.FLAG, Keyword.FLAG_GROUP_EXISTS)
                            .toString());

                    this.object.putFlag(this.device.uid, TransferItem.Flag.INTERRUPTED);
                    kuick().update(getDatabase(), this.object, this.transfer, null);
                } catch (Exception e) {
                    e.printStackTrace();

                    this.activeConnection.reply(new JSONObject()
                            .put(Keyword.RESULT, false)
                            .put(Keyword.ERROR, Keyword.ERROR_UNKNOWN)
                            .put(Keyword.FLAG, Keyword.FLAG_GROUP_EXISTS)
                            .toString());

                    this.object.putFlag(this.device.uid, TransferItem.Flag.INTERRUPTED);
                    kuick().update(getDatabase(), this.object, this.transfer, null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            interrupt();
        } finally {
            if (isInterrupted() && !isInterruptedByUser())
                getNotificationHelper().notifyConnectionError(this, null);

            broadcastTransferState(true);
        }
    }

    public static Identity identityOf(FileTransferTask task)
    {
        return identifyWith(task.transfer.id, task.device.uid, task.type);
    }

    public static Identity identifyWith(long transferId)
    {
        return Identity.withANDs(from(Id.TransferId, transferId));
    }

    public static Identity identifyWith(long transferId, TransferItem.Type type)
    {
        return Identity.withANDs(from(Id.TransferId, transferId), from(Id.Type, type));
    }

    public static Identity identifyWith(long transferId, String deviceId)
    {
        return Identity.withANDs(from(Id.TransferId, transferId), from(Id.DeviceId, deviceId));
    }

    public static Identity identifyWith(long transferId, String deviceId, TransferItem.Type type)
    {
        return Identity.withANDs(from(Id.TransferId, transferId), from(Id.DeviceId, deviceId), from(Id.Type, type));
    }

    public void startTransferAsClient() throws TaskStoppedException
    {
        try (CommunicationBridge bridge = CommunicationBridge.connect(kuick(), addressList, device, 0)) {
            bridge.requestFileTransferStart(this.transfer.id, this.type);

            if (bridge.receiveResult()) {
                this.activeConnection = bridge.getActiveConnection();
                this.attemptsLeft = 2;

                if (TransferItem.Type.INCOMING.equals(this.type)) {
                    handleTransferAsReceiver();
                } else if (TransferItem.Type.OUTGOING.equals(this.type)) {
                    handleTransferAsSender();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            post(CommonErrorHelper.messageOf(getContext(), e));
        }
    }

    public enum Id
    {
        TransferId,
        DeviceId,
        Type
    }
}
