package com.genonbeta.TrebleShot.helper;

import com.genonbeta.CoolSocket.CoolCommunication;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.DeviceRegistry;
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

	public void startLoading(final DeviceRegistry deviceRegistry, final String deviceIp)
	{
		try
		{
			startLoading(false, deviceRegistry, deviceIp);
		} catch (ConnectException e)
		{
			e.printStackTrace();
		}
	}

	public NetworkDevice startLoading(boolean currentThread, final DeviceRegistry deviceRegistry, final String deviceIp) throws ConnectException
	{
		final NetworkDevice device = deviceRegistry.getNetworkDevice(deviceIp);
		final JsonResponseHandler handler = new JsonResponseHandler()
		{
			@Override
			public void onConfigure(CoolCommunication.Messenger.Process process)
			{
				super.onConfigure(process);

				try
				{
					Thread.sleep(1500);
				} catch (InterruptedException e)
				{
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
				try
				{
					JSONObject json = new JSONObject(response).getJSONObject(Keyword.DEVICE_INFO);

					device.brand = json.getString(Keyword.BRAND);
					device.model = json.getString(Keyword.MODEL);
					device.user = json.getString(Keyword.USER);
					device.deviceId = json.getString(Keyword.SERIAL);
					device.lastUsageTime = System.currentTimeMillis();

					if (mListener != null)
						mListener.onInfoAvailable(device);
				} catch (Exception e)
				{
					onError(e);
				}
			}
		};

		if (currentThread)
		{
			CoolCommunication.Messenger.sendOnCurrentThread(deviceIp, AppConfig.COMMUNATION_SERVER_PORT, null, handler);

			if (device.deviceId == null)
				throw new ConnectException("Serial is needed for " + deviceIp + " in order to list the device");
		}
		else
			CoolCommunication.Messenger.send(deviceIp, AppConfig.COMMUNATION_SERVER_PORT, null, handler);

		return device;
	}

	public static interface OnInfoAvailableListener
	{
		public void onInfoAvailable(NetworkDevice device);
	}
}
