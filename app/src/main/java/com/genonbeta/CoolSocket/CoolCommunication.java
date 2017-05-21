package com.genonbeta.CoolSocket;

import android.support.v7.app.WindowDecorActionBar;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeoutException;

public abstract class CoolCommunication extends CoolSocket
{
	public CoolCommunication()
	{
	}

	public CoolCommunication(int port)
	{
		super(port);
	}

	public CoolCommunication(String address, int port)
	{
		super(address, port);
	}

	@Override
	protected void onPacketReceived(Socket socket)
	{
		try
		{
			PrintWriter writer = getStreamWriter(socket.getOutputStream());
			String message = readStreamMessage(socket.getInputStream(), getSocketTimeout());

			this.onMessage(socket, message, writer, socket.getInetAddress().isAnyLocalAddress() ? "127.0.0.1" : socket.getInetAddress().getHostAddress());

			writer.append(CoolSocket.END_SEQUENCE);
			writer.flush();

			socket.close();
		} catch (IOException e)
		{
			this.onError(e);
		} catch (TimeoutException e)
		{
			this.onError(e);
		}
	}

	abstract protected void onMessage(Socket socket, String message, PrintWriter writer, String clientIp);

	public static class Messenger
	{
		public static final int NO_TIMEOUT = -1;

		public static void send(String socketHost, int socketPort, String message, ResponseHandler handler)
		{
			send(new InetSocketAddress(socketHost, socketPort), message, handler);
		}

		public static void send(InetSocketAddress address, String message, ResponseHandler handler)
		{
			SenderRunnable runnable = new SenderRunnable(address, message, handler);
			new Thread(runnable).start();
		}

		public static String sendOnCurrentThread(String socketHost, int socketPort, String message, ResponseHandler handler)
		{
			return sendOnCurrentThread(new InetSocketAddress(socketHost, socketPort), message, handler);
		}

		public static String sendOnCurrentThread(InetSocketAddress address, String message, ResponseHandler handler)
		{
			SenderRunnable runnable = new SenderRunnable(address, message, handler);

			return runnable.runProcess();
		}

		private static class SenderRunnable implements Runnable
		{
			private Process mProcess;

			public SenderRunnable(SocketAddress address, String message, ResponseHandler handler)
			{
				this.mProcess = new Process(address, message, handler);
			}

			@Override
			public void run()
			{
				runProcess();
			}

			public String runProcess()
			{
				if (this.mProcess.getResponseHandler() != null)
					this.mProcess.getResponseHandler().onConfigure(this.mProcess);

				Socket socket = new Socket();

				try
				{
					socket.bind(null);
					socket.connect(this.mProcess.getSocketAddress());

					if (this.mProcess.getSocketTimeout() != NO_TIMEOUT)
						socket.setSoTimeout(this.mProcess.getSocketTimeout());

					PrintWriter writer = getStreamWriter(socket.getOutputStream());

					this.mProcess.setSocket(socket);
					this.mProcess.setWriter(writer);

					if (this.mProcess.getMessage() != null)
						writer.append(this.mProcess.getMessage());

					if (this.mProcess.getResponseHandler() != null)
						this.mProcess.getResponseHandler().onMessage(socket, mProcess, writer);

					this.mProcess.waitForResponse();

					if (this.mProcess.getResponseHandler() != null)
						this.mProcess.getResponseHandler().onResponseAvailable(this.mProcess.getResponse());

					return this.mProcess.getResponse();
				} catch (Exception e)
				{
					if (this.mProcess.getResponseHandler() != null)
						this.mProcess.getResponseHandler().onError(e);
				}
				finally
				{
					if (this.mProcess.getResponseHandler() != null)
						this.mProcess.getResponseHandler().onFinal();
				}

				return null;
			}
		}

		public static class Process
		{
			private SocketAddress mAddress;
			private ResponseHandler mHandler;
			private String mMessage;
			private String mResponse;
			private PrintWriter mWriter;
			private Socket mSocket;
			private Object mPutLater;

			private boolean mResponseReceived = false;
			private boolean mIsFlushRequested = false;
			private int mSocketTimeout = Messenger.NO_TIMEOUT;

			public Process(SocketAddress address, String message, ResponseHandler handler)
			{
				this.mAddress = address;
				this.mMessage = message;
				this.mHandler = handler;
			}

			public String getMessage()
			{
				return this.mMessage;
			}

			public Object getPutLater()
			{
				return this.mPutLater;
			}

			public PrintWriter getPrintWriter()
			{
				return this.mWriter;
			}

			public String getResponse()
			{
				return this.mResponse;
			}

			public ResponseHandler getResponseHandler()
			{
				return this.mHandler;
			}

			public Socket getSocket()
			{
				return this.mSocket;
			}

			public void setSocket(Socket socket)
			{
				this.mSocket = socket;
			}

			public SocketAddress getSocketAddress()
			{
				return this.mAddress;
			}

			public int getSocketTimeout()
			{
				return this.mSocketTimeout;
			}

			public void setSocketTimeout(int timeout)
			{
				this.mSocketTimeout = timeout;
			}

			public boolean isResponseReceived()
			{
				return this.mResponseReceived;
			}

			public void setResponseReceived(String response)
			{
				this.mResponse = response;
				this.mResponseReceived = true;
			}

			public boolean isFlushRequested()
			{
				return this.mIsFlushRequested;
			}

			public void putLater(Object object)
			{
				this.mPutLater = object;
			}

			public boolean requestFlush()
			{
				if (!this.isFlushRequested() && this.getPrintWriter() != null)
				{
					this.mIsFlushRequested = true;

					if (this.getPutLater() != null)
					{
						this.getPrintWriter().append(this.getPutLater().toString());
						this.mPutLater = null;
					}

					this.getPrintWriter().append(CoolSocket.END_SEQUENCE);
					this.getPrintWriter().flush();

					try
					{
						this.setResponseReceived(readStreamMessage(this.getSocket().getInputStream(), getSocketTimeout()));
					} catch (IOException e)
					{
						return false;
					} catch (TimeoutException e)
					{
						return false;
					}

					return true;
				}

				return false;
			}

			public void setWriter(PrintWriter writer)
			{
				this.mWriter = writer;
			}

			public String waitForResponse() throws TimeoutException
			{
				long timeStart = System.currentTimeMillis();

				if (this.requestFlush())
				{
					while (!this.isResponseReceived() && (this.getSocketTimeout() == Messenger.NO_TIMEOUT || (System.currentTimeMillis() - timeStart) < this.getSocketTimeout()))
					{
					}

					if (!this.isResponseReceived())
						throw new TimeoutException();
				}

				return this.getResponse();
			}
		}

		public static class ResponseHandler
		{
			public void onConfigure(Process process)
			{
			}

			public void onMessage(Socket socket, Process process, PrintWriter response)
			{
			}

			public void onResponseAvailable(String response)
			{
			}

			public void onError(Exception exception)
			{
			}

			public void onFinal()
			{
			}
		}
	}
}
