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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.object.DeviceRoute;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableBgTask;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.util.CommonErrorHelper;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.communicationbridge.CommunicationException;
import org.json.JSONException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import static com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter.NetworkDescription;

public class DeviceIntroductionTask extends AttachableBgTask<DeviceIntroductionTask.ResultListener>
{
    public static final String TAG = DeviceIntroductionTask.class.getSimpleName();

    private NetworkDescription mDescription;
    private InetAddress mAddress;
    private int mPin;
    private BroadcastReceiver mReceiver = null;

    public DeviceIntroductionTask(InetAddress address, int pin)
    {
        assert address != null;

        mAddress = address;
        mPin = pin;
    }

    public DeviceIntroductionTask(DeviceAddress connection, int pin) throws UnknownHostException
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

            DeviceRoute deviceRoute = ConnectionUtils.setupConnection(getService(), mAddress, mPin);

            if (hasAnchor())
                post(() -> getAnchor().onDeviceReached(deviceRoute));
            Log.d(TAG, "onRun: Found device - " + deviceRoute.device.username);
        } catch (CommunicationException ignored) {

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (SuggestNetworkException e) {
            post(CommonErrorHelper.messageOf(e, getService()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ConnectionUtils.WifiInaccessibleException e) {
            e.printStackTrace();
        } finally {
            if (mReceiver != null)
                getService().unregisterReceiver(mReceiver);
        }
    }

    private void connectToNetwork() throws SuggestNetworkException, ConnectionUtils.WifiInaccessibleException,
            TimeoutException, InterruptedException
    {
        ConnectionUtils utils = new ConnectionUtils(getService());

        if (Build.VERSION.SDK_INT >= 29) {
            mReceiver = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    if (WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION.equals(intent.getAction()))
                        DeviceIntroductionTask.this.notify();
                }
            };

            getService().registerReceiver(mReceiver, new IntentFilter(
                    WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION));

            int status = utils.suggestNetwork(mDescription);
            switch (status) {
                case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP:
                    throw new SuggestNetworkException(mDescription, SuggestNetworkException.Type.ExceededLimit);
                case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED:
                    throw new SuggestNetworkException(mDescription, SuggestNetworkException.Type.AppDisallowed);
                case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL:
                    throw new SuggestNetworkException(mDescription, SuggestNetworkException.Type.ErrorInternal);
                case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE:
                case WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS:
                default:
                    setOngoingContent(getService().getString(R.string.mesg_connectingToSelfHotspot));
                    publishStatus();
                    wait(AppConfig.DEFAULT_SOCKET_TIMEOUT_LARGE);

                    if (!utils.isConnectedToNetwork(mDescription))
                        throw new SuggestNetworkException(mDescription, SuggestNetworkException.Type.DidNotConnect);
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
            AppDisallowed,
            NetworkDuplicate,
            DidNotConnect
        }
    }
}
