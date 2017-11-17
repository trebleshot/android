package com.genonbeta.TrebleShot.util;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;

import org.json.JSONObject;

import java.net.ConnectException;
import java.net.InetSocketAddress;

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
		CoolSocket.Client.ConnectionHandler connectionHandler = new CoolSocket.Client.ConnectionHandler()
		{
			@Override
			public void onConnect(CoolSocket.Client client)
			{
				try {
					Thread.sleep(1500);

					NetworkDevice device = new NetworkDevice();

					CoolSocket.ActiveConnection activeConnection = client.connect(new InetSocketAddress(ipAddress, AppConfig.COMMUNICATION_SERVER_PORT), AppConfig.DEFAULT_SOCKET_TIMEOUT);

					activeConnection.reply(null);
					CoolSocket.ActiveConnection.Response clientResponse = activeConnection.receive();

					JSONObject jsonResponse = new JSONObject(clientResponse.response);
					JSONObject deviceInfo = jsonResponse.getJSONObject(Keyword.DEVICE_INFO);
					JSONObject appInfo = jsonResponse.getJSONObject(Keyword.APP_INFO);

					device.brand = deviceInfo.getString(Keyword.BRAND);
					device.model = deviceInfo.getString(Keyword.MODEL);
					device.user = deviceInfo.getString(Keyword.USER);
					device.deviceId = deviceInfo.getString(Keyword.SERIAL);
					device.lastUsageTime = System.currentTimeMillis();
					device.buildNumber = appInfo.getInt(Keyword.VERSION_CODE);
					device.buildName = appInfo.getString(Keyword.VERSION_NAME);

					if (mListener != null)
						mListener.onInfoAvailable(database, device, ipAddress);

					client.setReturn(device);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		if (currentThread)
			return CoolSocket.connect(connectionHandler, NetworkDevice.class);
		else
			CoolSocket.connect(connectionHandler);

		return null;
	}

	public interface OnInfoAvailableListener
	{
		void onInfoAvailable(AccessDatabase database, NetworkDevice device, String ipAddress);
	}
}
