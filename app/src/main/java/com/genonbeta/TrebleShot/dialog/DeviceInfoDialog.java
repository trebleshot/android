package com.genonbeta.TrebleShot.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.HomeActivity;
import com.genonbeta.TrebleShot.activity.TransactionActivity;
import com.genonbeta.TrebleShot.adapter.TransactionGroupListAdapter;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.io.DocumentFile;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.DynamicNotification;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.NotificationUtils;
import com.genonbeta.TrebleShot.util.UpdateUtils;
import com.genonbeta.android.database.SQLQuery;

import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/18/17 5:16 PM
 */

public class DeviceInfoDialog extends AlertDialog.Builder
{
	public static final String TAG = DeviceInfoDialog.class.getSimpleName();
	public static final int JOB_RECEIVE_UPDATE = 1;

	private AlertDialog mThisDialog;

	public DeviceInfoDialog(@NonNull final Activity activity, final AccessDatabase database, final NetworkDevice device)
	{
		super(activity);

		try {
			database.reconstruct(device);

			@SuppressLint("InflateParams")
			View rootView = LayoutInflater.from(activity).inflate(R.layout.layout_device_info, null);

			TextView notSupportedText = rootView.findViewById(R.id.device_info_not_supported_text);
			TextView modelText = rootView.findViewById(R.id.device_info_brand_and_model);
			TextView addressText = rootView.findViewById(R.id.device_info_ip_address);
			TextView versionText = rootView.findViewById(R.id.device_info_version);
			AppCompatButton getUpdateButton = rootView.findViewById(R.id.device_info_get_update_button);
			SwitchCompat accessSwitch = rootView.findViewById(R.id.device_info_access_switcher);
			final SwitchCompat trustSwitch = rootView.findViewById(R.id.device_info_trust_switcher);

			if (device.versionNumber < AppConfig.SUPPORTED_MIN_VERSION)
				notSupportedText.setVisibility(View.VISIBLE);

			getUpdateButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					new ConnectionChooserDialog(activity, database, device, new ConnectionChooserDialog.OnDeviceSelectedListener()
					{
						@Override
						public void onDeviceSelected(final NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> availableInterfaces)
						{
							WorkerService.run(activity, new WorkerService.NotifiableRunningTask(TAG, JOB_RECEIVE_UPDATE)
							{
								private DocumentFile mReceivedFile = null;

								@Override
								public void onUpdateNotification(DynamicNotification dynamicNotification, UpdateType updateType)
								{
									switch (updateType) {
										case Started:
											dynamicNotification.setContentText(getContext().getString(R.string.mesg_ongoingUpdateDownload))
													.setSmallIcon(android.R.drawable.stat_sys_download);
											break;
										case Done:
											dynamicNotification.setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

											if (mReceivedFile != null) {
												Intent openIntent = new Intent();

												try {
													openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
															.setAction(Intent.ACTION_VIEW);

													FileUtils.applySecureOpenIntent(getContext(), mReceivedFile, openIntent);
												} catch (Exception e) {
													e.printStackTrace();

													openIntent.setComponent(new ComponentName(getContext(), HomeActivity.class))
															.setAction(HomeActivity.ACTION_OPEN_RECEIVED_FILES)
															.putExtra(HomeActivity.EXTRA_FILE_PATH, FileUtils.getApplicationDirectory(getContext()).getUri());
												}

												dynamicNotification.setContentText(getContext().getString(R.string.mesg_updateDownloadComplete))
														.setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), openIntent, 0))
														.setAutoCancel(true);
											} else {
												dynamicNotification.setContentText(getContext().getString(R.string.mesg_somethingWentWrong))
														.setSmallIcon(R.drawable.ic_error_white_24dp);
											}
											break;
									}
								}

								@Override
								public void onRun()
								{
									try {
										mReceivedFile = UpdateUtils.receiveUpdate(activity, device, getInterrupter(), new UpdateUtils.OnConnectionReadyListener()
										{
											@Override
											public void onConnectionReady(ServerSocket socket)
											{
												CoolSocket.connect(new CoolSocket.Client.ConnectionHandler()
												{
													@Override
													public void onConnect(CoolSocket.Client client)
													{
														try {
															CoolSocket.ActiveConnection activeConnection = client.connect(new InetSocketAddress(connection.ipAddress, AppConfig.SERVER_PORT_COMMUNICATION), AppConfig.DEFAULT_SOCKET_TIMEOUT);
															activeConnection.reply(new JSONObject().put(Keyword.REQUEST, Keyword.BACK_COMP_REQUEST_SEND_UPDATE).toString());

															CoolSocket.ActiveConnection.Response response = activeConnection.receive();
															JSONObject responseJSON = new JSONObject(response.response);

															if (!responseJSON.has(Keyword.RESULT) || !responseJSON.getBoolean(Keyword.RESULT))
																throw new Exception("Not the answer we were looking for.");
														} catch (Exception e) {
															e.printStackTrace();
															getInterrupter().interrupt(false);
														}
													}
												});
											}
										});
									} catch (Exception e) {
										e.printStackTrace();
									} finally {
									}
								}
							});
						}
					}, false).show();
				}
			});

			modelText.setText(device.brand.toUpperCase() + " " + device.model.toUpperCase());
			versionText.setText(device.versionName);

			ArrayList<NetworkDevice.Connection> connections = database.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION)
					.setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=?", device.deviceId), NetworkDevice.Connection.class);

			if (connections.size() > 0)
				addressText.setText(connections.size() > 1 ?
						activity.getResources().getQuantityString(R.plurals.text_availableConnections,
								connections.size(),
								connections.size()) : connections.get(0).ipAddress);

			accessSwitch.setChecked(!device.isRestricted);
			trustSwitch.setChecked(device.isTrusted);

			accessSwitch.setOnCheckedChangeListener(
					new CompoundButton.OnCheckedChangeListener()
					{
						@Override
						public void onCheckedChanged(CompoundButton button, boolean isChecked)
						{
							device.isRestricted = !isChecked;
							database.publish(device);

							trustSwitch.setEnabled(isChecked);
						}
					}
			);

			trustSwitch.setOnCheckedChangeListener(
					new CompoundButton.OnCheckedChangeListener()
					{
						@Override
						public void onCheckedChanged(CompoundButton button, boolean isChecked)
						{
							device.isTrusted = isChecked;
							database.publish(device);
						}
					}
			);

			setTitle(device.nickname);
			setView(rootView);
			setPositiveButton(R.string.butn_close, null);
			setNeutralButton(R.string.butn_pendingTransfers, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int p2)
						{
							final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

							final TransactionGroupListAdapter adapter = new TransactionGroupListAdapter(getContext())
									.setSelect(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP)
											.setWhere(AccessDatabase.FIELD_TRANSFERGROUP_DEVICEID + "=?", device.deviceId));

							builder.setPositiveButton(R.string.butn_close, null);

							new Thread()
							{
								@Override
								public void run()
								{
									super.run();

									Looper.prepare();

									adapter.onUpdate(adapter.onLoad());
									adapter.notifyDataSetChanged();

									if (adapter.getCount() > 0) {
										builder.setAdapter(adapter, new DialogInterface.OnClickListener()
										{
											@Override
											public void onClick(DialogInterface dialogInterface, int i)
											{
												TransactionActivity.startInstance(getContext(), ((TransactionObject.Group) adapter.getItem(i)).groupId);
											}
										});
									} else
										builder.setMessage(R.string.text_listEmpty);

									builder.show();

									Looper.loop();
								}
							}.start();
						}
					}
			);

			setNegativeButton(R.string.butn_remove, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					AlertDialog.Builder askPermission = new AlertDialog.Builder(activity);

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

	@Override
	public AlertDialog show()
	{
		return mThisDialog = super.show();
	}
}
