package com.genonbeta.CoolSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.net.Socket;

public abstract class CoolJsonCommunication extends CoolCommunication
{
	public static final int NO_TAB = -1;

	private int mAddTabsToResponse = NO_TAB;
	private boolean mAllowMalformedRequest = false;

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

	public boolean isMalformedRequestAllowed()
	{
		return this.mAllowMalformedRequest;
	}

	public int getAddTabsToResponse()
	{
		return this.mAddTabsToResponse;
	}

	public void setAddTabsToResponse(int line)
	{
		if (line >= NO_TAB)
			this.mAddTabsToResponse = line;
	}

	public void setAllowMalformedRequest(boolean allow)
	{
		this.mAllowMalformedRequest = allow;
	}

	@Override
	protected void onMessage(Socket socket, String message, PrintWriter writer, String clientIp)
	{
		JSONObject receivedMessage = null;

		try
		{
			receivedMessage = new JSONObject(message);
		} catch (JSONException e)
		{
			this.onError(e);
		}

		if (receivedMessage == null && !this.isMalformedRequestAllowed())
			return; // request cannot be parsed && malformed requests are not allowed

		if (receivedMessage == null)
			receivedMessage = new JSONObject();

		try
		{
			JSONObject responseJson = new JSONObject();

			this.onJsonMessage(socket, receivedMessage, responseJson, clientIp);

			writer.append((this.getAddTabsToResponse() > NO_TAB) ? responseJson.toString(this.getAddTabsToResponse()) : responseJson.toString());
			writer.flush();
		} catch (JSONException e)
		{
			this.onError(e);
		}
	}

	public abstract void onJsonMessage(Socket socket, JSONObject receivedMessage, JSONObject response, String clientIp);

	public static abstract class JsonResponseHandler extends Messenger.ResponseHandler
	{
		@Override
		public void onMessage(Socket socket, CoolCommunication.Messenger.Process process, final PrintWriter response)
		{
			JSONObject json = new JSONObject();
			process.putLater(json);

			this.onJsonMessage(socket, process, json);
		}

		public abstract void onJsonMessage(Socket socket, CoolCommunication.Messenger.Process process, JSONObject json);
	}
}
