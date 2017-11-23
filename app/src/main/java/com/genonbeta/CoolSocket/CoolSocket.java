package com.genonbeta.CoolSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

	public static final String HEADER_SEPARATOR = "\nHEADER_END\n";
	public static final String HEADER_ITEM_LENGTH = "length";

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

	protected abstract void onConnected(ActiveConnection activeConnection);

	public static <T> T connect(final Client.ConnectionHandler handler, Class<T> clazz)
	{
		Client clientInstance = new Client();

		handler.onConnect(clientInstance);

		return clientInstance.getReturn() != null ? clazz.cast(clientInstance.getReturn()) : null;
	}

	public static void connect(final Client.ConnectionHandler handler)
	{
		final Client clientInstance = new Client();

		new Thread()
		{
			@Override
			public void run()
			{
				super.run();
				handler.onConnect(clientInstance);
			}
		}.start();
	}

	public int getConnectionCountByAddress(InetAddress address)
	{
		int returnObject = 0;

		for (ActiveConnection activeConnection : getConnections())
			if (activeConnection.getAddress().equals(address))
				returnObject++;

		return returnObject;
	}

	public synchronized ArrayList<ActiveConnection> getConnections()
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

	protected boolean respondRequest(final Socket socket)
	{
		if (this.getConnections().size() <= this.mMaxConnections || this.mMaxConnections == 0) {
			final ActiveConnection connectionHandler = new ActiveConnection(socket);

			CoolSocket.this.getConnections().add(connectionHandler);

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

					try {
						socket.getInputStream().close();
						socket.getOutputStream().close();
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						CoolSocket.this.getConnections().remove(connectionHandler);
					}
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
		private int mTimeout = NO_TIMEOUT;

		public ActiveConnection(Socket socket)
		{
			this.mSocket = socket;
		}

		public ActiveConnection(Socket socket, int timeout)
		{
			this(socket);
			setTimeout(timeout);
		}

		public InetAddress getAddress()
		{
			return this.getSocket().getInetAddress();
		}

		public String getClientAddress()
		{
			return this.getAddress().getHostAddress();
		}

		public Socket getSocket()
		{
			return this.mSocket;
		}

		public long getTimeout()
		{
			return mTimeout;
		}

		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof ActiveConnection ? ((ActiveConnection) obj).toString().equals(toString()) : super.equals(obj);
		}

		public Response receive() throws IOException, TimeoutException, JSONException
		{
			byte[] buffer = new byte[8096];
			int len;
			long calculatedTimeout = getTimeout() != NO_TIMEOUT ? System.currentTimeMillis() + getTimeout() : NO_TIMEOUT;

			InputStream inputStream = getSocket().getInputStream();
			ByteArrayOutputStream headerIndex = new ByteArrayOutputStream();
			ByteArrayOutputStream receivedMessage = new ByteArrayOutputStream();

			Response response = new Response();
			response.remoteAddress = getSocket().getRemoteSocketAddress();

			do {
				if ((len = inputStream.read(buffer)) > 0) {
					if (response.totalLength > 0) {
						receivedMessage.write(buffer, 0, len);
						receivedMessage.flush();
					} else {
						headerIndex.write(buffer, 0, len);
						headerIndex.flush();

						if (headerIndex.toString().contains(HEADER_SEPARATOR)) {
							String headerString = headerIndex.toString();
							int headerEndPoint = headerString.indexOf(HEADER_SEPARATOR);

							JSONObject headerJSON = new JSONObject(headerString.substring(0, headerEndPoint));
							response.totalLength = headerJSON.getLong(HEADER_ITEM_LENGTH);
							response.headerIndex = headerJSON;

							if (headerEndPoint < headerIndex.size())
								receivedMessage.write(headerString.substring(headerEndPoint + (HEADER_SEPARATOR.length())).getBytes());
						}
					}
				}

				if (calculatedTimeout != NO_TIMEOUT && System.currentTimeMillis() > calculatedTimeout)
					throw new TimeoutException("Read timed out!");
			}
			while (response.totalLength != receivedMessage.size() && response.totalLength != 0);

			response.response = receivedMessage.toString();

			return response;
		}

		public void reply(String out) throws TimeoutException, IOException, JSONException
		{
			byte[] outputBytes = out == null ? new byte[0] : out.getBytes();

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			PrintWriter outputWriter = new PrintWriter(outputStream);

			JSONObject headerJSON = new JSONObject()
					.put(HEADER_ITEM_LENGTH, outputBytes.length);

			outputWriter.write(headerJSON.toString() + HEADER_SEPARATOR);
			outputWriter.flush();

			byte[] buffer = new byte[8096];
			int len;
			long calculatedTimeout = getTimeout() != NO_TIMEOUT ? System.currentTimeMillis() + getTimeout() : NO_TIMEOUT;

			ByteArrayInputStream inputStream = new ByteArrayInputStream(outputBytes);
			DataOutputStream remoteOutputStream = new DataOutputStream(getSocket().getOutputStream());

			remoteOutputStream.write(outputStream.toByteArray());
			remoteOutputStream.flush();

			do {
				if ((len = inputStream.read(buffer)) > 0)
					remoteOutputStream.write(buffer, 0, len);

				if (calculatedTimeout != NO_TIMEOUT && System.currentTimeMillis() > calculatedTimeout)
					throw new TimeoutException("Read timed out!");
			}
			while (len != -1);
		}

		public void setTimeout(int timeout)
		{
			this.mTimeout = timeout;
		}

		public class Response
		{
			public SocketAddress remoteAddress;
			public JSONObject headerIndex;
			public String response;
			public long totalLength;

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

	public static class Client
	{
		private Object mReturn;

		public ActiveConnection connect(SocketAddress socketAddress, int operationTimeout) throws IOException
		{
			Socket socket = new Socket();

			if (operationTimeout != NO_TIMEOUT)
				socket.setSoTimeout(operationTimeout);

			socket.bind(null);
			socket.connect(socketAddress);

			return new ActiveConnection(socket, operationTimeout);
		}

		public interface ConnectionHandler
		{
			void onConnect(Client client);
		}

		public Object getReturn()
		{
			return mReturn;
		}

		public void setReturn(Object returnedObject)
		{
			this.mReturn = returnedObject;
		}
	}
}