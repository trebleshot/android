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
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.object.DeviceRoute;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException;
import com.genonbeta.TrebleShot.util.CommonErrorHelper;
import com.genonbeta.TrebleShot.util.ConnectionUtils;

import java.net.InetAddress;

import static com.genonbeta.TrebleShot.adapter.DeviceListAdapter.NetworkDescription;

public class DeviceIntroductionTask extends AttachableAsyncTask<DeviceIntroductionTask.ResultListener>
{
    public static final String TAG = DeviceIntroductionTask.class.getSimpleName();

    private final int mPin;
    private NetworkDescription mDescription;
    private InetAddress mAddress;

    public DeviceIntroductionTask(InetAddress address, int pin)
    {
        assert address != null;

        mAddress = address;
        mPin = pin;
    }

    public DeviceIntroductionTask(DeviceAddress address, int pin)
    {
        this(address.inetAddress, pin);
    }

    public DeviceIntroductionTask(NetworkDescription description, int pin)
    {
        assert description != null;

        mDescription = description;
        mPin = pin;
    }

    @Override
    public void onRun() throws TaskStoppedException
    {
        try {
            if (mAddress == null) {
                ConnectionUtils connectionUtils = new ConnectionUtils(getContext());
                mAddress = connectionUtils.connectToNetwork(this, mDescription);
            }

            DeviceRoute deviceRoute = ConnectionUtils.setupConnection(getContext(), mAddress, mPin);
            DeviceIntroductionTask.ResultListener anchor = getAnchor();
            if (anchor != null)
                post(() -> anchor.onDeviceReached(deviceRoute));
        } catch (Exception e) {
            e.printStackTrace();
            post(CommonErrorHelper.messageOf(getContext(), e));
        }
    }

    @Override
    public String getName(Context context)
    {
        return context.getString(R.string.text_addNewDevice);
    }

    public interface ResultListener extends AttachedTaskListener
    {
        void onDeviceReached(DeviceRoute deviceRoute);
    }

    public static class SuggestNetworkException extends Exception
    {
        public NetworkDescription description;
        public Type type;

        public SuggestNetworkException(NetworkDescription description, Type type)
        {
            this.description = description;
            this.type = type;
        }

        public enum Type
        {
            ExceededLimit,
            ErrorInternal,
            Duplicate,
            AppDisallowed,
            NetworkDuplicate,
            DidNotConnect
        }
    }
}
