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

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.exception.AssigneeNotFoundException;
import com.genonbeta.TrebleShot.exception.ConnectionNotFoundException;
import com.genonbeta.TrebleShot.exception.DeviceNotFoundException;
import com.genonbeta.TrebleShot.exception.TransferGroupNotFoundException;
import com.genonbeta.TrebleShot.fragment.FileListFragment;
import com.genonbeta.TrebleShot.object.*;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.service.backgroundservice.BackgroundTask;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.android.database.exception.ReconstructionFailedException;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.io.LocalDocumentFile;
import com.genonbeta.android.framework.io.StreamInfo;
import org.json.JSONObject;

import java.io.*;

public class FileTransferTask extends BackgroundTask
{
    public static final String TAG = FileTransferTask.class.getSimpleName();

    // Static objects
    public CoolSocket.ActiveConnection activeConnection;
    public NetworkDevice device;
    public PreloadedGroup group;
    public TransferAssignee assignee;
    public DeviceConnection connection;
    public TransferObject.Type type;

    // Changing objects
    public TransferObject object;
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
    protected void onRun() throws InterruptedException
    {
        if (this.activeConnection == null)
            startTransferAsClient();
        else if (TransferObject.Type.OUTGOING.equals(type))
            handleTransferAsSender();
        else if (TransferObject.Type.INCOMING.equals(type))
            handleTransferAsReceiver();
    }

    private void broadcastTransferState(boolean isLast)
    {
        long time = System.currentTimeMillis();

		/*if (isLast || time - mTimeTransactionSaved > AppConfig.DEFAULT_NOTIFICATION_DELAY) {
			mTimeTransactionSaved = time;

			if (getDbInstance().inTransaction()) {
				getDbInstance().setTransactionSuccessful();
				getDbInstance().endTransaction();
			}

			if (!isLast)
				getDbInstance().beginTransaction();
		}*/
        boolean delayReached = time - lastProcessingTime > AppConfig.DEFAULT_NOTIFICATION_DELAY;

        if (delayReached && !isLast) {
            this.lastProcessingTime = time;

            try {
                getNotificationHelper().notifyFileTransfer(this);

                TransferObject.Flag flag = TransferObject.Flag.IN_PROGRESS;
                flag.setBytesValue(this.currentBytes);

                if (TransferObject.Type.INCOMING.equals(this.type))
                    this.object.setFlag(flag);
                else if (TransferObject.Type.OUTGOING.equals(this.type))
                    this.object.putFlag(this.device.id, flag);

                kuick().update(getDatabase(), this.object, this.group, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (delayReached || isLast)
            kuick().broadcast();
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

    @Override
    public String getDescription()
    {
        return null;
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
    public String getTitle()
    {
        return null;
    }

    private void handleTransferAsReceiver()
    {
        boolean retry = false;

        try {
            TransferUtils.loadGroupInfo(getService(), this.group, this.assignee);

            while (this.activeConnection.getSocket().isConnected()) {
                this.currentBytes = 0;
                if (isInterrupted())
                    break;

                try {
                    TransferObject object = TransferUtils.fetchFirstValidIncomingTransfer(getService(), this.group.id);

                    if (object == null) {
                        Log.d(TAG, "handleTransferAsReceiver(): Exiting because there is no pending file " +
                                "instance left");
                        break;
                    } else
                        Log.d(TAG, "handleTransferAsReceiver(): Starting to receive " + object);

                    this.object = object;
                    this.currentFile = FileUtils.getIncomingFile(getService(), this.object, this.group);
                    StreamInfo streamInfo = StreamInfo.getStreamInfo(getService(), this.currentFile.getUri());
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
                        JSONObject response = new JSONObject(this.activeConnection.receive().response);
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
                                    this.object.setFlag(TransferObject.Flag.REMOVED);
                                    Log.d(TAG, "handleTransferAsReceiver(): Sender says it does not have the " +
                                            "file defined");
                                } else if (response.has(Keyword.ERROR)
                                        && response.getString(Keyword.ERROR).equals(Keyword.ERROR_NOT_ACCESSIBLE)) {
                                    this.object.setFlag(TransferObject.Flag.INTERRUPTED);
                                    Log.d(TAG, "handleTransferAsReceiver(): Sender says it can't open the file");
                                } else if (response.has(Keyword.ERROR)
                                        && response.getString(Keyword.ERROR).equals(Keyword.ERROR_UNKNOWN)) {
                                    this.object.setFlag(TransferObject.Flag.INTERRUPTED);
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
                                    this.object.setFlag(TransferObject.Flag.REMOVED);
                                    continue;
                                }

                                Log.d(TAG, "handleTransferAsReceiver(): receive: " +
                                        this.activeConnection.receive().response);
                            }

                            this.activeConnection.reply(Keyword.STUB);
                            OutputStream outputStream = null;
                            boolean completed = false;

                            try {
                                outputStream = streamInfo.openOutputStream();
                                int readLength;
                                long lastReceivedTime = 0;
                                long timeout = this.activeConnection.getTimeout();
                                byte[] buffer = new byte[AppConfig.BUFFER_LENGTH_DEFAULT];
                                InputStream inputStream = this.activeConnection.getSocket()
                                        .getInputStream();

                                while (this.currentBytes < this.object.size) {
                                    if ((readLength = inputStream.read(buffer)) > 0) {
                                        this.currentBytes += readLength;
                                        outputStream.write(buffer, 0, readLength);
                                        outputStream.flush();

                                        lastReceivedTime = System.currentTimeMillis();
                                    }

                                    broadcastTransferState(false);

                                    if (isInterrupted()) {
                                        this.object.setFlag(TransferObject.Flag.INTERRUPTED);
                                        break;
                                    }

                                    if (timeout != CoolSocket.NO_TIMEOUT
                                            && System.currentTimeMillis() - lastReceivedTime > timeout)
                                        break;
                                }

                                completed = this.currentBytes == this.object.size;
                                this.object.setFlag(completed ? TransferObject.Flag.DONE
                                        : TransferObject.Flag.INTERRUPTED);

                                Log.d(TAG, "handleTransferAsSender(): File received " + this.object.name);
                            } catch (Exception e) {
                                e.printStackTrace();
                                interrupt(false);
                                this.object.setFlag(TransferObject.Flag.INTERRUPTED);
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

                                        getService().sendBroadcast(new Intent(FileListFragment.ACTION_FILE_LIST_CHANGED)
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
                        TransferUtils.recoverIncomingInterruptions(getService(), this.group.id);
                        this.recoverInterruptions = true;
                    }

                    break;
                } finally {
                    if (this.object != null) {
                        Log.d(TAG, "handleTransferAsReceiver(): Updating file instances to "
                                + this.object.getFlag().toString());
                        kuick().update(getDatabase(), this.object, this.group, null);
                    }
                }
            }

            try {
                DocumentFile savePath = FileUtils.getSavePath(getService(), this.group);
                boolean areFilesDone = kuick().getFirstFromTable(getDatabase(),
                        TransferUtils.createIncomingSelection(this.group.id, TransferObject.Flag.DONE,
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
                        getService().getNotificationHelper().notifyReceiveError(this);
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
            TransferUtils.loadGroupInfo(getService(), this.group, this.assignee);

            while (this.activeConnection.getSocket().isConnected()) {
                this.currentBytes = 0;
                CoolSocket.ActiveConnection.Response response = this.activeConnection.receive();
                Log.d(TAG, "handleTransferAsSender(): receive: " + response.response);
                JSONObject request = new JSONObject(response.response);

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

                    this.object = new TransferObject(this.group.id, request.getInt(Keyword.TRANSFER_REQUEST_ID),
                            this.type);

                    kuick().reconstruct(getDatabase(), this.object);

                    this.currentFile = FileUtils.fromUri(getService(), Uri.parse(this.object.file));
                    long fileSize = this.currentFile.length();
                    InputStream inputStream = getService().getContentResolver()
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

                        JSONObject validityOfChange = new JSONObject(this.activeConnection.receive().response);

                        if (!validityOfChange.has(Keyword.RESULT) || !validityOfChange.getBoolean(
                                Keyword.RESULT)) {
                            this.object.putFlag(this.device.id, TransferObject.Flag.INTERRUPTED);
                            kuick().update(getDatabase(), this.object, this.group, null);
                            continue;
                        }

                        this.activeConnection.reply(Keyword.STUB);
                    } else {
                        this.activeConnection.reply(reply.toString());
                        Log.d(TAG, "handleTransferAsSender(): reply: " + reply.toString());
                    }

                    this.activeConnection.receive();
                    this.object.putFlag(this.device.id, TransferObject.Flag.IN_PROGRESS);
                    kuick().update(getDatabase(), this.object, this.group, null);

                    try {
                        boolean sizeExceeded = false;
                        int readLength;
                        byte[] buffer = new byte[AppConfig.BUFFER_LENGTH_DEFAULT];
                        OutputStream outputStream = this.activeConnection.getSocket()
                                .getOutputStream();

                        while ((readLength = inputStream.read(buffer)) != -1
                                && this.currentBytes < this.object.size
                                && !this.activeConnection.getSocket().isOutputShutdown()) {
                            if (readLength > 0) {
                                if (this.currentBytes + readLength > this.object.size) {
                                    sizeExceeded = true;
                                    break;
                                }

                                broadcastTransferState(false);

                                this.currentBytes += readLength;
                                outputStream.write(buffer, 0, readLength);
                                outputStream.flush();
                            }

                            if (isInterrupted()) {
                                this.object.putFlag(this.device.id, TransferObject.Flag.INTERRUPTED);
                                break;
                            }
                        }

                        if (this.currentBytes == this.object.size) {
                            this.completedBytes += this.currentBytes;
                            this.completedCount++;
                            this.object.putFlag(this.device.id, TransferObject.Flag.DONE);
                        } else if (sizeExceeded)
                            this.object.putFlag(this.device.id, TransferObject.Flag.REMOVED);
                        else
                            this.object.putFlag(this.device.id, TransferObject.Flag.INTERRUPTED);

                        kuick().update(getDatabase(), this.object, this.group, null);

                        Log.d(TAG, "handleTransferAsSender(): File sent " + this.object.name);
                    } catch (Exception e) {
                        e.printStackTrace();
                        interrupt(false);
                        this.object.putFlag(this.device.id, TransferObject.Flag.INTERRUPTED);
                        kuick().update(getDatabase(), this.object, this.group, null);
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

                    this.object.putFlag(this.device.id, TransferObject.Flag.REMOVED);
                    kuick().update(getDatabase(), this.object, this.group, null);
                } catch (FileNotFoundException | StreamCorruptedException e) {
                    Log.d(TAG, "handleTransferAsSender(): File is not accessible ? " + this.object.name);

                    this.activeConnection.reply(new JSONObject()
                            .put(Keyword.RESULT, false)
                            .put(Keyword.ERROR, Keyword.ERROR_NOT_ACCESSIBLE)
                            .put(Keyword.FLAG, Keyword.FLAG_GROUP_EXISTS)
                            .toString());

                    this.object.putFlag(this.device.id, TransferObject.Flag.INTERRUPTED);
                    kuick().update(getDatabase(), this.object, this.group, null);
                } catch (Exception e) {
                    e.printStackTrace();

                    this.activeConnection.reply(new JSONObject()
                            .put(Keyword.RESULT, false)
                            .put(Keyword.ERROR, Keyword.ERROR_UNKNOWN)
                            .put(Keyword.FLAG, Keyword.FLAG_GROUP_EXISTS)
                            .toString());

                    this.object.putFlag(this.device.id, TransferObject.Flag.INTERRUPTED);
                    kuick().update(getDatabase(), this.object, this.group, null);
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

    public static void startTransferAsClient(BackgroundService service, long groupId, String deviceId,
                                             TransferObject.Type type) throws TransferGroupNotFoundException,
            DeviceNotFoundException, ConnectionNotFoundException, AssigneeNotFoundException
    {
        Kuick kuick = AppUtils.getKuick(service);
        SQLiteDatabase db = kuick.getReadableDatabase();
        FileTransferTask task = new FileTransferTask();
        task.type = type;
        task.device = new NetworkDevice(deviceId);

        try {
            kuick.reconstruct(db, task.device);
        } catch (ReconstructionFailedException e) {
            throw new DeviceNotFoundException();
        }

        task.group = new PreloadedGroup(groupId);

        try {
            kuick.reconstruct(db, task.group);
        } catch (ReconstructionFailedException e) {
            throw new TransferGroupNotFoundException();
        }

        task.assignee = new TransferAssignee(task.group, task.device, task.type);

        try {
            kuick.reconstruct(db, task.assignee);
        } catch (ReconstructionFailedException e) {
            throw new AssigneeNotFoundException();
        }

        task.connection = new DeviceConnection(task.assignee);

        try {
            kuick.reconstruct(db, task.connection);
        } catch (ReconstructionFailedException e) {
            throw new ConnectionNotFoundException();
        }

        Log.d(TAG, "startTransferAsClient(): With deviceId=" + task.device.id + " groupId=" + task.group.id
                + " adapter=" + task.assignee.connectionAdapter);
        service.run(task);
    }

    public static Identity identityOf(FileTransferTask task)
    {
        return identityWith(task.group.id, task.device.id, task.type);
    }

    public static Identity identityWith(long groupId, String deviceId, TransferObject.Type type)
    {
        return Identity.withANDs(groupId, deviceId, type);
    }

    public void startTransferAsClient()
    {
        CommunicationBridge.connect(kuick(), true, this::startTransferAsClientInternal);
    }

    void startTransferAsClientInternal(CommunicationBridge.Client client)
    {
        try {
            this.activeConnection = client.communicate(this.device, this.connection);

            {
                JSONObject reply = new JSONObject()
                        .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER_JOB)
                        .put(Keyword.TRANSFER_GROUP_ID, this.group.id)
                        .put(Keyword.TRANSFER_TYPE, this.type.toString());

                this.activeConnection.reply(reply.toString());
                Log.d(TAG, "startTransferAsClient(): reply: " + reply.toString());
            }

            {
                CoolSocket.ActiveConnection.Response response = this.activeConnection.receive();
                JSONObject responseJSON = new JSONObject(response.response);

                Log.d(TAG, "startTransferAsClient(): " + this.type.toString() + "; About to start with "
                        + response.response);

                if (responseJSON.getBoolean(Keyword.RESULT)) {
                    this.attemptsLeft = 2;

                    if (TransferObject.Type.INCOMING.equals(this.type)) {
                        handleTransferAsReceiver();
                    } else if (TransferObject.Type.OUTGOING.equals(this.type)) {
                        this.activeConnection.reply(Keyword.STUB);
                        handleTransferAsSender();
                        this.activeConnection.reply(Keyword.STUB);
                    }

                    try {
                        CoolSocket.ActiveConnection.Response lastResponse = this.activeConnection.receive();

                        Log.d(TAG, "startTransferAsClient(): Final response before " + "exit: "
                                + lastResponse.response);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    getNotificationHelper().notifyConnectionError(this, responseJSON.has(Keyword.ERROR)
                            ? responseJSON.getString(Keyword.ERROR) : null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            getNotificationHelper().notifyConnectionError(this, null);
        }
    }
}
