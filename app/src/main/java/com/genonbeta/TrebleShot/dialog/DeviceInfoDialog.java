/*
 * Copyright (C) 2019 Veli Tasalı
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

package com.genonbeta.TrebleShot.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import com.genonbeta.TrebleShot.BuildConfig;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.DeviceLoader;
import com.genonbeta.android.database.exception.ReconstructionFailedException;

/**
 * Created by: veli
 * Date: 5/18/17 5:16 PM
 */

public class DeviceInfoDialog extends AlertDialog.Builder
{
    public static final String TAG = DeviceInfoDialog.class.getSimpleName();

    public DeviceInfoDialog(@NonNull final Activity activity, final Device device)
    {
        super(activity);

        final Kuick kuick = AppUtils.getKuick(activity);

        try {
            kuick.reconstruct(device);
        } catch (ReconstructionFailedException ignored) {
        }

        @SuppressLint("InflateParams")
        View rootView = LayoutInflater.from(activity).inflate(R.layout.layout_device_info, null);

        Device localDevice = AppUtils.getLocalDevice(activity);
        ImageView image = rootView.findViewById(R.id.image);
        TextView text1 = rootView.findViewById(R.id.text1);
        TextView notSupportedText = rootView.findViewById(R.id.notSupportedText);
        TextView modelText = rootView.findViewById(R.id.modelText);
        TextView versionText = rootView.findViewById(R.id.versionText);
        final SwitchCompat accessSwitch = rootView.findViewById(R.id.accessSwitch);
        final SwitchCompat trustSwitch = rootView.findViewById(R.id.trustSwitch);
        final boolean isDeviceNormal = Device.Type.NORMAL.equals(device.type);

        if (BuildConfig.PROTOCOL_VERSION_MIN > device.protocolVersionMin)
            notSupportedText.setVisibility(View.VISIBLE);

        DeviceLoader.showPictureIntoView(device, image, AppUtils.getDefaultIconBuilder(activity));
        text1.setText(device.username);
        modelText.setText(String.format("%s %s", device.brand.toUpperCase(), device.model.toUpperCase()));
        versionText.setText(device.versionName);
        accessSwitch.setChecked(!device.isBlocked);
        trustSwitch.setEnabled(!device.isBlocked);
        trustSwitch.setChecked(device.isTrusted);

        accessSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            device.isBlocked = !isChecked;
            kuick.publish(device);
            kuick.broadcast();
            trustSwitch.setEnabled(isChecked);
        });

        if (isDeviceNormal)
            trustSwitch.setOnCheckedChangeListener((button, isChecked) -> {
                device.isTrusted = isChecked;
                kuick.publish(device);
                kuick.broadcast();
            });
        else
            trustSwitch.setVisibility(View.GONE);

        setView(rootView);
        setPositiveButton(R.string.butn_close, null);
        setNegativeButton(R.string.butn_remove, (dialog, which) -> new RemoveDeviceDialog(activity, device).show());
    }
}
