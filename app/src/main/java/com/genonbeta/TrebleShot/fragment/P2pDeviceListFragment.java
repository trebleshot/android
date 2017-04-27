package com.genonbeta.TrebleShot.fragment;

import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.P2pDeviceListAdapter;
import com.genonbeta.TrebleShot.helper.NotificationUtils;

import java.util.ArrayList;

public class P2pDeviceListFragment extends ListFragment
{
	private P2pDeviceListAdapter mListAdapter;
	private NotificationUtils mNotification;
	private DefaultClassListener mListener = new DefaultClassListener();
	private WifiP2pManager mManager;
	private WifiP2pManager.Channel mChannel;
	private IntentFilter mIntentFilter = new IntentFilter();
	private P2pStatusReceiver mReceiver = new P2pStatusReceiver();
	private ArrayList<WifiP2pDevice> mPeers = new ArrayList<WifiP2pDevice>();

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mListAdapter = new P2pDeviceListAdapter(getActivity(), mPeers);
		setListAdapter(mListAdapter);

		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

		mManager = (WifiP2pManager) getActivity().getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mManager.initialize(getActivity(), getActivity().getMainLooper(), null);
		mNotification = new NotificationUtils(getActivity());
	}

	@Override
	public void onResume()
	{
		super.onResume();
		getActivity().registerReceiver(mReceiver, mIntentFilter);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getActivity().unregisterReceiver(mReceiver);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		final WifiP2pDevice device = mPeers.get(position);

		if (device == null)
			return;

		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;
		config.wps.setup = WpsInfo.PBC;

		if (device.status == WifiP2pDevice.INVITED)
			mManager.cancelConnect(mChannel, mListener);
		else if (device.status != WifiP2pDevice.UNAVAILABLE && device.status != WifiP2pDevice.CONNECTED)
			mManager.connect(mChannel, config, mListener);
		else
		{
			mNotification.showToast(getActivity().getString(R.string.device_not_be_managed_msg, device.deviceName));
		}
	}

	public void discoverPeers()
	{
		mManager.discoverPeers(mChannel, mListener);
	}

	protected class DefaultClassListener implements WifiP2pManager.PeerListListener, WifiP2pManager.ActionListener
	{
		@Override
		public void onSuccess()
		{
			mNotification.showToast(R.string.in_process_msg);
		}

		@Override
		public void onFailure(int p1)
		{
			mNotification.showToast(R.string.process_failure_msg_p2p);
		}

		@Override
		public void onPeersAvailable(WifiP2pDeviceList peerList)
		{
			mPeers.clear();
			mPeers.addAll(peerList.getDeviceList());

			mListAdapter.notifyDataSetChanged();
		}
	}

	public class P2pStatusReceiver extends BroadcastReceiver
	{

		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();

			if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action))
			{
				int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

				if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
				{
					discoverPeers();
				}
			}
			else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action))
				if (mManager != null)
					mManager.requestPeers(mChannel, mListener);
				else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action))
				{
					NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

					// if (networkInfo.isConnected())

				}
				else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action))
				{
					WifiP2pDevice thisDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
				}
		}
	}
}
