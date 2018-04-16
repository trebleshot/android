package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.app.Fragment;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.ui.callback.NetworkDeviceSelectedListener;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.Interrupter;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.ConnectException;
import java.util.List;

/**
 * created by: veli
 * date: 12/04/18 17:21
 */
public class CodeConnectFragment
		extends Fragment
		implements TitleSupport
{
	public static final String TAG = "CodeConnectFragment";

	public static final int WORKER_TASK_CONNECT_TS_NETWORK = 1;

	private BarcodeView mBarcodeView;
	private ConnectionUtils mConnectionUtils;
	private TextView mConductText;
	private ImageView mConductImage;
	private View mConductContainer;
	private View mTaskContainer;
	private AppCompatButton mTaskInterruptButton;
	private IntentFilter mIntentFilter = new IntentFilter();
	private NetworkDeviceSelectedListener mDeviceSelectedListener;

	private BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
					|| ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction()))
				updateState();
		}
	};

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mConnectionUtils = new ConnectionUtils(getContext());

		mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.layout_code_connect, container, false);

		mBarcodeView = view.findViewById(R.id.layout_code_connect_barcode_view);
		mConductText = view.findViewById(R.id.layout_code_connect_conduct_text);
		mConductImage = view.findViewById(R.id.layout_code_connect_conduct_image);
		mConductContainer = view.findViewById(R.id.container_conduct);
		mTaskContainer = view.findViewById(R.id.container_task);
		mTaskInterruptButton = view.findViewById(R.id.task_interrupter_button);

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		mBarcodeView.decodeContinuous(new BarcodeCallback()
		{
			@Override
			public void barcodeResult(BarcodeResult result)
			{
				try {
					JSONObject jsonObject = new JSONObject(result.getResult().getText());
					NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork = new NetworkDeviceListAdapter.HotspotNetwork();

					if (jsonObject.has(Keyword.NETWORK_NAME)) {
						hotspotNetwork.SSID = jsonObject.getString(Keyword.NETWORK_NAME);
						hotspotNetwork.qrConnection = true;

						boolean passProtected = jsonObject.has(Keyword.NETWORK_PASSWORD);

						if (passProtected) {
							hotspotNetwork.password = jsonObject.getString(Keyword.NETWORK_PASSWORD);
							hotspotNetwork.keyManagement = jsonObject.getInt(Keyword.NETWORK_KEYMGMT);
						}

						communicate(hotspotNetwork);
					} else if (jsonObject.has(Keyword.NETWORK_ADDRESS_IP)) {
						String bssid = jsonObject.getString(Keyword.NETWORK_ADDRESS_BSSID);
						String ipAddress = jsonObject.getString(Keyword.NETWORK_ADDRESS_IP);

						WifiInfo wifiInfo = mConnectionUtils.getWifiManager().getConnectionInfo();

						if (wifiInfo != null
								&& wifiInfo.getBSSID() != null
								&& wifiInfo.getBSSID().equals(bssid))
							communicate(ipAddress);
						else
							createSnackbar(R.string.mesg_errorNotSameNetwork)
									.show();
					}
				} catch (JSONException e) {
					e.printStackTrace();

					createSnackbar(R.string.mesg_somethingWentWrong)
							.show();
				}
			}

			@Override
			public void possibleResultPoints(List<ResultPoint> resultPoints)
			{

			}
		});
	}

	@Override
	public void onResume()
	{
		super.onResume();
		updateState();

		getContext().registerReceiver(mReceiver, mIntentFilter);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		mBarcodeView.pauseAndWait();

		getContext().unregisterReceiver(mReceiver);
	}

	protected void communicate(final Object object)
	{
		WorkerService.run(getContext(), new WorkerService.RunningTask(TAG, WORKER_TASK_CONNECT_TS_NETWORK)
		{
			private boolean mConnected = false;
			private String mRemoteAddress;

			@Override
			public void onRun()
			{
				if (getActivity() != null)
					getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							updateTaskStarted(getInterrupter());
						}
					});

				if (object instanceof NetworkDeviceListAdapter.HotspotNetwork) {
					mRemoteAddress = mConnectionUtils.establishHotspotConnection(getInterrupter(), (NetworkDeviceListAdapter.HotspotNetwork) object, new ConnectionUtils.TimeoutListener()
					{
						@Override
						public boolean onTimePassed(int delimiter, long timePassed)
						{
							return timePassed >= 20000;
						}
					});
				} else if (object instanceof String)
					mRemoteAddress = (String) object;

				if (mRemoteAddress != null) {
					try {
						NetworkDeviceLoader.load(true, getDatabase(), mRemoteAddress, new NetworkDeviceLoader.OnDeviceRegisteredErrorListener()
						{
							@Override
							public void onError(Exception error)
							{
							}

							@Override
							public void onDeviceRegistered(AccessDatabase database, NetworkDevice device, final NetworkDevice.Connection connection)
							{
								mConnected = true;

								if (object instanceof NetworkDeviceListAdapter.HotspotNetwork) {
									try {
										NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork = (NetworkDeviceListAdapter.HotspotNetwork) object;

										hotspotNetwork.deviceId = device.deviceId;
										getDatabase().reconstruct(hotspotNetwork);

										device = hotspotNetwork;
									} catch (Exception e) {
										e.printStackTrace();
									}
								}

								final NetworkDevice finalDevice = device;

								if (!getInterrupter().interrupted() && getActivity() != null)
									getActivity().runOnUiThread(new Runnable()
									{
										@Override
										public void run()
										{
											if (mDeviceSelectedListener != null)
												mDeviceSelectedListener.onNetworkDeviceSelected(finalDevice, connection);
										}
									});
							}
						});
					} catch (ConnectException e) {
						e.printStackTrace();
					}
				}

				if (!mConnected) {
					createSnackbar(R.string.mesg_connectionFailure)
							.show();
				}

				if (getActivity() != null)
					getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							updateTaskStopped();
						}
					});

				// We can't add dialog outside of the else statement as it may close other dialogs as well
			}
		});
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_connect);
	}

	public void setDeviceSelectedListener(NetworkDeviceSelectedListener listener)
	{
		mDeviceSelectedListener = listener;
	}

	public void updateState()
	{
		boolean wifiEnabled = mConnectionUtils.getWifiManager().isWifiEnabled();

		mConductImage.setImageResource(wifiEnabled
				? R.drawable.ic_crop_free_white_144dp
				: R.drawable.ic_signal_wifi_off_white_144dp);

		mConductText.setText(wifiEnabled
				? R.string.text_scanQRCodeHelp
				: R.string.text_scanQRWifiRequired);

		if (wifiEnabled)
			mBarcodeView.resume();
		else
			mBarcodeView.pauseAndWait();
	}

	public void updateTaskStarted(final Interrupter interrupter)
	{
		mBarcodeView.pauseAndWait();
		mConductContainer.setVisibility(View.GONE);
		mTaskContainer.setVisibility(View.VISIBLE);

		mTaskInterruptButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				interrupter.interrupt();
			}
		});
	}

	public void updateTaskStopped()
	{
		mBarcodeView.resume();
		mConductContainer.setVisibility(View.VISIBLE);
		mTaskContainer.setVisibility(View.GONE);

		mTaskInterruptButton.setOnClickListener(null);
	}
}
