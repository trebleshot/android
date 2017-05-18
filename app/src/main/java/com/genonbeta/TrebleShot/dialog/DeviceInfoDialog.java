package com.genonbeta.TrebleShot.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.DeviceRegistry;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.helper.NotificationUtils;

/**
 * Created by: veli
 * Date: 5/18/17 5:16 PM
 */

public class DeviceInfoDialog extends AlertDialog.Builder
{
	public DeviceInfoDialog(@NonNull final Context context, final DeviceRegistry registry, final NotificationUtils utils, final NetworkDevice device)
	{
		super(context);

		final NetworkDevice deviceUpdated = registry.getNetworkDevice(device.ip);

		View rootView = LayoutInflater.from(context).inflate(R.layout.layout_device_info, null);
		TextView modelText = (TextView) rootView.findViewById(R.id.device_info_brand_and_model);
		TextView ipText = (TextView) rootView.findViewById(R.id.device_info_ip_address);
		SwitchCompat accessSwitch = (SwitchCompat) rootView.findViewById(R.id.device_info_access_switcher);

		modelText.setText(device.brand.toUpperCase() + " " + device.model.toUpperCase());

		ipText.setText(device.availableConnections.length + " available");
		accessSwitch.setChecked(!device.isRestricted);

		accessSwitch.setOnCheckedChangeListener(
				new CompoundButton.OnCheckedChangeListener()
				{
					@Override
					public void onCheckedChanged(CompoundButton button, boolean isChecked)
					{
						registry.updateRestriction(device, !isChecked);
					}
				}
		);

		setTitle(device.user);
		setView(rootView);
		setNegativeButton(R.string.close, null);
		setNeutralButton(R.string.thread_queue_short, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int p2)
					{
						new PendingTransferListDialog(context, new Transaction(context), device.deviceId).show();
					}
				}
		);
	}
}
