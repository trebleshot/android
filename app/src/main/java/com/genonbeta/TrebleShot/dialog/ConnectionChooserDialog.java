package com.genonbeta.TrebleShot.dialog;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.service.DeviceScannerService;
import com.genonbeta.TrebleShot.util.AddressedInterface;
import com.genonbeta.TrebleShot.util.Interrupter;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.TrebleShot.util.NetworkUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.android.database.SQLQuery;

import org.json.JSONException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

/**
 * Created by: veli
 * Date: 5/19/17 12:18 AM
 */

public class ConnectionChooserDialog extends AlertDialog.Builder
{
	private ArrayList<NetworkDevice.Connection> mConnections = new ArrayList<>();
	private NetworkDevice mNetworkDevice;
	private AccessDatabase mDatabase;
	private OnDeviceSelectedListener mDeviceSelectedListener;

	public ConnectionChooserDialog(Context context, AccessDatabase database, NetworkDevice networkDevice, final OnDeviceSelectedListener listener)
	{
		super(context);

		mDatabase = database;
		mNetworkDevice = networkDevice;
		mDeviceSelectedListener = listener;
	}

	@Override
	public AlertDialog show()
	{
		mConnections.clear();

		mConnections.addAll(mDatabase.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION)
				.setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=?", mNetworkDevice.deviceId)
				.setOrderBy(AccessDatabase.FIELD_DEVICECONNECTION_LASTCHECKEDDATE + " DESC"), NetworkDevice.Connection.class));

		if (mConnections.size() > 0) {
			setMessage(null);

			setAdapter(new ConnectionListAdapter(), new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					mDeviceSelectedListener.onDeviceSelected(mConnections.get(which), mConnections);
				}
			});

			setNeutralButton(R.string.butn_feelLucky, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialogInterface, int i)
				{
					final ProgressDialog feelLucky = new ProgressDialog(getContext());
					final Interrupter interrupter = new Interrupter();

					feelLucky.setTitle(R.string.text_feelLuckyOngoing);
					feelLucky.setMax(mConnections.size());
					feelLucky.setCancelable(false);
					feelLucky.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					feelLucky.setButton(ProgressDialog.BUTTON_NEGATIVE, getContext().getString(R.string.butn_cancel), new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							interrupter.interrupt();
						}
					});

					feelLucky.show();

					new Thread()
					{
						@Override
						public void run()
						{
							super.run();

							Looper.prepare();

							@SuppressLint("UseSparseArrays")
							HashMap<Integer, NetworkDevice.Connection> calculatedConnections = new HashMap<>();

							for (NetworkDevice.Connection connection : mConnections) {
								if (interrupter.interrupted())
									break;

								feelLucky.setProgress(feelLucky.getProgress() + 1);

								final NetworkDevice.Connection finalConnection = connection;

								Integer calculatedTime = CoolSocket.connect(new CoolSocket.Client.ConnectionHandler()
								{
									@Override
									public void onConnect(CoolSocket.Client client)
									{
										int outTime = -1;

										try {
											long startTime = System.currentTimeMillis();
											final CoolSocket.ActiveConnection activeConnection = client.connect(new InetSocketAddress(finalConnection.ipAddress, AppConfig.COMMUNICATION_SERVER_PORT), 2000);

											interrupter.useCloser(new Interrupter.Closer()
											{
												@Override
												public void onClose()
												{
													try {
														activeConnection.getSocket().close();
													} catch (IOException e) {
														e.printStackTrace();
													}
												}
											});

											activeConnection.reply(null);
											activeConnection.receive();

											outTime = (int) (System.currentTimeMillis() - startTime);
										} catch (IOException e) {
											e.printStackTrace();
										} catch (JSONException e) {
											e.printStackTrace();
										} catch (TimeoutException e) {
											e.printStackTrace();
										} finally {
											client.setReturn(outTime);
										}
									}
								}, Integer.class);

								if (calculatedTime != null && calculatedTime > -1)
									calculatedConnections.put(calculatedTime, connection);
							}

							feelLucky.cancel();

							if (!interrupter.interrupted())
								if (calculatedConnections.size() < 1) {
									AlertDialog.Builder sorryDialog = new AlertDialog.Builder(getContext());

									sorryDialog.setTitle(R.string.text_error);
									sorryDialog.setMessage(R.string.text_feelLuckyFailed);
									sorryDialog.setNegativeButton(R.string.butn_close, null);
									sorryDialog.setPositiveButton(R.string.butn_connectionList, new DialogInterface.OnClickListener()
									{
										@Override
										public void onClick(DialogInterface dialogInterface, int i)
										{
											show();
										}
									});

									sorryDialog.show();
								} else {
									ArrayList<Integer> comparedList = new ArrayList<>(calculatedConnections.keySet());

									Collections.sort(comparedList, new Comparator<Integer>()
									{
										@Override
										public int compare(Integer integer1, Integer integer2)
										{
											return integer1 < integer2 ? -1 : 1;
										}
									});

									mDeviceSelectedListener.onDeviceSelected(calculatedConnections.get(comparedList.get(0)), mConnections);
								}

							Looper.loop();
						}
					}.start();
				}
			});
		} else
			setMessage(R.string.text_noNetworkAvailable);

		setTitle(R.string.text_availableNetworks);
		setNegativeButton(R.string.butn_cancel, null);

		setPositiveButton(R.string.text_refresh, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialogInterface, int i)
			{
				getContext().startService(new Intent(getContext(), DeviceScannerService.class)
						.setAction(DeviceScannerService.ACTION_SCAN_DEVICES));
				show();
			}
		});


		return super.show();
	}

	public abstract static class OnDeviceSelectedListener
	{
		public abstract void onDeviceSelected(NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> availableInterfaces);
	}

	private class ConnectionListAdapter extends BaseAdapter
	{
		private ArrayList<AddressedInterface> mNetworkInterfaces;

		public ConnectionListAdapter()
		{
			mNetworkInterfaces = NetworkUtils.getInterfaces(true, AppConfig.DEFAULT_DISABLED_INTERFACES);
		}

		@Override
		public int getCount()
		{
			return mConnections.size();
		}

		@Override
		public Object getItem(int position)
		{
			return mConnections.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if (convertView == null)
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_available_interface, parent, false);

			NetworkDevice.Connection address = (NetworkDevice.Connection) getItem(position);

			TextView textView1 = convertView.findViewById(R.id.pending_available_interface_text1);
			TextView textView2 = convertView.findViewById(R.id.pending_available_interface_text2);
			TextView textView3 = convertView.findViewById(R.id.pending_available_interface_text3);

			for (AddressedInterface addressedInterface : mNetworkInterfaces)
				if (address.adapterName.equals(addressedInterface.getNetworkInterface().getDisplayName())) {
					textView1.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
					break;
				}

			int availableName = TextUtils.getAdapterName(address);

			if (availableName == -1)
				textView1.setText(address.adapterName);
			else
				textView1.setText(availableName);

			textView2.setText(address.ipAddress);
			textView3.setText(TimeUtils.getTimeAgo(getContext(), address.lastCheckedDate));

			return convertView;
		}
	}
}
