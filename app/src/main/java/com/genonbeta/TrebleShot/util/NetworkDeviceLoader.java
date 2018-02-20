package com.genonbeta.TrebleShot.util;

import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.android.database.SQLQuery;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.ConnectException;

public class NetworkDeviceLoader
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
			AppUtils.applyAdapterName(connection);
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
		CommunicationBridge.Client.ConnectionHandler connectionHandler = new CommunicationBridge.Client.ConnectionHandler()
		{
			@Override
			public void onConnect(CommunicationBridge.Client client)
			{
				try {
					NetworkDevice device = client.loadDevice(ipAddress);

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
					if (listener != null && listener instanceof OnDeviceRegisteredErrorListener)
						((OnDeviceRegisteredErrorListener) listener).onError(e);
				}
			}
		};

		if (currentThread)
			return CommunicationBridge.connect(database, NetworkDevice.class, connectionHandler);
		else
			CommunicationBridge.connect(database, connectionHandler);

		return null;
	}

	public static NetworkDevice loadFrom(AccessDatabase database, JSONObject object) throws JSONException
	{
		JSONObject deviceInfo = object.getJSONObject(Keyword.DEVICE_INFO);
		JSONObject appInfo = object.getJSONObject(Keyword.APP_INFO);

		NetworkDevice device = new NetworkDevice(deviceInfo.getString(Keyword.DEVICE_INFO_SERIAL));

		try {
			database.reconstruct(device);
		} catch (Exception e) {
		}

		device.brand = deviceInfo.getString(Keyword.DEVICE_INFO_BRAND);
		device.model = deviceInfo.getString(Keyword.DEVICE_INFO_MODEL);
		device.nickname = deviceInfo.getString(Keyword.DEVICE_INFO_USER);
		device.lastUsageTime = System.currentTimeMillis();
		device.versionNumber = appInfo.getInt(Keyword.APP_INFO_VERSION_CODE);
		device.versionName = appInfo.getString(Keyword.APP_INFO_VERSION_NAME);

		if (device.nickname.length() > AppConfig.NICKNAME_LENGTH_MAX)
			device.nickname = device.nickname.substring(0, AppConfig.NICKNAME_LENGTH_MAX - 1);

		return device;
	}

	public interface OnDeviceRegisteredListener
	{
		void onDeviceRegistered(AccessDatabase database, NetworkDevice device, NetworkDevice.Connection connection);
	}

	public interface OnDeviceRegisteredErrorListener extends OnDeviceRegisteredListener
	{
		void onError(Exception error);
	}
}
