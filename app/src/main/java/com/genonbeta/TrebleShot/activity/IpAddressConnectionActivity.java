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

package com.genonbeta.TrebleShot.activity;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter.InfoHolder;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpAddressConnectionActivity extends Activity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip_address_connection);

        final AppCompatEditText editText = findViewById(R.id.editText);
        findViewById(R.id.confirm_button).setOnClickListener((v) -> {
            final String ipAddress = editText.getText().toString();

            if (ipAddress.matches("([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})")) {
                try {
                    InetAddress address = InetAddress.getByName(ipAddress);
                    BackgroundService.run(this, new DeviceIntroductionTask(new InfoHolder(address), -1));
                } catch (UnknownHostException e) {
                    editText.setError(getString(R.string.mesg_unknownHostError));
                }
            } else
                editText.setError(getString(R.string.mesg_errorNotAnIpAddress));
        });
    }
}
