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

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.service.backgroundservice.BackgroundTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.communicationbridge.CommunicationException;
import com.genonbeta.android.framework.io.DocumentFile;
import org.json.JSONException;
import org.json.JSONObject;
import org.monora.coolsocket.core.response.Response;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

public class ReceiveUpdateTask extends BackgroundTask
{
    private final Device mDevice;
    private final DeviceAddress mConnection;

    public ReceiveUpdateTask(Device device, DeviceAddress connection)
    {
        mDevice = device;
        mConnection = connection;
    }

    @Override
    public void onRun() throws InterruptedException
    {
        int versionCode;
        long updateSize;

        try (ActiveConnection activeConnection = CommunicationBridge.openConnection(mConnection.toInet4Address())) {
            setOngoingContent(getService().getString(R.string.mesg_waiting));

            activeConnection.reply(new JSONObject().put(Keyword.REQUEST, Keyword.REQUEST_UPDATE));

            {
                Response response = activeConnection.receive();
                JSONObject responseJSON = response.getAsJson();

                if (!responseJSON.getBoolean(Keyword.RESULT))
                    throw new CommunicationException("Update request was denied by the target");

                versionCode = responseJSON.getInt(Keyword.DEVICE_VERSION_CODE);
                updateSize = responseJSON.getLong(Keyword.INDEX_FILE_SIZE);

                if (updateSize < 1)
                    throw new IOException("The target did not report update size");
            }

            {
                activeConnection.reply(new JSONObject().put(Keyword.RESULT, true));
            }

            progress().addToTotal(100);
            setOngoingContent(getService().getString(R.string.text_receiving));
            publishStatus();

            DocumentFile dir = FileUtils.getApplicationDirectory(getService());
            String fileName = FileUtils.getUniqueFileName(dir, getService().getString(R.string.text_appName)
                    + "_v" + versionCode + ".apk", true);
            DocumentFile tmpFile = dir.createFile(null, fileName);

            InputStream inputStream = activeConnection.getSocket().getInputStream();
            OutputStream outputStream = getService().getContentResolver().openOutputStream(tmpFile.getUri());

            if (outputStream == null)
                throw new IOException("Could open a file to save the update.");

            long receivedBytes = 0;
            long lastRead = System.nanoTime();
            int len;
            byte[] buffer = new byte[AppConfig.BUFFER_LENGTH_DEFAULT];

            while (receivedBytes < updateSize) {
                throwIfInterrupted();

                if ((len = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                    outputStream.flush();

                    receivedBytes += len;
                    lastRead = System.nanoTime();

                    progress().setTotal((int) ((100 / updateSize) * receivedBytes));
                    publishStatus();
                }

                if (System.nanoTime() - lastRead > AppConfig.DEFAULT_SOCKET_TIMEOUT * 1e6)
                    throw new TimeoutException("Did not read for 5secs");
            }

            post(TaskMessage.newInstance()
                    .setTone(TaskMessage.Tone.Positive)
                    .setTitle(getService(), R.string.text_taskCompleted)
                    .setMessage(getService(), R.string.mesg_updateDownloadComplete)
                    .addAction(getService(), R.string.butn_open, TaskMessage.Tone.Positive,
                            (service, msg, action) -> FileUtils.openUriForeground(getService(), tmpFile)));
        } catch (IOException | CommunicationException | JSONException | TimeoutException e) {
            e.printStackTrace();

            post(TaskMessage.newInstance()
                    .setTone(TaskMessage.Tone.Negative)
                    .setTitle(getService(), R.string.mesg_fileReceiveError)
                    .setMessage(getService(), R.string.mesg_updateDownloadError)
                    .addAction(getService(), R.string.butn_retry, TaskMessage.Tone.Positive,
                            (service, msg, action) -> rerun(service)));
        }
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public String getTitle()
    {
        return getService().getString(R.string.mesg_ongoingUpdateDownload);
    }
}
