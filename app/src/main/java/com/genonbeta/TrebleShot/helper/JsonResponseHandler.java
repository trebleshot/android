package com.genonbeta.TrebleShot.helper;

import com.genonbeta.CoolSocket.CoolCommunication;
import com.genonbeta.CoolSocket.CoolJsonCommunication;
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
    public void onResponseAvaiable(String response)
    {
    }

    @Override
    public void onFinal()
    {
    }
}
