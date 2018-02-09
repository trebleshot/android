package com.genonbeta.TrebleShot.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.DeviceScannerService;
import com.genonbeta.TrebleShot.util.AddressedInterface;
import com.genonbeta.TrebleShot.util.Interrupter;
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
	private ArrayList<AddressedInterface> mNetworkInterfaces = new ArrayList<>();
	private AlertDialog mDialog;
	private AccessDatabase mDatabase;
	private NetworkDevice mNetworkDevice;
	private ConnectionListAdapter mAdapter;
	private OnDeviceSelectedListener mDeviceSelectedListener;
	private Activity mActivity;

	public ConnectionChooserDialog(Activity activity, AccessDatabase database, NetworkDevice networkDevice, final OnDeviceSelectedListener listener, boolean refreshProvided)
	{
		super(activity);

		mAdapter = new ConnectionListAdapter();
		mActivity = activity;
		mDatabase = database;
		mNetworkDevice = networkDevice;
		mDeviceSelectedListener = listener;

		setAdapter(mAdapter, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				mDeviceSelectedListener.onDeviceSelected(mConnections.get(which), mConnections);
			}
		});

		setTitle(getContext().getString(R.string.text_availableNetworks, networkDevice.nickname));
		setNegativeButton(R.string.butn_cancel, null);

		if (!refreshProvided)
			setPositiveButton(R.string.text_refresh, null);
	}

	@Override
	public AlertDialog show()
	{
		mAdapter.notifyDataSetChanged();

		if (mConnections.size() > 0) {
			setMessage(null);
			setNeutralButton(R.string.butn_feelLucky, null);
		} else
			setMessage(R.string.text_noNetworkAvailable);

		mDialog = super.show();

		startRefreshing();

		Button buttonPositive = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);

		if (buttonPositive != null)
			buttonPositive.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					getContext().startService(new Intent(getContext(), DeviceScannerService.class)
							.setAction(DeviceScannerService.ACTION_SCAN_DEVICES));
				}
			});

		mDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
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

						@SuppressLint("UseSparseArrays") final HashMap<Integer, NetworkDevice.Connection> calculatedConnections = new HashMap<>();

						for (NetworkDevice.Connection connection : mConnections) {
							if (interrupter.interrupted())
								break;

							feelLucky.setProgress(feelLucky.getProgress() + 1);

							final NetworkDevice.Connection finalConnection = connection;

							if (!NetworkUtils.ping(connection.ipAddress, 500))
								continue;

							Integer calculatedTime = CoolSocket.connect(new CoolSocket.Client.ConnectionHandler()
							{
								@Override
								public void onConnect(CoolSocket.Client client)
								{
									int outTime = -1;

									try {
										long startTime = System.currentTimeMillis();
										final CoolSocket.ActiveConnection activeConnection = client.connect(new InetSocketAddress(finalConnection.ipAddress, AppConfig.COMMUNICATION_SERVER_PORT), 2000);

										interrupter.addCloser(new Interrupter.Closer()
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

								sorryDialog.show();
							} else {
								final ArrayList<Integer> comparedList = new ArrayList<>(calculatedConnections.keySet());

								Collections.sort(comparedList, new Comparator<Integer>()
								{
									@Override
									public int compare(Integer integer1, Integer integer2)
									{
										return integer1 < integer2 ? -1 : 1;
									}
								});

								if (mActivity != null)
									mActivity.runOnUiThread(new Runnable()
									{
										@Override
										public void run()
										{
											mDeviceSelectedListener.onDeviceSelected(calculatedConnections.get(comparedList.get(0)), mConnections);
										}
									});

								mDialog.cancel();
							}

						Looper.loop();
					}
				}.start();
			}
		});

		return mDialog;
	}

	public void startRefreshing()
	{
		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				if (mActivity != null && mDialog != null && mDialog.isShowing()) {
					mActivity.runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							boolean previousState = mConnections.size() > 0;

							mAdapter.notifyDataSetChanged();

							if (previousState != (mConnections.size() > 0)) {
								if (mDialog != null && mDialog.isShowing())
									mDialog.cancel();
								show();
							}
						}
					});

					startRefreshing();
				}
			}
		}, 2000);
	}

	public abstract static class OnDeviceSelectedListener
	{
		public abstract void onDeviceSelected(NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> availableInterfaces);
	}

	private class ConnectionListAdapter extends BaseAdapter
	{
		public ConnectionListAdapter()
		{
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

			boolean accessible = false;

			for (AddressedInterface addressedInterface : mNetworkInterfaces)
				if (address.adapterName.equals(addressedInterface.getNetworkInterface().getDisplayName())) {
					accessible = true;
					break;
				}

			textView1.setTextColor(ContextCompat.getColor(getContext(), accessible ? R.color.colorAccent : R.color.unavailableConnection));

			int availableName = TextUtils.getAdapterName(address);

			if (availableName == -1)
				textView1.setText(address.adapterName);
			else
				textView1.setText(availableName);

			textView2.setText(address.ipAddress);
			textView3.setText(TimeUtils.getTimeAgo(getContext(), address.lastCheckedDate));

			return convertView;
		}

		@Override
		public void notifyDataSetChanged()
		{
			mConnections.clear();
			mNetworkInterfaces.clear();

			mConnections.addAll(mDatabase.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION)
					.setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=?", mNetworkDevice.deviceId)
					.setOrderBy(AccessDatabase.FIELD_DEVICECONNECTION_LASTCHECKEDDATE + " DESC"), NetworkDevice.Connection.class));

			mNetworkInterfaces.addAll(NetworkUtils.getInterfaces(true, AppConfig.DEFAULT_DISABLED_INTERFACES));

			super.notifyDataSetChanged();
		}
	}
}
