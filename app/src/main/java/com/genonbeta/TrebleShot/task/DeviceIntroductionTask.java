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

import android.app.Dialog;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter.NetworkSuggestion;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableBgTask;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableBgTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;

import java.net.InetAddress;

public class DeviceIntroductionTask extends AttachableBgTask<AttachedTaskListener>
{
    private boolean mConnected = false;
    private int mPin;
    private InetAddress mAddress;
    private NetworkDeviceListAdapter.InfoHolder mInfoHolder;

    public DeviceIntroductionTask(NetworkDeviceListAdapter.InfoHolder infoHolder, int pin)
    {
        mInfoHolder = infoHolder;
        mPin = pin;
    }

    @Override
    public void onRun()
    {
        // FIXME: 25.03.2020 rerun
        final TaskMessage.Callback retryCallback = (service, msg, action) -> rerun(service);
        final Object object = mInfoHolder.object();
        final ConnectionUtils utils = ConnectionUtils.getInstance(getService());

        try {
            if (object instanceof NetworkSuggestion) {
                // We might have used WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION intent
                // to proceed, but as we are already going to do concurrent task, it should become available
                // during that period.
                final int status = utils.suggestNetwork((NetworkSuggestion) object);

                if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
                        && status != WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE) {

                    TaskMessage message = TaskMessage.newInstance()
                            .setTitle(getService(), R.string.text_error)
                            .addAction(getService(), R.string.butn_close, null);

                    switch (status) {
                        case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP:
                            message.setMessage(getService(), R.string.text_errorExceededMaximumSuggestions)
                                    .addAction(getService(), R.string.butn_openSettings, Dialog.BUTTON_POSITIVE,
                                            (context, msg, action) -> context.startActivity(new Intent(
                                                    Settings.ACTION_WIFI_SETTINGS)));
                            break;
                        case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED:
                            message.setMessage(getService(), R.string.text_errorNetworkSuggestionsDisallowed)
                                    .addAction(getService(), R.string.butn_openSettings, Dialog.BUTTON_POSITIVE,
                                            (context, msg, action) -> AppUtils.startApplicationDetails(context));

                        case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL:
                            message.setMessage(getService(), R.string.text_errorNetworkSuggestionInternal)
                                    .addAction(getService(), R.string.butn_feedbackContact, Dialog.BUTTON_POSITIVE,
                                            (context, msg, action) -> AppUtils.startFeedbackActivity(context));
                    }

                    post(message);
                } else
                    mAddress = utils.establishHotspotConnection(this, mInfoHolder,
                            (delimiter, timePassed) -> timePassed >= 30000);
            } else if (object instanceof InetAddress)
                mAddress = (InetAddress) object;
            else if (object instanceof DeviceConnection)
                mAddress = ((DeviceConnection) object).toInet4Address();

            if (mAddress != null) {
                mConnected = utils.setupConnection(getService(), this, mAddress, mPin, new NetworkDeviceLoader
                        .OnDeviceRegisteredListener()
                {

                    @Override
                    public void onTaskStateChanged(BaseAttachableBgTask task)
                    {

                    }

                    @Override
                    public void onDeviceRegistered(Kuick kuick, NetworkDevice device, DeviceConnection connection)
                    {
                        Log.d(DeviceIntroductionTask.class.getSimpleName(), "onDeviceRegistered: " + device.id);
                    }
                }, retryCallback) != null;
            }

            if (!mConnected && !isInterruptedByUser()) {
                TaskMessage message = TaskMessage.newInstance()
                        .setMessage(getService(), R.string.mesg_connectionFailure)
                        .addAction(getService(), R.string.butn_close, Dialog.BUTTON_NEGATIVE, null)
                        .addAction(getService(), R.string.butn_retry, Dialog.BUTTON_POSITIVE, retryCallback);

                post(message);

                /*
                if (object instanceof NetworkDevice)
                    dialogBuilder.setTitle(((NetworkDevice) object).nickname);

                postDialog(activity, dialogBuilder.create());
                */
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public String getTitle()
    {
        return null;
    }
}
