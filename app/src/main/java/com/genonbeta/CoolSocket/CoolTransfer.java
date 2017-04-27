package com.genonbeta.CoolSocket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.NotYetBoundException;
import java.util.ArrayList;

public class CoolTransfer
{
	public final static int DELAY_DISABLED = -1;

	public abstract static class Send<T>
	{
		private final ArrayList<SendHandler> mProcess = new ArrayList<>();
		private int notifyDelay = CoolTransfer.DELAY_DISABLED;

		public abstract void onError(SendHandler handler, Exception error);

		public abstract void onNotify(SendHandler handler, int percent);

		public abstract void onTransferCompleted(SendHandler handler);

		public abstract void onInterrupted(SendHandler handler);

		public abstract void onSocketReady(SendHandler handler);

		public abstract boolean onStart(SendHandler handler);

		public boolean onCheckStatus(SendHandler handler)
		{
			return false;
		}

		public void onStop(SendHandler handler)
		{
		}

		public ArrayList<SendHandler> getProcesses()
		{
			synchronized (this.mProcess)
			{
				return this.mProcess;
			}
		}

		public SendHandler send(String serverIp, int port, File file, byte[] bufferSize, T extra)
		{
			SendHandler handler = new SendHandler(serverIp, port, file, bufferSize, extra);
			Thread thread = new Thread(handler);

			thread.start();

			return handler;
		}

		public SendHandler sendOnCurrentThread(String serverIp, int port, File file, byte[] bufferSize, T extra)
		{
			SendHandler handler = new SendHandler(serverIp, port, file, bufferSize, extra);

			handler.onRun();

			return handler;
		}

		public void setNotifyDelay(int delay)
		{
			this.notifyDelay = delay;
		}

		public class SendHandler extends TransferHandler
		{
			private String mServerIp;
			private int mPort;
			private File mFile;
			private byte[] mBufferSize;
			private T mExtra;
			private Socket mSocket;

			public SendHandler(String serverIp, int port, File file, byte[] bufferSize, T extra)
			{
				this.mServerIp = serverIp;
				this.mPort = port;
				this.mFile = file;
				this.mBufferSize = bufferSize;
				this.mExtra = extra;
			}

			@Override
			protected void onRun()
			{
				Send.this.getProcesses().add(this);

				if (!Send.this.onStart(this))
					return;

				try
				{
					mSocket = new Socket();

					mSocket.bind(null);
					mSocket.connect(new InetSocketAddress(this.mServerIp, this.mPort));

					Send.this.onSocketReady(this);

					FileInputStream inputStream = new FileInputStream(this.mFile);
					OutputStream outputStream = mSocket.getOutputStream();

					int len;
					int progressPercent = -1;
					long lastNotified = System.currentTimeMillis();

					while ((len = inputStream.read(this.mBufferSize)) > 0)
					{
						outputStream.write(this.mBufferSize, 0, len);
						outputStream.flush();

						if (Send.this.notifyDelay == -1 || (System.currentTimeMillis() - lastNotified) > Send.this.notifyDelay)
						{
							int currentPercent = (int) (((float) 100 / this.mFile.length()) * inputStream.getChannel().position());

							if (currentPercent > progressPercent)
							{
								Send.this.onNotify(this, currentPercent);
								progressPercent = currentPercent;
							}

							lastNotified = System.currentTimeMillis();
						}

						if (this.isInterrupted())
							break;
					}

					outputStream.close();
					inputStream.close();
					mSocket.close();

					if (this.isInterrupted())
						Send.this.onInterrupted(this);
					else
						Send.this.onTransferCompleted(this);
				} catch (Exception e)
				{
					Send.this.onError(this, e);
				}
				finally
				{
					Send.this.onStop(this);
					Send.this.getProcesses().remove(this);
				}
			}

			public byte[] getBufferSize()
			{
				return mBufferSize;
			}

			public T getExtra()
			{
				return mExtra;
			}

			public File getFile()
			{
				return mFile;
			}

			public int getPort()
			{
				return mPort;
			}

			public String getServerIp()
			{
				return mServerIp;
			}

			public Socket getSocket()
			{
				return mSocket;
			}

			@Override
			public boolean isInterrupted()
			{
				return super.isInterrupted() || Send.this.onCheckStatus(this);
			}
		}
	}

	public abstract static class Receive<T>
	{
		private final ArrayList<ReceiveHandler> mProcess = new ArrayList<>();
		private int notifyDelay = CoolTransfer.DELAY_DISABLED;

		public abstract void onError(ReceiveHandler handler, Exception error);

		public abstract void onNotify(ReceiveHandler handler, int percent);

		public abstract void onTransferCompleted(ReceiveHandler handler);

		public abstract void onInterrupted(ReceiveHandler handler);

		public abstract void onSocketReady(ReceiveHandler handler, ServerSocket serverSocket);

		public abstract boolean onStart(ReceiveHandler handler);

		public boolean onCheckStatus(ReceiveHandler handler)
		{
			return false;
		}

		public void onStop(ReceiveHandler handler)
		{
		}

		public ArrayList<ReceiveHandler> getProcesses()
		{
			synchronized (this.mProcess)
			{
				return this.mProcess;
			}
		}

		public ReceiveHandler receive(int port, File file, long fileSize, byte[] bufferSize, int timeOut, T extra)
		{
			ReceiveHandler handler = new ReceiveHandler(port, file, fileSize, bufferSize, timeOut, extra);
			Thread thread = new Thread(handler);

			thread.start();

			return handler;
		}

		public ReceiveHandler receiveOnCurrentThread(int port, File file, long fileSize, byte[] bufferSize, int timeOut, T extra)
		{
			ReceiveHandler handler = new ReceiveHandler(port, file, fileSize, bufferSize, timeOut, extra);

			handler.onRun();

			return handler;
		}

		public void setNotifyDelay(int delay)
		{
			this.notifyDelay = delay;
		}

		public class ReceiveHandler extends TransferHandler
		{
			private int mPort;
			private File mFile;
			private long mFileSize;
			private byte[] mBufferSize;
			private int mTimeout;
			private Socket mSocket;
			private T mExtra;

			public ReceiveHandler(int port, File file, long fileSize, byte[] bufferSize, int timeout, T extra)
			{
				this.mPort = port;
				this.mFile = file;
				this.mFileSize = fileSize;
				this.mBufferSize = bufferSize;
				this.mTimeout = timeout;
				this.mExtra = extra;
			}

			@Override
			protected void onRun()
			{
				Receive.this.getProcesses().add(this);

				if (!Receive.this.onStart(this))
					return;

				try
				{
					ServerSocket serverSocket = new ServerSocket(this.mPort);

					Receive.this.onSocketReady(this, serverSocket);

					mSocket = serverSocket.accept();

					InputStream inputStream = mSocket.getInputStream();
					FileOutputStream outputStream = new FileOutputStream(this.mFile);

					int len;
					int progressPercent = -1;
					long lastRead = System.currentTimeMillis();
					long lastNotified = System.currentTimeMillis();

					while (this.mFile.length() != this.mFileSize)
					{
						if ((len = inputStream.read(this.mBufferSize)) > 0)
						{
							outputStream.write(this.mBufferSize, 0, len);
							outputStream.flush();

							lastRead = System.currentTimeMillis();
						}

						if (Receive.this.notifyDelay == -1 || (System.currentTimeMillis() - lastNotified) > Receive.this.notifyDelay)
						{
							int currentPercent = (int) (((float) 100 / this.mFileSize) * outputStream.getChannel().position());

							if (currentPercent > progressPercent)
							{
								Receive.this.onNotify(this, currentPercent);
								progressPercent = currentPercent;
							}

							lastNotified = System.currentTimeMillis();
						}

						if ((this.mTimeout > 0 && (System.currentTimeMillis() - lastRead) > this.mTimeout) || this.isInterrupted())
							break;
					}

					outputStream.close();
					inputStream.close();
					mSocket.close();
					serverSocket.close();

					if (this.isInterrupted())
						Receive.this.onInterrupted(this);
					else
					{
						if (this.mFile.length() != this.mFileSize)
							throw new NotYetBoundException();
						else
							Receive.this.onTransferCompleted(this);
					}
				} catch (Exception e)
				{
					Receive.this.onError(this, e);
				}
				finally
				{
					Receive.this.onStop(this);
					Receive.this.getProcesses().remove(this);
				}
			}

			public byte[] getBufferSize()
			{
				return mBufferSize;
			}

			public T getExtra()
			{
				return mExtra;
			}

			public File getFile()
			{
				return mFile;
			}

			public long getFileSize()
			{
				return mFileSize;
			}

			public long getPort()
			{
				return mPort;
			}

			public Socket getSocket()
			{
				return mSocket;
			}

			public long getTimeout()
			{
				return mTimeout;
			}

			@Override
			public boolean isInterrupted()
			{
				return super.isInterrupted() || Receive.this.onCheckStatus(this);
			}
		}
	}

	public abstract static class TransferHandler implements Runnable
	{
		private boolean mInterrupted = false;

		protected abstract void onRun();

		public void interrupt()
		{
			this.mInterrupted = true;
		}

		public boolean isInterrupted()
		{
			return this.mInterrupted;
		}

		@Override
		public void run()
		{
			this.mInterrupted = false;
			this.onRun();
		}
	}
}
