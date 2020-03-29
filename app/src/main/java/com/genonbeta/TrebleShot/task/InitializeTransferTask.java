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

import com.genonbeta.CoolSocket.ActiveConnection;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.object.TransferAssignee;
import com.genonbeta.TrebleShot.service.backgroundservice.BackgroundTask;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.android.framework.util.Stoppable;
import org.json.JSONObject;

import java.io.IOException;

public class InitializeTransferTask extends BackgroundTask
{
    private Device mDevice;
    private DeviceConnection mConnection;
    private TransferAssignee mAssignee;

    public InitializeTransferTask(Device device, DeviceConnection connection, TransferAssignee assignee)
    {
        mDevice = device;
        mConnection = connection;
        mAssignee = assignee;
    }

    @Override
    protected void onRun()
    {
        CommunicationBridge.Client client = new CommunicationBridge.Client(kuick());

        try (final ActiveConnection activeConnection = client.communicate(mDevice, mConnection)) {
            Stoppable.Closer connectionCloser = userAction -> {
                try {
                    activeConnection.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

            addCloser(connectionCloser);

            JSONObject jsonRequest = new JSONObject()
                    .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER_JOB)
                    .put(Keyword.TRANSFER_GROUP_ID, mAssignee.groupId);

            activeConnection.reply(jsonRequest.toString());

            final JSONObject responseJSON = new JSONObject(activeConnection.receive().index);
            activeConnection.getSocket().close();
            removeCloser(connectionCloser);

            // FIXME: 21.03.2020 How to achieve these back?
            /*
            if (!responseJSON.getBoolean(Keyword.RESULT) && !activity.isFinishing())
                activity.runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    @StringRes int msg = R.string.mesg_somethingWentWrong;
                    String errorMsg = Keyword.ERROR_UNKNOWN;

                    try {
                        errorMsg = responseJSON.getString(Keyword.ERROR);
                    } catch (JSONException e) {
                        // do nothing
                    }

                    switch (errorMsg) {
                        case Keyword.ERROR_NOT_FOUND:
                            msg = R.string.mesg_notValidTransfer;
                            break;
                        case Keyword.ERROR_REQUIRE_TRUST:
                            msg = R.string.mesg_errorNotTrusted;
                            break;
                        case Keyword.ERROR_NOT_ALLOWED:
                            msg = R.string.mesg_notAllowed;
                            break;
                    }

                    builder.setMessage(getService().getString(msg));
                    builder.setNegativeButton(R.string.butn_close, null);
                    builder.setPositiveButton(R.string.butn_retry,
                            (dialog, which) -> rerun(AppUtils.getBgService(dialog)));

                    builder.show();
                });

             */
        } catch (Exception e) {
            // FIXME: 21.03.2020
            /*
            if (!activity.isFinishing())
                activity.runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                    builder.setMessage(getService().getString(R.string.mesg_connectionFailure));
                    builder.setNegativeButton(R.string.butn_close, null);

                    builder.setPositiveButton(R.string.butn_retry,
                            (dialog, which) -> rerun(AppUtils.getBgService(dialog)));

                    builder.show();
                });
             */
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
