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
import android.util.Log;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.protocol.communication.CommunicationException;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommonErrorHelper;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.TrebleShot.util.Transfers;
import com.genonbeta.android.database.exception.ReconstructionFailedException;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;

public class FindWorkingNetworkTask extends AttachableAsyncTask<FindWorkingNetworkTask.CalculationResultListener>
{
    private final Device device;

    public FindWorkingNetworkTask(Device device)
    {
        this.device = device;
    }

    @Override
    protected void onRun() throws TaskStoppedException
    {
        try {
            List<DeviceAddress> knownAddressList = AppUtils.getKuick(getContext()).castQuery(
                    Transfers.createAddressSelection(device.uid), DeviceAddress.class);

            progress().addToTotal(knownAddressList.size());
            publishStatus();

            if (knownAddressList.size() > 0) {
                for (DeviceAddress address : knownAddressList) {
                    throwIfStopped();
                    setOngoingContent(address.getHostAddress());
                    progress().addToCurrent(1);
                    publishStatus();

                    try (CommunicationBridge client = CommunicationBridge.connect(kuick(), address, device, 0)) {
                        client.requestAcquaintance();
                        if (client.receiveResult()) {
                            CalculationResultListener anchor = getAnchor();
                            if (anchor != null)
                                post(() -> anchor.onCalculationResult(device, address));
                            return;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            CalculationResultListener anchor = getAnchor();
            if (anchor != null)
                post(() -> anchor.onCalculationResult(device, null));
        } catch(Exception e) {
            post(CommonErrorHelper.messageOf(getContext(), e));
        }
    }

    @Override
    public String getName(Context context)
    {
        return context.getString(R.string.text_findAvailableNetwork);
    }

    public interface CalculationResultListener extends AttachedTaskListener
    {
        void onCalculationResult(Device device, @Nullable DeviceAddress address);
    }
}
