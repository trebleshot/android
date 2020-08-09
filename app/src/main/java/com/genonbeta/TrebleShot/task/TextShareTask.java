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
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.service.backgroundservice.BackgroundTask;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import org.json.JSONObject;
import org.monora.coolsocket.core.session.ActiveConnection;

public class TextShareTask extends BackgroundTask
{
    private final Device mDevice;
    private final DeviceConnection mConnection;
    private final String mText;

    public TextShareTask(Device device, DeviceConnection connection, String text)
    {
        mDevice = device;
        mConnection = connection;
        mText = text;
    }

    @Override
    protected void onRun() throws InterruptedException
    {
        CommunicationBridge bridge = new CommunicationBridge(kuick());

        try (ActiveConnection activeConnection = bridge.communicate(mDevice, mConnection)) {
            final JSONObject jsonRequest = new JSONObject()
                    .put(Keyword.REQUEST, Keyword.REQUEST_CLIPBOARD)
                    .put(Keyword.TRANSFER_CLIPBOARD_TEXT, mText);

            activeConnection.reply(jsonRequest.toString());

            JSONObject response = activeConnection.receive().getAsJson();

            if (response.has(Keyword.RESULT) && response.getBoolean(Keyword.RESULT)) {
                // TODO: 31.03.2020 implement
            } else
                ConnectionUtils.throwCommunicationError(response, mDevice);
        } catch (Exception e) {
            e.printStackTrace();
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
