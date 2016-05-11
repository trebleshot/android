package com.genonbeta.CoolSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.net.Socket;

public abstract class CoolJsonCommunication extends CoolCommunication
{
	public CoolJsonCommunication()
	{
	}

	public CoolJsonCommunication(int port)
	{
		super(port);
	}

	public CoolJsonCommunication(String address, int port)
	{
		super(address, port);
	}
	
	@Override
	protected final void onMessage(Socket socket, String message, PrintWriter writer, String clientIp)
	{
		try
		{
			JSONObject receivedMessage = new JSONObject(message);
			JSONObject responseJson = new JSONObject();
			
			this.onJsonMessage(socket, receivedMessage, responseJson, clientIp);

			writer.append(responseJson.toString());
			writer.flush();
		}
		catch (JSONException e)
		{
			this.onError(e);
		}
	}
	
	public abstract void onJsonMessage(Socket socket, JSONObject receivedMessage, JSONObject response, String clientIp);
	
	public static abstract class JsonResponseHandler extends Messenger.ResponseHandler
	{
		@Override
		public final void onMessage(Socket socket, CoolCommunication.Messenger.Process process, final PrintWriter response)
		{
			JSONObject json = new JSONObject();
			process.putLater(json);
			
			this.onJsonMessage(socket, process, json);
		}
		
		public abstract void onJsonMessage(Socket socket, CoolCommunication.Messenger.Process process, JSONObject json);
	}
}
