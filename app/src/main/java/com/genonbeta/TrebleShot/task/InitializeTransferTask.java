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

import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.object.TransferMember;
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import org.json.JSONObject;

public class InitializeTransferTask extends AsyncTask
{
    private final Device mDevice;
    private final DeviceAddress mAddress;
    private final TransferMember mMember;

    public InitializeTransferTask(Device device, DeviceAddress address, TransferMember member)
    {
        mDevice = device;
        mAddress = address;
        mMember = member;
    }

    @Override
    protected void onRun()
    {
        try (CommunicationBridge bridge = CommunicationBridge.connect(kuick(), mAddress, mDevice, 0)) {
            JSONObject jsonRequest = new JSONObject()
                    .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER_JOB)
                    .put(Keyword.TRANSFER_ID, mMember.transferId);

            bridge.getActiveConnection().reply(jsonRequest.toString());

            final JSONObject responseJSON = bridge.getActiveConnection().receive().getAsJson();

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
