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

import android.app.Activity;
import androidx.annotation.Nullable;
import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.ProgressDialog;
import com.genonbeta.TrebleShot.callback.OnDeviceSelectedListener;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.util.Interrupter;
import com.genonbeta.android.framework.util.MathUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EstablishConnectionDialog extends ProgressDialog
{
    private WorkerService.RunningTask mTask;

    public EstablishConnectionDialog(final Activity activity, final NetworkDevice networkDevice,
                                     @Nullable final OnDeviceSelectedListener listener)
    {
        super(activity);

        final Interrupter interrupter = new Interrupter();

        setTitle(R.string.text_automaticNetworkConnectionOngoing);
        setCancelable(false);
        setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        setButton(ProgressDialog.BUTTON_NEGATIVE, getContext().getString(R.string.butn_cancel), (dialogInterface, i) -> interrupter.interrupt());

        mTask = new WorkerService.RunningTask()
        {
            @Override
            protected void onRun()
            {
                setInterrupter(interrupter);
                publishStatusText(getService().getString(R.string.mesg_communicating));

                final List<ConnectionResult> reachedConnections = new ArrayList<>();
                final List<ConnectionResult> calculatedConnections = new ArrayList<>();
                final List<DeviceConnection> connectionList = AppUtils.getDatabase(activity).castQuery(new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION)
                        .setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=?", networkDevice.id)
                        .setOrderBy(AccessDatabase.FIELD_DEVICECONNECTION_LASTCHECKEDDATE + " DESC"), DeviceConnection.class);

                setMax(connectionList.size());

                for (DeviceConnection connection : connectionList)
                    calculatedConnections.add(new ConnectionResult(connection));

                for (final ConnectionResult connectionResult : calculatedConnections) {
                    if (getInterrupter().interrupted())
                        break;

                    publishStatusText(connectionResult.connection.adapterName);
                    setProgress(getProgress() + 1);

                    final Integer calculatedTime = CommunicationBridge.connect(AppUtils.getDatabase(activity), Integer.class,
                            client -> {
                                connectionResult.pingTime = -1;

                                try {
                                    final long startTime = System.currentTimeMillis();
                                    final CoolSocket.ActiveConnection activeConnection = client.connect(
                                            connectionResult.connection);
                                    final Interrupter.Closer selfCloser = userAction -> {
                                        try {
                                            activeConnection.getSocket().close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    };

                                    getInterrupter().addCloser(selfCloser);

                                    client.handshake(activeConnection, true);
                                    client.updateDeviceIfOkay(activeConnection, networkDevice);

                                    getInterrupter().removeCloser(selfCloser);

                                    connectionResult.pingTime = (int) (System.currentTimeMillis() - startTime);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    client.setReturn(connectionResult.pingTime);
                                }
                            });

                    if (calculatedTime != null && calculatedTime > -1)
                        reachedConnections.add(connectionResult);
                }

                dismiss();

                Comparator<ConnectionResult> connectionComparator = (resultFirst, resultLast) -> {
                    if (resultFirst.pingTime < 0 || resultLast.pingTime < 0)
                        return MathUtils.compare(resultFirst.pingTime, resultLast.pingTime);

                    // reverse: the smaller is the fastest
                    return MathUtils.compare(resultLast.pingTime, resultFirst.pingTime);
                };

                Collections.sort(reachedConnections, connectionComparator);
                Collections.sort(calculatedConnections, connectionComparator);

                if (activity != null && !activity.isFinishing())
                    activity.runOnUiThread(() -> {
                        if (listener == null) {
                            new ConnectionTestDialog(activity, networkDevice, calculatedConnections).show();
                        } else {
                            if (!getInterrupter().interrupted())
                                if (reachedConnections.size() < 1) {
                                    new Builder(activity)
                                            .setTitle(R.string.text_error)
                                            .setMessage(R.string.text_automaticNetworkConnectionFailed)
                                            .setNeutralButton(R.string.butn_choose, (dialog, which) ->
                                                    new ConnectionChooserDialog(activity, networkDevice, listener)
                                                            .show())
                                            .setNegativeButton(R.string.butn_close, null)
                                            .setPositiveButton(R.string.butn_retry, (dialog, which) -> show())
                                            .show();
                                } else {
                                    listener.onDeviceSelected(reachedConnections.get(0).connection, connectionList);
                                    dismiss();
                                }
                        }
                    });
            }
        };
    }

    @Override
    public void show()
    {
        super.show();
        mTask.setTitle(getContext().getString(R.string.text_connectDevices))
                .run(getContext());
    }

    public static class ConnectionResult
    {
        public DeviceConnection connection;
        public int pingTime = -1;

        public ConnectionResult(DeviceConnection connection)
        {
            this.connection = connection;
        }
    }
}