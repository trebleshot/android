package com.genonbeta.CoolSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

abstract public class CoolSocket
{
	public static final int NO_TIMEOUT = -1;

	private Thread mServerThread;
	private ServerSocket mServerSocket;
	private SocketAddress mSocketAddress = null;
	private int mSocketTimeout = NO_TIMEOUT; // no timeout
	private int mMaxConnections = 0; // no limit
	private ServerRunnable mSocketRunnable = new ServerRunnable();
	private ArrayList<CoolSocket.ActiveConnection> mConnections = new ArrayList<>();

	public CoolSocket()
	{
	}

	public CoolSocket(int port)
	{
		this.mSocketAddress = new InetSocketAddress(port);
	}

	public CoolSocket(String address, int port)
	{
		this.mSocketAddress = new InetSocketAddress(address, port);
	}

	abstract void onConnected(ActiveConnection activeConnection);

	public ArrayList<ActiveConnection> getConnections()
	{
		return this.mConnections;
	}

	public int getLocalPort()
	{
		return this.getServerSocket().getLocalPort();
	}

	protected ServerSocket getServerSocket()
	{
		return this.mServerSocket;
	}

	protected Thread getServerThread()
	{
		return this.mServerThread;
	}

	public SocketAddress getSocketAddress()
	{
		return this.mSocketAddress;
	}

	protected ServerRunnable getSocketRunnable()
	{
		return this.mSocketRunnable;
	}

	public int getSocketTimeout()
	{
		return this.mSocketTimeout;
	}

	public boolean isComponentsReady()
	{
		return this.getServerSocket() != null && this.getServerThread() != null && this.getSocketAddress() != null;
	}

	public boolean isInterrupted()
	{
		return this.getServerThread().isInterrupted();
	}

	public boolean isServerAlive()
	{
		return this.getServerThread().isAlive();
	}

	protected boolean respondRequest(Socket socket)
	{
		if (this.getConnections().size() < this.mMaxConnections || this.mMaxConnections == 0) {
			final ActiveConnection connectionHandler = new ActiveConnection(socket);

			this.getConnections().add(connectionHandler);

			new Thread()
			{
				@Override
				public void run()
				{
					try {
						if (CoolSocket.this.mSocketTimeout > NO_TIMEOUT)
							connectionHandler.getSocket().setSoTimeout(CoolSocket.this.mSocketTimeout);
					} catch (SocketException e) {
						e.printStackTrace();
					}

					onConnected(connectionHandler);

					CoolSocket.this.getConnections().remove(this);
				}
			}.start();
		} else
			return false;

		return true;
	}

	public void setMaxConnections(int value)
	{
		this.mMaxConnections = value;
	}

	public void setSocketAddress(SocketAddress address)
	{
		this.mSocketAddress = address;
	}

	public void setSocketTimeout(int timeout)
	{
		this.mSocketTimeout = timeout;
	}

	public boolean start()
	{
		if (this.getServerSocket() == null || this.getServerSocket().isClosed()) {
			try {
				this.mServerSocket = new ServerSocket();
				this.getServerSocket().bind(this.mSocketAddress);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		if (this.getServerThread() == null || Thread.State.TERMINATED.equals(this.getServerThread().getState())) {
			this.mServerThread = new Thread(this.getSocketRunnable());

			this.getServerThread().setDaemon(true);
			this.getServerThread().setName("CoolSocket Main Thread");
		} else if (this.getServerThread().isAlive())
			return false;

		this.getServerThread().start();

		return true;
	}

	public boolean startDelayed(int timeout)
	{
		long startTime = System.currentTimeMillis();

		while (this.isServerAlive() && (System.currentTimeMillis() - startTime) < timeout) {
		}

		return this.start();
	}

	public boolean stop()
	{
		if (this.isInterrupted())
			return false;

		this.getServerThread().interrupt();

		if (!this.getServerSocket().isClosed()) {
			try {
				this.getServerSocket().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	public void onInternalError(Exception exception)
	{
	}

	public static class ActiveConnection
	{
		private Socket mSocket;
		private long mTimeout = NO_TIMEOUT;

		public ActiveConnection(Socket socket)
		{
			this.mSocket = socket;
		}

		public InetAddress getAddress()
		{
			return this.getSocket().getInetAddress();
		}

		public Socket getSocket()
		{
			return this.mSocket;
		}

		public long getTimeout()
		{
			return mTimeout;
		}

		protected Response receive(String message) throws IOException, TimeoutException, JSONException
		{
			byte[] buffer = new byte[8096];
			int len;
			long calculatedTimeout = getTimeout() != NO_TIMEOUT ? System.currentTimeMillis() + getTimeout() : NO_TIMEOUT;

			DataInputStream inputStream = new DataInputStream(getSocket().getInputStream());
			ByteArrayOutputStream headerIndex = new ByteArrayOutputStream();
			ByteArrayOutputStream receivedMessage = new ByteArrayOutputStream();

			inputStream.readFully(buffer);
			headerIndex.write(buffer);

			JSONObject headerJSON = new JSONObject(headerIndex.toString());
			long totalLength = headerJSON.getLong("length");

			do {
				if ((len = inputStream.read(buffer)) > 0)
					receivedMessage.write(buffer, 0, len);

				if (calculatedTimeout != NO_TIMEOUT && System.currentTimeMillis() > calculatedTimeout)
					throw new TimeoutException("Read timed out!");
			}
			while (totalLength != receivedMessage.size());

			Response response = new Response();

			response.remoteAddress = getSocket().getRemoteSocketAddress();
			response.headerIndex = headerJSON;
			response.response = receivedMessage.toString();

			return response;
		}

		protected void reply(String string) throws TimeoutException, IOException, JSONException
		{
			byte[] buffer = new byte[8096];
			int len;
			long calculatedTimeout = getTimeout() != NO_TIMEOUT ? System.currentTimeMillis() + getTimeout() : NO_TIMEOUT;

			DataInputStream inputStream = new DataInputStream(getSocket().getInputStream());

			inputStream.readFully(buffer);

			ByteArrayOutputStream headerReadable = new ByteArrayOutputStream();
			DataOutputStream headerWriter = new DataOutputStream(headerReadable);
			headerReadable.write(buffer);

			JSONObject headerJSON = new JSONObject(headerReadable.toString());
			long totalLength = headerJSON.getLong("length");

			ByteArrayOutputStream receivedMessage = new ByteArrayOutputStream();

			do {
				if ((len = inputStream.read(buffer)) > 0)
					receivedMessage.write(buffer, 0, len);

				if (calculatedTimeout != NO_TIMEOUT && System.currentTimeMillis() > calculatedTimeout)
					throw new TimeoutException("Read timed out!");
			}
			while (totalLength != receivedMessage.size());
		}

		public class Response
		{
			public SocketAddress remoteAddress;
			public JSONObject headerIndex;
			public String response;

			public Response()
			{
			}
		}
	}

	private class ServerRunnable implements Runnable
	{
		@Override
		public void run()
		{
			try {
				do {
					Socket request = CoolSocket.this.getServerSocket().accept();

					if (CoolSocket.this.isInterrupted())
						request.close();
					else
						respondRequest(request);
				}
				while (!CoolSocket.this.isInterrupted());
			} catch (IOException e) {
				CoolSocket.this.onInternalError(e);
			}
		}
	}

	abstract public static class Connect
	{
		private String returnedMessage;

		public void Connect()
		{
		}

		public ActiveConnection connect(SocketAddress socketAddress) throws IOException
		{
			Socket socket = new Socket();

			socket.bind(null);
			socket.connect(socketAddress);

			return new ActiveConnection(socket);
		}

		public void doConnect(boolean currentThread, final ConnectionHandler handler)
		{
			if (currentThread) {
				handler.onConnect(this);
			} else
				new Thread()
				{
					@Override
					public void run()
					{
						super.run();
						handler.onConnect(Connect.this);
					}
				}.start();
		}

		interface ConnectionHandler
		{
			void onConnect(Connect connect);
		}
	}

	public static PrintWriter getStreamWriter(OutputStream outputStream)
	{
		return new PrintWriter(new BufferedOutputStream(outputStream));
	}
}
