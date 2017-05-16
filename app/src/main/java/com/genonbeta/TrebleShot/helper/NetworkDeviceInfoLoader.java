package com.genonbeta.TrebleShot.helper;

import android.content.Context;
import android.util.Log;

import com.genonbeta.CoolSocket.CoolCommunication;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.DeviceRegistry;
import com.genonbeta.TrebleShot.service.Keyword;
import com.genonbeta.android.database.SQLQuery;

import org.json.JSONObject;

import java.net.Socket;

public class NetworkDeviceInfoLoader
{
	private OnInfoAvailableListener mOnInfoAvailableListener;

	public NetworkDeviceInfoLoader(OnInfoAvailableListener listener)
	{
		setOnInfoAvailableListener(listener);
	}

	public boolean startLoading(final Context context, final DeviceRegistry deviceRegistry, final String deviceIp)
	{
		CoolCommunication.Messenger.send(deviceIp, AppConfig.COMMUNATION_SERVER_PORT, null,
				new JsonResponseHandler()
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
							NetworkDevice device = deviceRegistry.getNetworkDevice(deviceIp);

							device.brand = json.getString(Keyword.BRAND);
							device.model = json.getString(Keyword.MODEL);
							device.user = json.getString(Keyword.USER);

							if (mOnInfoAvailableListener != null)
								mOnInfoAvailableListener.onInfoAvailable(device);
						} catch (Exception e)
						{
							this.onError(e);
						}
					}
				}
		);

		return true;
	}

	public void setOnInfoAvailableListener(OnInfoAvailableListener listener)
	{
		mOnInfoAvailableListener = listener;
	}

	public static interface OnInfoAvailableListener
	{
		public void onInfoAvailable(NetworkDevice device);
	}
}
