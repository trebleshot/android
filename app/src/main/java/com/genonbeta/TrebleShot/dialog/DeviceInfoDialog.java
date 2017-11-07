package com.genonbeta.TrebleShot.dialog;

import android.annotation.SuppressLint;
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
import com.genonbeta.TrebleShot.activity.PendingTransferListActivity;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.TrebleShot.util.NotificationUtils;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/18/17 5:16 PM
 */

public class DeviceInfoDialog extends AlertDialog.Builder
{
	public DeviceInfoDialog(@NonNull final Context context, final AccessDatabase database, final NotificationUtils utils, final NetworkDevice device)
	{
		super(context);

		try {
			database.reconstruct(device);

			@SuppressLint("InflateParams")
			View rootView = LayoutInflater.from(context).inflate(R.layout.layout_device_info, null);

			TextView modelText = (TextView) rootView.findViewById(R.id.device_info_brand_and_model);
			TextView ipText = (TextView) rootView.findViewById(R.id.device_info_ip_address);
			SwitchCompat accessSwitch = (SwitchCompat) rootView.findViewById(R.id.device_info_access_switcher);

			modelText.setText(device.brand.toUpperCase() + " " + device.model.toUpperCase());

			ArrayList<NetworkDevice.Connection> connections = database.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION)
					.setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=?", device.deviceId), NetworkDevice.Connection.class);

			if (connections.size() > 0)
				ipText.setText(connections.size() > 1 ?
						context.getResources().getQuantityString(R.plurals.text_availableConnections,
								connections.size(),
								connections.size()) : connections.get(0).ipAddress);

			accessSwitch.setChecked(!device.isRestricted);

			accessSwitch.setOnCheckedChangeListener(
					new CompoundButton.OnCheckedChangeListener()
					{
						@Override
						public void onCheckedChanged(CompoundButton button, boolean isChecked)
						{
							device.isRestricted = !isChecked;
							database.publish(device);
						}
					}
			);

			setTitle(device.user);
			setView(rootView);
			setPositiveButton(R.string.butn_close, null);
			setNeutralButton(R.string.butn_pendingTransfers, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int p2)
						{
							PendingTransferListActivity.startInstance(context, device.deviceId);
						}
					}
			);

			setNegativeButton(R.string.butn_remove, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					AlertDialog.Builder askPermission = new AlertDialog.Builder(context);

					askPermission.setTitle(R.string.ques_removeDevice);
					askPermission.setMessage(R.string.text_removeDeviceSummary);
					askPermission.setNegativeButton(R.string.butn_cancel, null);
					askPermission.setPositiveButton(R.string.butn_proceed, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							database.remove(device);
						}
					});

					askPermission.show();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
