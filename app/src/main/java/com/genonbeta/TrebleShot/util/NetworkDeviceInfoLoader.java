package com.genonbeta.TrebleShot.util;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.android.database.SQLQuery;

import org.json.JSONObject;

import java.net.ConnectException;
import java.net.InetSocketAddress;

public class NetworkDeviceInfoLoader
{
	public static NetworkDevice.Connection processConnection(AccessDatabase database, NetworkDevice device, String ipAddress)
	{
		NetworkDevice.Connection connection = new NetworkDevice.Connection(ipAddress);

		processConnection(database, device, connection);

		return connection;
	}

	public static void processConnection(AccessDatabase database, NetworkDevice device, NetworkDevice.Connection connection)
	{
		try {
			database.reconstruct(connection);
		} catch (Exception e) {
			connection.adapterName = Keyword.UNKNOWN_INTERFACE;
		}

		connection.lastCheckedDate = System.currentTimeMillis();
		connection.deviceId = device.deviceId;

		database.delete(new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION)
				.setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=? AND "
								+ AccessDatabase.FIELD_DEVICECONNECTION_ADAPTERNAME + " =? AND "
								+ AccessDatabase.FIELD_DEVICECONNECTION_IPADDRESS + " != ?",
						connection.deviceId, connection.adapterName, connection.ipAddress));

		database.publish(connection);
	}

	public static void load(final AccessDatabase database, final String ipAddress, OnDeviceRegisteredListener listener)
	{
		try {
			load(false, database, ipAddress, listener);
		} catch (ConnectException e) {
			e.printStackTrace();
		}
	}

	public static NetworkDevice load(boolean currentThread, final AccessDatabase database, final String ipAddress, final OnDeviceRegisteredListener listener) throws ConnectException
	{
		CoolSocket.Client.ConnectionHandler connectionHandler = new CoolSocket.Client.ConnectionHandler()
		{
			@Override
			public void onConnect(CoolSocket.Client client)
			{
				try {
					Thread.sleep(1500);

					CoolSocket.ActiveConnection activeConnection = client.connect(new InetSocketAddress(ipAddress, AppConfig.COMMUNICATION_SERVER_PORT), AppConfig.DEFAULT_SOCKET_TIMEOUT);

					activeConnection.reply(null);
					CoolSocket.ActiveConnection.Response clientResponse = activeConnection.receive();

					JSONObject jsonResponse = new JSONObject(clientResponse.response);
					JSONObject deviceInfo = jsonResponse.getJSONObject(Keyword.DEVICE_INFO);
					JSONObject appInfo = jsonResponse.getJSONObject(Keyword.APP_INFO);

					NetworkDevice device = new NetworkDevice(deviceInfo.getString(Keyword.SERIAL));

					try {
						database.reconstruct(device);
					} catch (Exception e) {
					}

					device.brand = deviceInfo.getString(Keyword.BRAND);
					device.model = deviceInfo.getString(Keyword.MODEL);
					device.nickname = deviceInfo.getString(Keyword.USER);
					device.lastUsageTime = System.currentTimeMillis();
					device.versionNumber = appInfo.getInt(Keyword.VERSION_CODE);
					device.versionName = appInfo.getString(Keyword.VERSION_NAME);

					if (device.deviceId != null) {
						NetworkDevice localDevice = AppUtils.getLocalDevice(database.getContext());
						NetworkDevice.Connection connection = processConnection(database, device, ipAddress);

						if (!localDevice.deviceId.equals(device.deviceId)) {
							device.lastUsageTime = System.currentTimeMillis();

							database.publish(device);

							if (listener != null)
								listener.onDeviceRegistered(database, device, connection);
						}
					}

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

	public interface OnDeviceRegisteredListener
	{
		void onDeviceRegistered(AccessDatabase database, NetworkDevice device, NetworkDevice.Connection connection);
	}
}
