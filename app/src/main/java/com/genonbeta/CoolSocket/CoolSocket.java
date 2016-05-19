package com.genonbeta.CoolSocket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
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

abstract public class CoolSocket
{
	public static final String END_SEQUENCE = "\n();;";
	public static final int NO_TIMEOUT = -1;
	
	private Thread mServerThread;
	private ServerSocket mServerSocket;
	private SocketAddress mSocketAddress = null;
	private int mSocketTimeout = NO_TIMEOUT; // no timeout
	private int mMaxConnections = 0; // no limit
	private SocketRunnable mSocketRunnable = new SocketRunnable();
	private ArrayList<ClientHandler> mConnections = new ArrayList<ClientHandler>();
	
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
	
	protected void onClosingConnection(ClientHandler client)
	{}
	
	abstract protected void onError(Exception exception);
	abstract protected void onPacketReceived(Socket socket);
	
	public ArrayList<ClientHandler> getConnections()
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
	
	public SocketAddress getSocketAddress()
	{
		return this.mSocketAddress;
	}
	
	protected SocketRunnable getSocketRunnable()
	{
		return this.mSocketRunnable;
	}
	
	protected Thread getServerThread()
	{
		return this.mServerThread;
	}
	
	public static PrintWriter getStreamWriter(OutputStream outputStream)
	{
		return new PrintWriter(new BufferedOutputStream(outputStream));
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
	
	public static ByteArrayOutputStream readStream(InputStream inputStreamIns) throws IOException
	{
		BufferedInputStream inputStream = new BufferedInputStream(inputStreamIns);
		ByteArrayOutputStream inputStreamResult = new ByteArrayOutputStream();

		byte[] buffer = new byte[8096];
		int len = 0;
		
		do
		{
			if ((len = inputStream.read(buffer)) > 0)
				inputStreamResult.write(buffer, 0, len);
		}
		while (!inputStreamResult.toString().endsWith(END_SEQUENCE));
		
		return inputStreamResult;
	}

	public static String readStreamMessage(InputStream inputStream) throws IOException
	{
		return readStreamMessage(readStream(inputStream));
	}

	public static String readStreamMessage(ByteArrayOutputStream outputStream)
	{
		String message = outputStream.toString();

		return message.substring(0, message.length() - END_SEQUENCE.length());
	}
	
	protected boolean respondRequest(Socket socket)
	{
		if (this.getConnections().size() < this.mMaxConnections || this.mMaxConnections == 0)
		{	
			ClientHandler clientRunnable = new ClientHandler(socket);
			Thread selfThread = new Thread(clientRunnable);

			this.getConnections().add(clientRunnable);

			selfThread.start();
		}
		else
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
		if (this.getServerSocket() == null || this.getServerSocket().isClosed())
		{
			try
			{
				this.mServerSocket = new ServerSocket();
				this.getServerSocket().bind(this.mSocketAddress);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return false;
			}
		}
		
		if (this.getServerThread() == null || Thread.State.TERMINATED.equals(this.getServerThread().getState()))
		{
			this.mServerThread = new Thread(this.getSocketRunnable());

			this.getServerThread().setDaemon(true);
			this.getServerThread().setName("CoolSocket Main Thread");
		}
		else if (this.getServerThread().isAlive())
			return false;
		
		this.getServerThread().start();	

		return true;
	}
	
	public boolean startDelayed(int timeout)
	{
		long startTime = System.currentTimeMillis();
		
		while (this.isServerAlive() && (System.currentTimeMillis() - startTime) < timeout)
		{}
		
		return this.start();
	}
	
	public boolean stop()
	{
		if (this.isInterrupted())
			return false;
			
		this.getServerThread().interrupt();
		
		if (!this.getServerSocket().isClosed())
		{
			try
			{
				this.getServerSocket().close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		return true;
	}
	
	protected class ClientHandler implements Runnable
	{
		private Socket mSocket;
		
		public ClientHandler(Socket socket)
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
		
		@Override
		public void run()
		{
			try
			{
				if (CoolSocket.this.mSocketTimeout > NO_TIMEOUT)
					this.mSocket.setSoTimeout(CoolSocket.this.mSocketTimeout);
			}
			catch (SocketException e)
			{
				e.printStackTrace();
			}
			
			CoolSocket.this.onPacketReceived(this.mSocket);
			
			CoolSocket.this.onClosingConnection(this);
			CoolSocket.this.getConnections().remove(this);
		}
	}
	
	private class SocketRunnable implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				do
				{
					Socket request = CoolSocket.this.getServerSocket().accept();
					
					if (CoolSocket.this.isInterrupted())
						request.close();
					else
						respondRequest(request);
				}
				while (!CoolSocket.this.isInterrupted());
			}
			catch (IOException e)
			{
				CoolSocket.this.onError(e);
			}
		}
	}
}
