/*
 * Copyright (C) 2019 Veli Tasalı
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

package com.genonbeta.TrebleShot.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.BuildConfig;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.genonbeta.android.framework.io.DocumentFile;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;

/**
 * Created by: veli
 * Date: 5/18/17 5:16 PM
 */

public class DeviceInfoDialog extends AlertDialog.Builder
{
    public static final String TAG = DeviceInfoDialog.class.getSimpleName();

    public DeviceInfoDialog(@NonNull final Activity activity, final AccessDatabase database,
                            final NetworkDevice device)
    {
        super(activity);

        try {
            database.reconstruct(device);

            @SuppressLint("InflateParams")
            View rootView = LayoutInflater.from(activity).inflate(R.layout.layout_device_info, null);

            NetworkDevice localDevice = AppUtils.getLocalDevice(activity);
            ImageView image = rootView.findViewById(R.id.image);
            TextView text1 = rootView.findViewById(R.id.text1);
            TextView notSupportedText = rootView.findViewById(R.id.notSupportedText);
            TextView modelText = rootView.findViewById(R.id.modelText);
            TextView versionText = rootView.findViewById(R.id.versionText);
            final SwitchCompat accessSwitch = rootView.findViewById(R.id.accessSwitch);
            final SwitchCompat trustSwitch = rootView.findViewById(R.id.trustSwitch);
            final boolean isDeviceNormal = NetworkDevice.Type.NORMAL.equals(device.type);

            if (device.versionCode < AppConfig.SUPPORTED_MIN_VERSION)
                notSupportedText.setVisibility(View.VISIBLE);

            if (isDeviceNormal && (localDevice.versionCode < device.versionCode || BuildConfig.DEBUG))
                setNeutralButton(R.string.butn_update, (dialog, which) -> new EstablishConnectionDialog(activity, device,
                        (connection, availableInterfaces) -> runReceiveTask(activity, device, connection)).show());

            NetworkDeviceLoader.showPictureIntoView(device, image, AppUtils.getDefaultIconBuilder(activity));
            text1.setText(device.nickname);
            modelText.setText(String.format("%s %s", device.brand.toUpperCase(), device.model.toUpperCase()));
            versionText.setText(device.versionName);
            accessSwitch.setChecked(!device.isRestricted);
            trustSwitch.setEnabled(!device.isRestricted);
            trustSwitch.setChecked(device.isTrusted);

            accessSwitch.setOnCheckedChangeListener(
                    (button, isChecked) -> {
                        device.isRestricted = !isChecked;
                        database.publish(device);
                        database.broadcast();
                        trustSwitch.setEnabled(isChecked);
                    }
            );

            if (isDeviceNormal)
                trustSwitch.setOnCheckedChangeListener(
                        (button, isChecked) -> {
                            device.isTrusted = isChecked;
                            database.publish(device);
                            database.broadcast();
                        }
                );
            else
                trustSwitch.setVisibility(View.GONE);

            setView(rootView);
            setPositiveButton(R.string.butn_close, null);

            setNegativeButton(R.string.butn_remove, (dialog, which) -> new RemoveDeviceDialog(activity, device)
                    .show());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void runReceiveTask(final Activity activity, final NetworkDevice device,
                                  final DeviceConnection connection)
    {
        new WorkerService.RunningTask()
        {
            @Override
            public void onRun()
            {
                try {
                    publishStatusText(getService().getString(R.string.mesg_waiting));

                    int versionCode = -1;
                    long updateSize = -1;
                    CoolSocket.Client client = new CoolSocket.Client();
                    CoolSocket.ActiveConnection activeConnection = client.connect(
                            new InetSocketAddress(connection.ipAddress, AppConfig.SERVER_PORT_COMMUNICATION),
                            AppConfig.DEFAULT_SOCKET_TIMEOUT);

                    activeConnection.reply(new JSONObject()
                            .put(Keyword.REQUEST, Keyword.REQUEST_UPDATE_V2)
                            .toString());

                    {
                        CoolSocket.ActiveConnection.Response response = activeConnection.receive();
                        JSONObject responseJSON = new JSONObject(response.response);

                        if (!responseJSON.getBoolean(Keyword.RESULT))
                            throw new Exception("Update request was denied by the target");

                        versionCode = responseJSON.getInt(Keyword.APP_INFO_VERSION_CODE);
                        updateSize = responseJSON.getLong(Keyword.INDEX_FILE_SIZE);

                        if (updateSize < 1)
                            throw new Exception("The target did not report update size");
                    }

                    {
                        activeConnection.reply(new JSONObject()
                                .put(Keyword.RESULT, true)
                                .toString());
                    }

                    publishStatusText(getService().getString(R.string.text_receiving));

                    DocumentFile tmpFile;

                    {
                        tmpFile = FileUtils.getApplicationDirectory(getContext()).createFile(
                                null, device.versionName + "_" + System.currentTimeMillis() + ".apk");

                        InputStream inputStream = activeConnection.getSocket().getInputStream();
                        OutputStream outputStream = getContext().getContentResolver().openOutputStream(
                                tmpFile.getUri());

                        if (outputStream == null)
                            throw new Exception("Could open a file to save the update.");

                        long receivedBytes = 0;
                        long lastRead = 0;
                        int len;
                        byte[] buffer = new byte[AppConfig.BUFFER_LENGTH_DEFAULT];

                        while (receivedBytes < updateSize) {
                            long currentTime = System.currentTimeMillis();

                            if ((len = inputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, len);
                                outputStream.flush();

                                receivedBytes += len;
                                lastRead = currentTime;
                            }

                            if (System.currentTimeMillis() - lastRead > AppConfig.DEFAULT_SOCKET_TIMEOUT)
                                throw new TimeoutException("Did not read for 5secs");
                        }

                        outputStream.close();
                    }

                    final DocumentFile updateFile = tmpFile;

                    showDialog(activity, new AlertDialog.Builder(activity)
                            .setTitle(R.string.text_taskCompleted)
                            .setMessage(R.string.mesg_updateDownloadComplete)
                            .setNegativeButton(R.string.butn_close, null)
                            .setPositiveButton(R.string.butn_open, (dialog, which) -> FileUtils.openUriForeground(activity, updateFile)));
                } catch (Exception e) {
                    e.printStackTrace();
                    getInterrupter().interrupt(false);

                    showDialog(activity, new AlertDialog.Builder(activity)
                            .setTitle(R.string.text_error)
                            .setMessage(R.string.mesg_somethingWentWrong)
                            .setNegativeButton(R.string.butn_close, null)
                            .setPositiveButton(R.string.butn_retry, (dialog, which) -> runReceiveTask(activity, device, connection)));
                }
            }
        }.setTitle(getContext().getString(R.string.mesg_ongoingUpdateDownload))
                .run(activity);
    }

    public void showDialog(Activity activity, AlertDialog.Builder builder)
    {
        if (activity == null || activity.isFinishing())
            return;

        activity.runOnUiThread(builder::show);
    }
}
