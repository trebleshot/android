package com.genonbeta.TrebleShot.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.ProgressDialog;
import com.genonbeta.TrebleShot.callback.OnDeviceSelectedListener;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.ConnectionChooserDialog;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.TrebleShot.util.NetworkUtils;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.util.Interrupter;
import com.genonbeta.android.framework.util.MathUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import androidx.appcompat.app.AlertDialog;

public class EstablishConnectionDialog extends ProgressDialog
{
    public static final String TAG = EstablishConnectionDialog.class.getSimpleName();

    public static final int TASK_CONNECT = 1;

    public EstablishConnectionDialog(final Activity activity, final NetworkDevice networkDevice, final OnDeviceSelectedListener listener)
    {
        super(activity);

        final Interrupter interrupter = new Interrupter();

        setTitle(R.string.text_feelLuckyOngoing);
        setCancelable(false);
        setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        setButton(android.app.ProgressDialog.BUTTON_NEGATIVE, getContext().getString(R.string.butn_cancel), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                interrupter.interrupt();
            }
        });

        show();

        runThis(interrupter, activity, networkDevice, listener);
    }

    private void runThis(final Interrupter interrupter, final Activity activity, final NetworkDevice networkDevice, final OnDeviceSelectedListener listener)
    {
        WorkerService.run(activity, new WorkerService.RunningTask(TAG, TASK_CONNECT)
        {
            @Override
            protected void onRun()
            {
                setInterrupter(interrupter);

                @SuppressLint("UseSparseArrays") final HashMap<Integer, NetworkDevice.Connection> calculatedConnections = new HashMap<>();
                final ArrayList<NetworkDevice.Connection> connectionList = AppUtils.getDatabase(activity).castQuery(new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION)
                        .setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=?", networkDevice.deviceId)
                        .setOrderBy(AccessDatabase.FIELD_DEVICECONNECTION_LASTCHECKEDDATE + " DESC"), NetworkDevice.Connection.class);

                for (final NetworkDevice.Connection connection : connectionList) {
                    if (getInterrupter().interrupted())
                        break;

                    setProgress(getProgress() + 1);

                    if (!NetworkUtils.ping(connection.ipAddress, 500))
                        continue;

                    Integer calculatedTime = CommunicationBridge.connect(AppUtils.getDatabase(activity), Integer.class, new CommunicationBridge.Client.ConnectionHandler()
                    {
                        @Override
                        public void onConnect(CommunicationBridge.Client client)
                        {
                            int outTime = -1;

                            try {
                                final long startTime = System.currentTimeMillis();
                                final CoolSocket.ActiveConnection activeConnection = client.connect(connection);
                                final Interrupter.Closer selfCloser = new Interrupter.Closer()
                                {
                                    @Override
                                    public void onClose(boolean userAction)
                                    {
                                        try {
                                            activeConnection.getSocket().close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                };

                                getInterrupter().addCloser(selfCloser);

                                client.handshake(activeConnection, true);
                                client.updateDeviceIfOkay(activeConnection, networkDevice);

                                getInterrupter().removeCloser(selfCloser);

                                outTime = (int) (System.currentTimeMillis() - startTime);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                client.setReturn(outTime);
                            }
                        }
                    });

                    if (calculatedTime != null && calculatedTime > -1)
                        calculatedConnections.put(calculatedTime, connection);
                }

                dismiss();

                if (!getInterrupter().interrupted())
                    if (calculatedConnections.size() < 1) {
                        if (activity != null)
                            activity.runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    new AlertDialog.Builder(activity)
                                            .setTitle(R.string.text_error)
                                            .setMessage(R.string.text_feelLuckyFailed)
                                            .setNeutralButton(R.string.butn_choose, new OnClickListener()
                                            {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which)
                                                {
                                                    new ConnectionChooserDialog(activity, networkDevice, listener)
                                                            .show();
                                                }
                                            })
                                            .setNegativeButton(R.string.butn_close, null)
                                            .setPositiveButton(R.string.butn_retry, new OnClickListener()
                                            {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which)
                                                {
                                                    runThis(interrupter, activity, networkDevice, listener);
                                                }
                                            })
                                            .show();
                                }
                            });
                    } else {
                        final ArrayList<Integer> comparedList = new ArrayList<>(calculatedConnections.keySet());

                        Collections.sort(comparedList, new Comparator<Integer>()
                        {
                            @Override
                            public int compare(Integer integer1, Integer integer2)
                            {
                                return MathUtils.compare(integer1, integer2);
                            }
                        });

                        if (activity != null)
                            activity.runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    listener.onDeviceSelected(calculatedConnections.get(comparedList.get(0)), connectionList);
                                }
                            });

                        dismiss();
                    }
            }
        });
    }
}