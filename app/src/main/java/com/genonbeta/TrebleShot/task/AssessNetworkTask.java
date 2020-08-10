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
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableBgTask;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.util.MathUtils;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AssessNetworkTask extends AttachableBgTask<AssessNetworkTask.CalculationResultListener>
{
    private final Device mDevice;

    public AssessNetworkTask(Device device)
    {
        mDevice = device;
    }

    @Override
    protected void onRun() throws InterruptedException
    {
        List<DeviceAddress> knownConnectionList = AppUtils.getKuick(getService()).castQuery(
                new SQLQuery.Select(Kuick.TABLE_DEVICECONNECTION)
                        .setWhere(Kuick.FIELD_DEVICECONNECTION_DEVICEID + "=?", mDevice.uid)
                        .setOrderBy(Kuick.FIELD_DEVICECONNECTION_LASTCHECKEDDATE + " DESC"), DeviceAddress.class);
        ConnectionResult[] results = new ConnectionResult[knownConnectionList.size()];

        progress().addToTotal(knownConnectionList.size());
        publishStatus();

        if (results.length > 0) {
            for (int i = 0; i < results.length; i++) {
                throwIfInterrupted();

                ConnectionResult connectionResult = results[i] = new ConnectionResult(knownConnectionList.get(i));

                setOngoingContent(connectionResult.connection.adapterName);
                progress().addToCurrent(1);
                publishStatus();

                try {
                    CommunicationBridge client = new CommunicationBridge(kuick());
                    long startTime = System.nanoTime();
                    ActiveConnection connection = client.connectWithHandshake(connectionResult.connection,
                            true);
                    connectionResult.pingTime = System.nanoTime() - startTime;
                    connectionResult.successful = true;

                    connection.getSocket().close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Comparator<ConnectionResult> connectionComparator = (resultFirst, resultLast) -> {
                // make sure we are not comparing unsuccessful attempts with their pingTime values.
                if (resultFirst.successful != resultLast.successful)
                    return resultFirst.successful ? 1 : -1;

                return MathUtils.compare(resultLast.pingTime, resultFirst.pingTime);
            };

            Arrays.sort(results, connectionComparator);
        }

        if (hasAnchor())
            post(() -> getAnchor().onCalculationResult(results));
    }

    public static List<ConnectionResult> getAvailableList(ConnectionResult[] results)
    {
        List<ConnectionResult> availableList = new ArrayList<>();
        for (ConnectionResult result : results)
            if (result.successful)
                availableList.add(result);
        return availableList;
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

    public interface CalculationResultListener extends AttachedTaskListener
    {
        void onCalculationResult(ConnectionResult[] connectionResults);
    }

    public static class ConnectionResult
    {
        public DeviceAddress connection;
        public long pingTime = 0; // nanoseconds

        public boolean successful = false;

        public ConnectionResult(DeviceAddress connection)
        {
            this.connection = connection;
        }
    }
}
