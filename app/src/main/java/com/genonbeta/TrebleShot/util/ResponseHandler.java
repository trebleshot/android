package com.genonbeta.TrebleShot.util;

import com.genonbeta.TrebleShot.config.AppConfig;

public abstract class ResponseHandler extends CoolCommunication.Messenger.ResponseHandler
{
	@Override
	public void onConfigure(CoolCommunication.Messenger.Process process)
	{
		process.setSocketTimeout(AppConfig.DEFAULT_SOCKET_TIMEOUT);
	}
}
