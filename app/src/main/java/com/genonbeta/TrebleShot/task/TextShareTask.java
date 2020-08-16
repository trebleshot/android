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
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import org.json.JSONObject;
import org.monora.coolsocket.core.session.ActiveConnection;

public class TextShareTask extends AsyncTask
{
    private final Device mDevice;
    private final DeviceAddress mAddress;
    private final String mText;

    public TextShareTask(Device device, DeviceAddress address, String text)
    {
        mDevice = device;
        mAddress = address;
        mText = text;
    }

    @Override
    protected void onRun()
    {
        try (CommunicationBridge bridge = CommunicationBridge.connect(kuick(), mAddress, mDevice, 0)) {
            final JSONObject jsonRequest = new JSONObject()
                    .put(Keyword.REQUEST, Keyword.REQUEST_CLIPBOARD)
                    .put(Keyword.TRANSFER_TEXT, mText);

            ActiveConnection activeConnection = bridge.getActiveConnection();
            activeConnection.reply(jsonRequest.toString());

            JSONObject response = activeConnection.receive().getAsJson();

            if (bridge.receiveResult()) {
                // TODO: 31.03.2020 implement
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName()
    {
        return null;
    }
}
