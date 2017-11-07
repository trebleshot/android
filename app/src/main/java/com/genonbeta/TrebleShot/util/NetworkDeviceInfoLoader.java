package com.genonbeta.TrebleShot.util;

import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.service.Keyword;

import org.json.JSONObject;

import java.net.ConnectException;
import java.net.Socket;

public class NetworkDeviceInfoLoader
{
	private OnInfoAvailableListener mListener;

	public NetworkDeviceInfoLoader(OnInfoAvailableListener listener)
	{
		mListener = listener;
	}

	public NetworkDeviceInfoLoader()
	{
	}

	public void startLoading(final AccessDatabase database, final String ipAddress)
	{
		try {
			startLoading(false, database, ipAddress);
		} catch (ConnectException e) {
			e.printStackTrace();
		}
	}

	public NetworkDevice startLoading(boolean currentThread, final AccessDatabase database, final String ipAddress) throws ConnectException
	{
		final NetworkDevice device = new NetworkDevice();
		final JsonResponseHandler handler = new JsonResponseHandler()
		{
			@Override
			public void onConfigure(CoolCommunication.Messenger.Process process)
			{
				super.onConfigure(process);

				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onJsonMessage(Socket socket, CoolCommunication.Messenger.Process process, JSONObject json)
			{
			}

			@Override
			public void onResponseAvailable(String response)
			{
				try {
					JSONObject jsonResponse = new JSONObject(response);
					JSONObject deviceInfo = jsonResponse.getJSONObject(Keyword.DEVICE_INFO);

					device.brand = deviceInfo.getString(Keyword.BRAND);
					device.model = deviceInfo.getString(Keyword.MODEL);
					device.user = deviceInfo.getString(Keyword.USER);
					device.deviceId = deviceInfo.getString(Keyword.SERIAL);
					device.lastUsageTime = System.currentTimeMillis();
					device.buildNumber = jsonResponse
							.getJSONObject(Keyword.APP_INFO)
							.getInt(Keyword.VERSION_CODE);

					if (mListener != null)
						mListener.onInfoAvailable(database, device, ipAddress);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		try {
			if (currentThread) {
				CoolCommunication.Messenger.sendOnCurrentThread(ipAddress, AppConfig.COMMUNATION_SERVER_PORT, null, handler);

				if (device.deviceId == null)
					throw new ConnectException("Serial is needed for " + ipAddress + " in order to list the device");
			} else
				CoolCommunication.Messenger.send(ipAddress, AppConfig.COMMUNATION_SERVER_PORT, null, handler);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return device;
	}

	public interface OnInfoAvailableListener
	{
		void onInfoAvailable(AccessDatabase database, NetworkDevice device, String ipAddress);
	}
}
