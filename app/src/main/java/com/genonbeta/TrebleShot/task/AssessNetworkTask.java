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
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableBgTask;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.TrebleShot.util.Transfers;
import com.genonbeta.android.framework.util.MathUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AssessNetworkTask extends AttachableBgTask<AssessNetworkTask.CalculationResultListener>
{
    private final Device device;

    public AssessNetworkTask(Device device)
    {
        this.device = device;
    }

    @Override
    protected void onRun() throws TaskStoppedException
    {
        List<DeviceAddress> knownConnectionList = AppUtils.getKuick(getService()).castQuery(
                Transfers.createAddressSelection(device.uid), DeviceAddress.class);
        ConnectionResult[] results = new ConnectionResult[knownConnectionList.size()];

        progress().addToTotal(knownConnectionList.size());
        publishStatus();

        if (results.length > 0) {
            for (int i = 0; i < results.length; i++) {
                throwIfStopped();

                ConnectionResult connectionResult = results[i] = new ConnectionResult(knownConnectionList.get(i));

                setOngoingContent(connectionResult.address.getHostAddress());
                progress().addToCurrent(1);
                publishStatus();
                long startTime = System.nanoTime();

                try (CommunicationBridge client = CommunicationBridge.connect(kuick(), connectionResult.address,
                        device, 0)) {
                    connectionResult.pingTime = System.nanoTime() - startTime;
                    connectionResult.successful = true;
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

        CalculationResultListener anchor = getAnchor();
        if (anchor != null)
            post(() -> anchor.onCalculationResult(results));
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
        public DeviceAddress address;
        public long pingTime = 0; // nanoseconds

        public boolean successful = false;

        public ConnectionResult(DeviceAddress address)
        {
            this.address = address;
        }
    }
}
