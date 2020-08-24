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
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.exception.ConnectionNotFoundException;
import com.genonbeta.TrebleShot.exception.DeviceNotFoundException;
import com.genonbeta.TrebleShot.exception.MemberNotFoundException;
import com.genonbeta.TrebleShot.exception.TransferNotFoundException;
import com.genonbeta.TrebleShot.fragment.FileListFragment;
import com.genonbeta.TrebleShot.object.*;
import com.genonbeta.TrebleShot.protocol.communication.ContentException;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException;
import com.genonbeta.TrebleShot.util.*;
import com.genonbeta.android.database.exception.ReconstructionFailedException;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.io.LocalDocumentFile;
import com.genonbeta.android.framework.io.StreamInfo;
import org.json.JSONObject;
import org.monora.coolsocket.core.response.SizeLimitExceededException;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.session.CancelledException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.List;

import static com.genonbeta.TrebleShot.object.Identifier.from;

public class FileTransferTask extends AttachableAsyncTask<AttachedTaskListener>
{
    public static final String TAG = FileTransferTask.class.getSimpleName();

    // Static objects
    public ActiveConnection activeConnection;
    public Device device;
    public TransferIndex index;
    public Transfer transfer;
    public TransferMember member;
    public List<DeviceAddress> addressList;
    public TransferItem.Type type;

    // Changing objects
    public TransferItem item;
    public TransferItem lastItem;
    public DocumentFile file;
    public long lastMovedBytes;
    public long currentBytes; // moving
    public long completedBytes;
    public int completedCount;

    private long mTimeTransactionSaved;
    private SQLiteDatabase mDatabase;

    @Override
    protected void onRun() throws TaskStoppedException
    {
        if (activeConnection == null)
            startTransferAsClient();
        else if (TransferItem.Type.OUTGOING.equals(type))
            handleTransferAsSender();
        else if (TransferItem.Type.INCOMING.equals(type))
            handleTransferAsReceiver();
    }

    @Override
    public void onPublishStatus()
    {
        super.onPublishStatus();

        if (isInterrupted() || isFinished()) {
            if (isInterrupted())
                setOngoingContent(getContext().getString(R.string.text_cancellingTransfer));
            kuick().broadcast();
            return;
        }

        long bytesTransferred = completedBytes + currentBytes;
        StringBuilder text = new StringBuilder();

        progress().setTotal(100);

        if (bytesTransferred > 0 && index.bytesPending() > 0)
            progress().setCurrent((int) (100 * ((double) bytesTransferred / index.bytesPending())));

        if (lastMovedBytes > 0 && bytesTransferred > 0) {
            long change = bytesTransferred - lastMovedBytes;

            text.append(FileUtils.sizeExpression(change, false));

            if (index.bytesPending() > 0 && change > 0) {
                long timeNeeded = (index.bytesPending() - bytesTransferred) / change;

                text.append(" (");
                text.append(getContext().getString(R.string.text_remainingTime,
                        TimeUtils.getDuration(timeNeeded, false)));
                text.append(")");
            }
        }

        lastMovedBytes = bytesTransferred;

        if (item != null) {
            if (text.length() > 0)
                text.append(" ").append(getContext().getString(R.string.mode_middleDot)).append(" ");
            text.append(item.name);

            try {
                TransferItem.Flag flag = TransferItem.Flag.IN_PROGRESS;
                flag.setBytesValue(currentBytes);

                if (TransferItem.Type.INCOMING.equals(type))
                    item.setFlag(flag);
                else if (TransferItem.Type.OUTGOING.equals(this.type))
                    item.putFlag(this.device.uid, flag);


                kuick().update(getDatabase(), item, transfer, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        setOngoingContent(text.toString());
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

        Log.d(TAG, "createFrom: deviceId=" + device.uid + " transferId=" + transfer.id);

        FileTransferTask task = new FileTransferTask();
        task.type = type;
        task.device = device;
        task.transfer = transfer;
        task.member = member;
        task.addressList = addressList;
        task.index = new TransferIndex(transfer);

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
        try {
            Transfers.loadTransferInfo(getContext(), index, member);

            while (activeConnection.getSocket().isConnected()) {
                publishStatus();

                currentBytes = 0;
                item = Transfers.fetchFirstValidIncomingTransferItem(getContext(), transfer.id);

                if (item == null)
                    throw new ContentException(ContentException.Error.AlreadyExists);
                else
                    lastItem = item;

                // We don't handle IO errors on the receiver side.
                // An IO error for this side means there is a permission/storage issue.
                file = FileUtils.getIncomingFile(getContext(), item, transfer);
                currentBytes = file.length();
                StreamInfo streamInfo = StreamInfo.getStreamInfo(getContext(), file.getUri());

                try (OutputStream outputStream = streamInfo.openOutputStream()) {
                    activeConnection.reply(new JSONObject()
                            .put(Keyword.TRANSFER_REQUEST_ID, item.id)
                            .put(Keyword.SKIPPED_BYTES, currentBytes));

                    if (CommunicationBridge.receiveResult(activeConnection, device)) {
                        int len;
                        ActiveConnection.Description description = activeConnection.readBegin();
                        WritableByteChannel writableByteChannel = Channels.newChannel(outputStream);

                        while (description.hasAvailable() && (len = activeConnection.read(description)) != -1) {
                            publishStatus();

                            currentBytes += len;
                            writableByteChannel.write(description.byteBuffer);
                        }

                        outputStream.flush();
                        item.setFlag(TransferItem.Flag.DONE);
                        completedBytes += currentBytes;
                        completedCount++;

                        if (file.getParentFile() != null) {
                            file = FileUtils.saveReceivedFile(file.getParentFile(), file, item);

                            Log.d(TAG, "handleTransferAsReceiver(): File is " + this.file.getUri().toString()
                                    + " and name is " + this.item.file);

                            getContext().sendBroadcast(new Intent(FileListFragment.ACTION_FILE_LIST_CHANGED)
                                    .putExtra(FileListFragment.EXTRA_FILE_PARENT, file.getParentFile().getUri())
                                    .putExtra(FileListFragment.EXTRA_FILE_NAME, file.getName()));
                        }

                        if (this.file instanceof LocalDocumentFile && getMediaScanner().isConnected())
                            getMediaScanner().scanFile(((LocalDocumentFile) file).getFile().getAbsolutePath(),
                                    item.mimeType);

                        Log.d(TAG, "handleTransferAsSender(): File received " + item.name);
                    }
                } catch (CancelledException e) {
                    item.setFlag(TransferItem.Flag.PENDING);
                    throw e;
                } catch (FileNotFoundException e) {
                    throw e;
                } catch (ContentException e) {
                    switch (e.error) {
                        case NotFound:
                            item.setFlag(TransferItem.Flag.REMOVED);
                            break;
                        case AlreadyExists:
                        case NotAccessible:
                        default:
                            item.setFlag(TransferItem.Flag.INTERRUPTED);
                    }
                } catch (Exception e) {
                    item.setFlag(TransferItem.Flag.INTERRUPTED);
                    throw e;
                } finally {
                    kuick().update(getDatabase(), item, transfer, null);
                    item = null;
                }
            }
        } catch (TaskStoppedException | CancelledException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
            try {
                CommunicationBridge.sendError(activeConnection, e);
            } catch (Exception e1) {
                try {
                    post(TaskMessage.newInstance()
                            .setTone(TaskMessage.Tone.Negative)
                            .setTitle(getContext(), R.string.text_communicationError)
                            .setMessage(getContext().getString(R.string.mesg_errorDuringTransfer, device.username)));
                } catch (TaskStoppedException ignored) {
                }
            }
        }

        if (completedCount > 0) {
            getNotificationHelper().notifyFileReceived(this, FileUtils.getSavePath(getContext(), transfer));
            Log.d(TAG, "handleTransferAsReceiver(): Notify user");
        }
    }

    private void handleTransferAsSender()
    {
        try {
            Transfers.loadTransferInfo(getContext(), index, member);

            while (activeConnection.getSocket().isConnected()) {
                publishStatus();
                JSONObject request = CommunicationBridge.receiveSecure(activeConnection, device);

                try {
                    item = new TransferItem(transfer.id, request.getInt(Keyword.TRANSFER_REQUEST_ID), type);
                    kuick().reconstruct(getDatabase(), item);

                    try {
                        file = FileUtils.fromUri(getContext(), Uri.parse(item.file));
                        if (item.size != file.length())
                            throw new FileNotFoundException("File size has changed. Probably it is a different file.");

                        currentBytes = request.getLong(Keyword.SKIPPED_BYTES);
                        long length = item.size - currentBytes;

                        try (InputStream inputStream = getContext().getContentResolver().openInputStream(
                                file.getUri())) {
                            if (inputStream == null)
                                throw new FileNotFoundException("The input stream for the file has failed to open.");

                            if (currentBytes > 0 && inputStream.skip(currentBytes) != currentBytes)
                                throw new IOException("Failed to skip " + currentBytes + "bytes");

                            CommunicationBridge.sendResult(activeConnection, true);

                            item.putFlag(device.uid, TransferItem.Flag.IN_PROGRESS);
                            kuick().update(getDatabase(), item, transfer, null);

                            ActiveConnection.Description description = activeConnection.writeBegin(0, length);
                            byte[] bytes = new byte[8196];
                            int readLength;

                            try {
                                while ((readLength = inputStream.read(bytes)) != -1) {
                                    publishStatus();

                                    if (readLength > 0) {
                                        currentBytes += readLength;
                                        activeConnection.write(description, bytes, 0, readLength);
                                    }
                                }

                                activeConnection.writeEnd(description);
                            } catch (SizeLimitExceededException ignored) {
                            }

                            completedBytes += currentBytes;
                            completedCount++;
                            item.putFlag(device.uid, TransferItem.Flag.DONE);

                            Log.d(TAG, "handleTransferAsSender(): File sent " + this.item.name);
                        }
                    } catch (CancelledException e) {
                        item.putFlag(device.uid, TransferItem.Flag.PENDING);
                        throw e;
                    } catch (FileNotFoundException e) {
                        item.putFlag(device.uid, TransferItem.Flag.REMOVED);
                        throw e;
                    } catch (Exception e) {
                        item.putFlag(device.uid, TransferItem.Flag.INTERRUPTED);
                        throw e;
                    } finally {
                        kuick().update(getDatabase(), item, transfer, null);
                        item = null;
                    }
                } catch (CancelledException e) {
                    throw e;
                } catch (FileNotFoundException | ReconstructionFailedException e) {
                    CommunicationBridge.sendError(activeConnection, Keyword.ERROR_NOT_FOUND);
                } catch (IOException e) {
                    CommunicationBridge.sendError(activeConnection, Keyword.ERROR_NOT_ACCESSIBLE);
                } catch (Exception e) {
                    CommunicationBridge.sendError(activeConnection, Keyword.ERROR_UNKNOWN);
                }
            }
        } catch (CancelledException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (!(e instanceof ContentException)
                        || !ContentException.Error.AlreadyExists.equals(((ContentException) e).error))
                    post(TaskMessage.newInstance()
                            .setTone(TaskMessage.Tone.Negative)
                            .setTitle(getContext(), R.string.text_communicationError)
                            .setMessage(getContext().getString(R.string.mesg_errorDuringTransfer, device.username)));
            } catch (TaskStoppedException ignored) {
            }
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

    @Override
    public boolean interrupt(boolean userAction)
    {
        if (activeConnection != null) {
            try {
                activeConnection.closeSafely();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return super.interrupt(userAction);
    }

    public void startTransferAsClient() throws TaskStoppedException
    {
        try (CommunicationBridge bridge = CommunicationBridge.connect(kuick(), addressList, device, 0)) {
            bridge.requestFileTransferStart(transfer.id, type);

            if (bridge.receiveResult()) {
                activeConnection = bridge.getActiveConnection();

                if (TransferItem.Type.INCOMING.equals(type)) {
                    handleTransferAsReceiver();
                } else if (TransferItem.Type.OUTGOING.equals(type)) {
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
