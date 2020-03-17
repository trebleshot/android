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

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.callback.OnDeviceSelectedListener;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.ConnectionChooserDialog;
import com.genonbeta.TrebleShot.dialog.ConnectionTestDialog;
import com.genonbeta.TrebleShot.dialog.EstablishConnectionDialog;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.backgroundservice.BackgroundTask;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.android.database.Progress;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.util.MathUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AssessNetworkTask extends BackgroundTask
{
    private EstablishConnectionDialog mDialog;
    private NetworkDevice mDevice;
    private OnDeviceSelectedListener mListener;

    public AssessNetworkTask(EstablishConnectionDialog dialog, NetworkDevice device,
                             OnDeviceSelectedListener listener)
    {
        mDialog = dialog;
        mDevice = device;
        mListener = listener;
    }

    @Override
    protected void onRun()
    {
        final List<EstablishConnectionDialog.ConnectionResult> reachedConnections = new ArrayList<>();
        final List<EstablishConnectionDialog.ConnectionResult> calculatedConnections = new ArrayList<>();
        final List<DeviceConnection> connectionList = AppUtils.getKuick(getService()).castQuery(
                new SQLQuery.Select(Kuick.TABLE_DEVICECONNECTION)
                        .setWhere(Kuick.FIELD_DEVICECONNECTION_DEVICEID + "=?", mDevice.id)
                        .setOrderBy(Kuick.FIELD_DEVICECONNECTION_LASTCHECKEDDATE + " DESC"),
                DeviceConnection.class);

        Progress.addToTotal(progressListener(), connectionList.size());

        for (DeviceConnection connection : connectionList)
            calculatedConnections.add(new EstablishConnectionDialog.ConnectionResult(connection));

        for (final EstablishConnectionDialog.ConnectionResult connectionResult : calculatedConnections) {
            if (isInterrupted())
                break;

            setCurrentContent(connectionResult.connection.adapterName);
            Progress.addToCurrent(progressListener(), 1);

            final Long calculatedTime = CommunicationBridge.connect(AppUtils.getKuick(getService()), Long.class,
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

        mDialog.dismiss();

        Comparator<EstablishConnectionDialog.ConnectionResult> connectionComparator = (resultFirst, resultLast) -> {
            // make sure we are not comparing unsuccessful attempts with their pingTime values.
            if (resultFirst.successful != resultLast.successful)
                return resultFirst.successful ? 1 : -1;

            return MathUtils.compare(resultLast.pingTime, resultFirst.pingTime);
        };

        Collections.sort(reachedConnections, connectionComparator);
        Collections.sort(calculatedConnections, connectionComparator);

        Activity activity = mDialog.getOwnerActivity();

        if (activity != null && !activity.isFinishing())
            activity.runOnUiThread(() -> {
                if (mListener == null) {
                    new ConnectionTestDialog(activity, mDevice, calculatedConnections).show();
                } else {
                    if (!isInterrupted())
                        if (reachedConnections.size() < 1) {
                            new AlertDialog.Builder(activity)
                                    .setTitle(R.string.text_error)
                                    .setMessage(R.string.text_automaticNetworkConnectionFailed)
                                    .setNeutralButton(R.string.butn_choose, (dialog, which) ->
                                            new ConnectionChooserDialog(activity, mDevice, mListener)
                                                    .show())
                                    .setNegativeButton(R.string.butn_close, null)
                                    .setPositiveButton(R.string.butn_retry, (dialog, which) -> mDialog.show())
                                    .show();
                        } else {
                            mListener.onDeviceSelected(reachedConnections.get(0).connection, connectionList);
                            mDialog.dismiss();
                        }
                }
            });
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public String getTitle()
    {
        return getService().getString(R.string.text_connectionTest);
    }

    @Override
    public boolean isInterrupted()
    {
        return super.isInterrupted() || !mDialog.isShowing();
    }
}
