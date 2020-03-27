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

import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableBgTask;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.communicationbridge.CommunicationException;
import org.json.JSONException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import static com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter.NetworkDescription;

public class DeviceIntroductionTask extends AttachableBgTask<AttachedTaskListener>
{
    public static final String TAG = DeviceIntroductionTask.class.getSimpleName();

    private NetworkDescription mDescription;
    private InetAddress mAddress;
    private int mPin;

    public DeviceIntroductionTask(InetAddress address, int pin)
    {
        assert address != null;

        mAddress = address;
        mPin = pin;
    }

    public DeviceIntroductionTask(DeviceConnection connection, int pin) throws UnknownHostException
    {
        this(connection.toInet4Address(), pin);
    }

    public DeviceIntroductionTask(NetworkDescription description, int pin)
    {
        assert description != null;

        mDescription = description;
        mPin = pin;
    }

    @Override
    public void onRun()
    {
        TaskMessage.Callback retryCallback = (service, msg, action) -> rerun(service);

        try {
            if (mAddress == null)
                connectToNetwork();

            DeviceAddress deviceAddress = ConnectionUtils.setupConnection(getService(), mAddress, mPin);
            Log.d(TAG, "onRun: Found device - " + deviceAddress.device.nickname);
        } catch (CommunicationException ignored) {

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (SuggestNetworkException e) {
            e.printStackTrace();

            /*
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
                */
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ConnectionUtils.WifiInaccessibleException e) {
            e.printStackTrace();
        }

        if (isInterrupted())
            return;

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d("DeviceIntroductionTask", "onRun: rerun");
        onRun();
    }

    private void connectToNetwork() throws SuggestNetworkException, ConnectionUtils.WifiInaccessibleException,
            TimeoutException, InterruptedException
    {
        ConnectionUtils utils = new ConnectionUtils(getService());

        // TODO: 27.03.2020 We might have used WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION intent
        // to proceed, but as we are already going to do concurrent task, it should become available
        // during that period.
        if (Build.VERSION.SDK_INT >= 29) {
            int status = utils.suggestNetwork(mDescription);
            switch (status) {
                case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP:
                    throw new SuggestNetworkException(SuggestNetworkException.Type.ExceededLimit);
                case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED:
                    throw new SuggestNetworkException(SuggestNetworkException.Type.AppDisallowed);
                case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL:
                    throw new SuggestNetworkException(SuggestNetworkException.Type.ErrorInternal);
                case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE:
                case WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS:
                default:
                    //
            }
        }

        mAddress = utils.establishHotspotConnection(this, mDescription);
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

    private static class SuggestNetworkException extends Exception
    {
        Type mType;

        public SuggestNetworkException(Type type)
        {
            mType = type;
        }

        enum Type
        {
            ExceededLimit,
            ErrorInternal,
            AppDisallowed,
            NetworkDuplicate
        }
    }
}
