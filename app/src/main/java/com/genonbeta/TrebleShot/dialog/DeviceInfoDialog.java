package com.genonbeta.TrebleShot.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.genonbeta.CoolSocket.CoolCommunication;
import com.genonbeta.CoolSocket.CoolJsonCommunication;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.DeviceRegistry;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.helper.NotificationUtils;
import com.genonbeta.TrebleShot.service.Keyword;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.Socket;

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
		setNegativeButton(R.string.close, null);


		/*
		setNeutralButton(R.string.thread_queue_short, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int p2)
					{
						AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

						final PendingProcessListAdapter adapter = new PendingProcessListAdapter(getActivity(), device.ip);

						dialog.setTitle(context.getString(R.string.thread_queue, device.user));

						if (adapter.getCount() < 1)
							dialog.setMessage(R.string.list_empty_msg);
						else
							dialog.setAdapter(adapter, null);

						dialog.setNegativeButton(R.string.close, null);
						dialog.setNeutralButton(R.string.clear_queue, new DialogInterface.OnClickListener()
								{
									@Override
									public void onClick(DialogInterface dialogInterface, int p2)
									{
										adapter.clearQueue();
									}
								}
						);

						dialog.show();
					}
				}
		);
		*/

		setView(rootView);
	}
}
