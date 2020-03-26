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
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableBgTask;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class DeviceIntroductionTask extends AttachableBgTask<AttachedTaskListener>
{
    public static final String TAG = DeviceIntroductionTask.class.getSimpleName();

    private int mPin;
    private NetworkDeviceListAdapter.InfoHolder mInfoHolder;

    public DeviceIntroductionTask(NetworkDeviceListAdapter.InfoHolder infoHolder, int pin)
    {
        mInfoHolder = infoHolder;
        mPin = pin;
    }

    @Override
    public void onRun()
    {
        TaskMessage.Callback retryCallback = (service, msg, action) -> rerun(service);

        try {
            InetAddress inetAddress = findAddress();

            if (inetAddress != null) {
                DeviceAddress deviceAddress = ConnectionUtils.setupConnection(getService(), this, inetAddress,
                        mPin, retryCallback);

                if (deviceAddress != null)
                    Log.d(TAG, "onRun: Found device - " + deviceAddress.device.nickname);
            }

            if (!isInterruptedByUser()) {
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

        /**if (isInterrupted())
            return;

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d("DeviceIntroductionTask", "onRun: rerun");
        onRun();*/
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

    private InetAddress findAddress()
    {
        ConnectionUtils utils = ConnectionUtils.getInstance(getService());
        Object object = mInfoHolder.object();

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
                return utils.establishHotspotConnection(this, mInfoHolder,
                        (delimiter, timePassed) -> timePassed >= AppConfig.DEFAULT_SOCKET_TIMEOUT_LARGE);
        } else if (object instanceof InetAddress)
            return (InetAddress) object;
        else if (object instanceof DeviceConnection) {
            try {
                return ((DeviceConnection) object).toInet4Address();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
