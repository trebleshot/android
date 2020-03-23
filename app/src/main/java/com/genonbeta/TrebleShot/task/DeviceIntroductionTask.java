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

import android.content.DialogInterface;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableBgTask;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.util.AppUtils;

import java.net.InetAddress;

public class DeviceIntroductionTask extends AttachableBgTask<AttachedTaskListener>
{
    private boolean mConnected = false;
    private InetAddress mAddress;
    private NetworkDeviceListAdapter.InfoHolder mObject;

    public DeviceIntroductionTask(NetworkDeviceListAdapter.InfoHolder object)
    {
        mObject = object;
    }

    @Override
    public void onRun()
    {
        final DialogInterface.OnClickListener retryCallback = (dialog, which) -> rerun(AppUtils.getBgService(dialog));

        /*
        try {
            if (mObject instanceof NetworkDeviceListAdapter.NetworkSpecifier) {
                boolean canContinue = true;

                if (mObject instanceof NetworkDeviceListAdapter.NetworkSuggestion) {
                    // We might have used WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION intent
                    // to proceed, but as we are already going to do concurrent task, it should become available
                    // during that period.
                    final int status = getConnectionUtils().suggestNetwork((NetworkDeviceListAdapter.NetworkSuggestion) mObject);

                    if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
                            && status != WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE) {
                        canContinue = false;

                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity)
                                .setTitle(R.string.text_error)
                                .setNegativeButton(R.string.butn_close, null);

                        switch (status) {
                            case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP:
                                dialogBuilder.setMessage(R.string.text_errorExceededMaximumSuggestions)
                                        .setPositiveButton(R.string.butn_openSettings,
                                                (dialog, which) -> activity.startActivity(new Intent(
                                                        Settings.ACTION_WIFI_SETTINGS)));
                                break;
                            case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED:
                                dialogBuilder.setMessage(R.string.text_errorNetworkSuggestionsDisallowed)
                                        .setPositiveButton(R.string.butn_openSettings,
                                                (dialog, which) -> AppUtils.startApplicationDetails(activity));

                            case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL:
                                dialogBuilder.setMessage(R.string.text_errorNetworkSuggestionInternal)
                                        .setPositiveButton(R.string.butn_feedbackContact,
                                                (dialog, which) -> AppUtils.startFeedbackActivity(activity));
                        }

                        postDialog(activity, dialogBuilder.create());
                    }
                }

                if (canContinue) {
                    mAddress = getConnectionUtils().establishHotspotConnection(this,
                            (NetworkDeviceListAdapter.NetworkSpecifier<?>) object,
                            (delimiter, timePassed) -> timePassed >= 30000);
                }
            } else if (object instanceof InetAddress)
                mAddress = (InetAddress) object;
            else if (object instanceof DeviceConnection)
                mAddress = ((DeviceConnection) object).toInet4Address();

            if (mAddress != null) {
                mConnected = setupConnection(activity, mAddress, accessPin, (database, device, connection) -> {
                    // we may be working with direct IP scan
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (registerListener != null)
                            registerListener.onDeviceRegistered(database, device, connection);
                    });
                }, retryCallback) != null;
            }

            if (!mConnected && !isInterruptedByUser()) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity)
                        .setMessage(R.string.mesg_connectionFailure)
                        .setNegativeButton(R.string.butn_close, null)
                        .setPositiveButton(R.string.butn_retry, retryCallback);

                if (object instanceof NetworkDevice)
                    dialogBuilder.setTitle(((NetworkDevice) object).nickname);

                postDialog(activity, dialogBuilder.create());
            }
        } catch (Exception ignored) {

        } finally {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (task != null && !activity.isFinishing())
                    task.updateTaskStopped();
            });
        }*/

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
