package com.genonbeta.TrebleShot.util;

import com.genonbeta.TrebleShot.config.AppConfig;

import org.json.JSONObject;

import java.net.Socket;

public abstract class JsonResponseHandler extends CoolJsonCommunication.JsonResponseHandler
{
	@Override
	public void onConfigure(CoolCommunication.Messenger.Process process)
	{
		process.setSocketTimeout(AppConfig.DEFAULT_SOCKET_TIMEOUT);
	}

	@Override
	public void onJsonMessage(Socket socket, CoolCommunication.Messenger.Process process, JSONObject json)
	{
	}

	@Override
	public void onError(Exception exception)
	{
	}

	@Override
	public void onResponseAvailable(String response)
	{
	}

	@Override
	public void onFinal()
	{
	}
}
