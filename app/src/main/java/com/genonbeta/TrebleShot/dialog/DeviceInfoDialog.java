package com.genonbeta.TrebleShot.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.BuildConfig;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.callback.OnDeviceSelectedListener;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.genonbeta.TrebleShot.util.UpdateUtils;
import com.genonbeta.android.framework.io.DocumentFile;

import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.List;

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

            if (device.versionNumber < AppConfig.SUPPORTED_MIN_VERSION)
                notSupportedText.setVisibility(View.VISIBLE);

            if (localDevice.versionNumber < device.versionNumber || BuildConfig.DEBUG)
                setNeutralButton(R.string.butn_update, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        new EstablishConnectionDialog(activity, device, new OnDeviceSelectedListener()
                        {
                            @Override
                            public void onDeviceSelected(final NetworkDevice.Connection connection, List<NetworkDevice.Connection> availableInterfaces)
                            {
                                runReceiveTask(activity, device, connection);
                            }
                        }).show();
                    }
                });

            NetworkDeviceLoader.showPictureIntoView(device, image, AppUtils.getDefaultIconBuilder(activity));
            text1.setText(device.nickname);
            modelText.setText(String.format("%s %s", device.brand.toUpperCase(), device.model.toUpperCase()));
            versionText.setText(device.versionName);
            accessSwitch.setChecked(!device.isRestricted);
            trustSwitch.setEnabled(!device.isRestricted);
            trustSwitch.setChecked(device.isTrusted);

            accessSwitch.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener()
                    {
                        @Override
                        public void onCheckedChanged(CompoundButton button, boolean isChecked)
                        {
                            device.isRestricted = !isChecked;
                            database.publish(device);
                            trustSwitch.setEnabled(isChecked);
                        }
                    }
            );

            trustSwitch.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener()
                    {
                        @Override
                        public void onCheckedChanged(CompoundButton button, boolean isChecked)
                        {
                            device.isTrusted = isChecked;
                            database.publish(device);
                        }
                    }
            );

            setView(rootView);
            setPositiveButton(R.string.butn_close, null);

            setNegativeButton(R.string.butn_remove, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    new RemoveDeviceDialog(activity, device)
                            .show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void runReceiveTask(final Activity activity, final NetworkDevice device,
                                  final NetworkDevice.Connection connection)
    {
        new WorkerService.RunningTask()
        {
            @Override
            public void onRun()
            {
                try {
                    publishStatusText(getService().getString(R.string.mesg_waiting));

                    final Context context = getContext();
                    final DocumentFile receivedFile = UpdateUtils.receiveUpdate(activity, device, getInterrupter(), new UpdateUtils.OnConnectionReadyListener()
                    {
                        @Override
                        public void onConnectionReady(ServerSocket socket)
                        {
                            CoolSocket.connect(new CoolSocket.Client.ConnectionHandler()
                            {
                                @Override
                                public void onConnect(CoolSocket.Client client)
                                {
                                    try {
                                        CoolSocket.ActiveConnection activeConnection = client.connect(new InetSocketAddress(connection.ipAddress, AppConfig.SERVER_PORT_COMMUNICATION), AppConfig.DEFAULT_SOCKET_TIMEOUT);
                                        activeConnection.reply(new JSONObject().put(Keyword.REQUEST, Keyword.BACK_COMP_REQUEST_SEND_UPDATE).toString());

                                        CoolSocket.ActiveConnection.Response response = activeConnection.receive();
                                        JSONObject responseJSON = new JSONObject(response.response);

                                        if (!responseJSON.has(Keyword.RESULT) || !responseJSON.getBoolean(Keyword.RESULT))
                                            throw new Exception("Not the answer we were looking for.");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        getInterrupter().interrupt(false);
                                    }
                                }
                            });
                        }
                    });

                    new Handler(Looper.getMainLooper()).post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (!activity.isFinishing()) {
                                if (receivedFile == null)
                                    new AlertDialog.Builder(activity)
                                            .setTitle(R.string.text_error)
                                            .setMessage(R.string.mesg_somethingWentWrong)
                                            .setNegativeButton(R.string.butn_close, null)
                                            .setPositiveButton(R.string.butn_retry, new DialogInterface.OnClickListener()
                                            {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which)
                                                {
                                                    runReceiveTask(activity, device, connection);
                                                }
                                            })
                                            .show();
                                else
                                    new AlertDialog.Builder(activity)
                                            .setTitle(R.string.text_taskCompleted)
                                            .setMessage(R.string.mesg_updateDownloadComplete)
                                            .setNegativeButton(R.string.butn_close, null)
                                            .setPositiveButton(R.string.butn_open, new DialogInterface.OnClickListener()
                                            {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which)
                                                {
                                                    FileUtils.openUriForeground(activity, receivedFile);
                                                }
                                            })
                                            .show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.setTitle(getContext().getString(R.string.mesg_ongoingUpdateDownload))
                .run(activity);
    }
}
