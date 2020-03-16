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
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.util.MathUtils;
import com.genonbeta.android.framework.util.Stoppable;
import com.genonbeta.android.framework.util.StoppableImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EstablishConnectionDialog extends ProgressDialog
{
    private BackgroundService.RunningTask mTask;

    public EstablishConnectionDialog(final Activity activity, final NetworkDevice networkDevice,
                                     @Nullable final OnDeviceSelectedListener listener)
    {
        super(activity);

        final Stoppable stoppable = new StoppableImpl();

        setTitle(R.string.text_automaticNetworkConnectionOngoing);
        setCancelable(false);
        setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        setButton(ProgressDialog.BUTTON_NEGATIVE, getContext().getString(R.string.butn_cancel), (dialogInterface, i) -> stoppable.interrupt());

        mTask = new BackgroundService.RunningTask()
        {
            @Override
            protected void onRun()
            {
                setStoppable(stoppable);
                publishStatusText(getService().getString(R.string.mesg_communicating));

                final List<ConnectionResult> reachedConnections = new ArrayList<>();
                final List<ConnectionResult> calculatedConnections = new ArrayList<>();
                final List<DeviceConnection> connectionList = AppUtils.getKuick(activity).castQuery(
                        new SQLQuery.Select(Kuick.TABLE_DEVICECONNECTION)
                                .setWhere(Kuick.FIELD_DEVICECONNECTION_DEVICEID + "=?",
                                        networkDevice.id)
                                .setOrderBy(Kuick.FIELD_DEVICECONNECTION_LASTCHECKEDDATE + " DESC"),
                        DeviceConnection.class);

                setMax(connectionList.size());

                for (DeviceConnection connection : connectionList)
                    calculatedConnections.add(new ConnectionResult(connection));

                for (final ConnectionResult connectionResult : calculatedConnections) {
                    if (isInterrupted())
                        break;

                    publishStatusText(connectionResult.connection.adapterName);
                    setProgress(getProgress() + 1);

                    final Long calculatedTime = CommunicationBridge.connect(AppUtils.getKuick(activity), Long.class,
                            client -> {
                                try {
                                    long startTime = System.nanoTime();
                                    CoolSocket.ActiveConnection connection = client.connectWithHandshake(
                                            connectionResult.connection, true);
                                    connectionResult.pingTime = System.nanoTime() - startTime;
                                    connectionResult.successful = true;

                                    connection.getSocket().close();
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
                    // make sure we are not comparing unsuccessful attempts with their pingTime values.
                    if (resultFirst.successful != resultLast.successful)
                        return resultFirst.successful ? 1 : -1;

                    return MathUtils.compare(resultLast.pingTime, resultFirst.pingTime);
                };

                Collections.sort(reachedConnections, connectionComparator);
                Collections.sort(calculatedConnections, connectionComparator);

                if (activity != null && !activity.isFinishing())
                    activity.runOnUiThread(() -> {
                        if (listener == null) {
                            new ConnectionTestDialog(activity, networkDevice, calculatedConnections).show();
                        } else {
                            if (!isInterrupted())
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
        public long pingTime = 0; // nano
        boolean successful = false;

        public ConnectionResult(DeviceConnection connection)
        {
            this.connection = connection;
        }
    }
}