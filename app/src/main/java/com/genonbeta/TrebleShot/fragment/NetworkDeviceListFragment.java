package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;

import com.genonbeta.CoolSocket.CoolCommunication;
import com.genonbeta.CoolSocket.CoolJsonCommunication;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.adapter.PendingProcessListAdapter;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.helper.NotificationPublisher;
import com.genonbeta.TrebleShot.receiver.DeviceScannerProvider;
import com.genonbeta.TrebleShot.service.CommunicationService;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.Socket;

public class NetworkDeviceListFragment extends ListFragment
{
	private IntentFilter mIntentFilter = new IntentFilter();
	private SelfReceiver mReceiver = new SelfReceiver();
	private PokeHandler mPokeHandler = new PokeHandler();
	private NetworkDeviceListAdapter mListAdapter;
	private NotificationPublisher mPublisher;
	private SharedPreferences mPreferences;
	private Menu mOptionsMenu;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mIntentFilter.addAction(DeviceScannerProvider.ACTION_DEVICE_FOUND);
		mIntentFilter.addAction(DeviceScannerProvider.ACTION_SCAN_STARTED);
		mIntentFilter.addAction(DeviceScannerProvider.ACTION_DEVICE_SCAN_COMPLETED);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mListAdapter = new NetworkDeviceListAdapter(getActivity());
		mPublisher = new NotificationPublisher(getActivity());
		mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

		getListView().setPadding(20, 0, 20, 0);
		
		setListAdapter(mListAdapter);
		setHasOptionsMenu(true);
		
		setEmptyText(getString(R.string.find_device_hint));

		if (mPreferences.getBoolean("developer_mode", false))
			getActivity().sendBroadcast(new Intent(DeviceScannerProvider.ACTION_ADD_IP).putExtra(DeviceScannerProvider.EXTRA_DEVICE_IP, "127.0.0.1"));

		if (mPreferences.getBoolean("scan_devices_auto", false))
			getActivity().sendBroadcast(new Intent(DeviceScannerProvider.ACTION_SCAN_DEVICES));
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		Intent actIntent = getActivity().getIntent();
		final NetworkDevice device = (NetworkDevice) mListAdapter.getItem(position);

		if (Intent.ACTION_SEND.equals(actIntent.getAction()) || Intent.ACTION_SEND_MULTIPLE.equals(actIntent.getAction()) || ShareActivity.ACTION_SEND.equals(actIntent.getAction()) || ShareActivity.ACTION_SEND_MULTIPLE.equals(actIntent.getAction()))
		{
			Intent serviceIntent = (Intent) actIntent.clone();

			serviceIntent.setClass(getActivity(), CommunicationService.class);
			serviceIntent.putExtra(CommunicationService.EXTRA_DEVICE_IP, device.ip);

			getActivity().startService(serviceIntent);
		}
		else if (device.brand != null && device.model != null)
		{
			AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

			dialog.setTitle(device.user);

			View rootView = getActivity().getLayoutInflater().inflate(R.layout.layout_device_info, null);

			TextView modelText = (TextView) rootView.findViewById(R.id.device_info_brand_and_model);
			TextView ipText = (TextView) rootView.findViewById(R.id.device_info_ip_address);
			SwitchCompat accessSwitch = (SwitchCompat) rootView.findViewById(R.id.device_info_access_switcher);

			modelText.setText(device.brand.toUpperCase() + " " + device.model.toUpperCase());
			ipText.setText(device.ip);
			accessSwitch.setChecked(!device.isRestricted);

			accessSwitch.setOnCheckedChangeListener(
				new OnCheckedChangeListener()
				{
					@Override
					public void onCheckedChanged(CompoundButton button, boolean isChecked)
					{
						device.isRestricted = !isChecked;
					}
				}
			);

			dialog.setNegativeButton(R.string.close, null);

			dialog.setPositiveButton(R.string.poke, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int p2)
					{
						CoolJsonCommunication.Messenger.send(device.ip, AppConfig.COMMUNATION_SERVER_PORT, null, mPokeHandler);
					}	
				}
			);

			dialog.setNeutralButton(R.string.thread_queue_short, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int p2)
					{
						AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

						final PendingProcessListAdapter adapter = new PendingProcessListAdapter(getActivity(), device.ip);

						dialog.setTitle(getString(R.string.thread_queue) + " - " + device.user);

						if (adapter.getCount() < 1)
							dialog.setMessage(R.string.list_empty_msg);
						else
							dialog.setAdapter(adapter, null);

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

			dialog.setView(rootView);			
			dialog.show();
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();

		mListAdapter.notifyDataSetChanged();
		getActivity().registerReceiver(mReceiver, mIntentFilter);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		this.checkScanStatus();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getActivity().unregisterReceiver(mReceiver);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.network_devices_options, menu);

		this.mOptionsMenu = menu;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.network_devices_scan:
				getActivity().sendBroadcast(new Intent(DeviceScannerProvider.ACTION_SCAN_DEVICES));
				return true;
			case R.id.network_devices_clear_list:
				ApplicationHelper.getDeviceList().clear();
				mListAdapter.notifyDataSetChanged();
				mPublisher.makeToast(R.string.device_list_cleared_msg);
				return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	private void checkScanStatus()
	{
		if (mOptionsMenu != null && mOptionsMenu.findItem(R.id.network_devices_scan) != null)
		{
			if (ApplicationHelper.getNetworkDeviceScanner().isScannerAvaiable()) 
				mOptionsMenu.findItem(R.id.network_devices_scan).setActionView(null);
			else
			{
				MenuItem searchItem = mOptionsMenu.findItem(R.id.network_devices_scan);
				searchItem.setActionView(R.layout.layout_running_process);
				View progressBar = searchItem.getActionView().findViewById(R.id.layout_running_process_progressBar);

				progressBar.setOnClickListener(
					new OnClickListener()
					{
						@Override
						public void onClick(View view)
						{
							mPublisher.makeToast(R.string.stopping_msg);
							ApplicationHelper.getNetworkDeviceScanner().interrupt();
						}
					}
				);
			}
		}
	}
	
	private void showSnackbar(int resId)
	{
		Snackbar.make(NetworkDeviceListFragment.this.getActivity().findViewById(android.R.id.content), resId, Snackbar.LENGTH_SHORT).show();
	}
	
	private class SelfReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			NetworkDeviceListFragment.this.checkScanStatus();

			if (DeviceScannerProvider.ACTION_DEVICE_FOUND.equals(intent.getAction()))
			{
				mListAdapter.notifyDataSetChanged();
			}
			else if (DeviceScannerProvider.ACTION_SCAN_STARTED.equals(intent.getAction()) && intent.hasExtra(DeviceScannerProvider.EXTRA_SCAN_STATUS))
			{
				String scanStatus = intent.getStringExtra(DeviceScannerProvider.EXTRA_SCAN_STATUS);

				if (DeviceScannerProvider.STATUS_OK.equals(scanStatus))
					showSnackbar(R.string.devices_scanning_msg);
				else if (DeviceScannerProvider.STATUS_NO_NETWORK_INTERFACE.equals(scanStatus))
					showSnackbar(R.string.no_network_interface_msg);
			}
			else if (DeviceScannerProvider.ACTION_DEVICE_SCAN_COMPLETED.equals(intent.getAction()))
			{
				showSnackbar(R.string.scan_completed);
			}
		}
	}
	
	protected class PokeHandler extends CoolJsonCommunication.JsonResponseHandler
	{
		@Override
		public void onJsonMessage(Socket socket, CoolCommunication.Messenger.Process process, JSONObject json)
		{
			try
			{
				json.put("request", "poke_the_device");
				
				JSONObject response = new JSONObject(process.waitForResponse());
				
				if (response.getBoolean("result"))
				{
					showToast(getString(R.string.poke_sent));
					return;
				}
				
				showToast(getString(R.string.poke_error, getString(R.string.not_allowed_error)));
			}
			catch (JSONException e)
			{
				showToast(getString(R.string.poke_error, getString(R.string.communication_problem)));
			}
		}

		@Override
		public void onError(Exception exception)
		{
			super.onError(exception);
			
			showToast(getString(R.string.poke_error, getString(R.string.connection_problem)));
			
			Looper.loop();
		}
		
		private void showToast(String text)
		{
			Looper.prepare();
			
			if (getActivity() != null)
				mPublisher.makeToast(text);
			
			Looper.loop();
		}
	}
}
