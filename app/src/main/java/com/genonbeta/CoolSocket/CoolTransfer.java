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
import java.util.concurrent.ArrayBlockingQueue;

public class CoolTransfer
{
	public final static int DELAY_DISABLED = -1;

	public abstract static class Send<T extends Object>
	{
		private ArrayBlockingQueue<SendHandler> mProcess = new ArrayBlockingQueue<SendHandler>(100, true);
		private int notifyDelay = CoolTransfer.DELAY_DISABLED;

		public abstract void onError(String serverIp, int port, File file, Exception error, T extra);

		public abstract void onNotify(Socket socket, String serverIp, int port, File file, int percent, T extra);

		public abstract void onTransferCompleted(String serverIp, int port, File file, T extra);

		public abstract void onSocketReady(Socket socket, String serverIp, int port, File file, T extra);

		public abstract boolean onStart(String serverIp, int port, File file, T extra);

		public boolean onBreakRequest(String serverIp, int port, File file, T extra)
		{
			return false;
		}

		public void onStop(String serverIp, int port, File file, T extra)
		{
		}

		public ArrayBlockingQueue<SendHandler> getProcesses()
		{
			return this.mProcess;
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
			handler.transferCompleted();

			return handler;
		}

		public void setNotifyDelay(int delay)
		{
			this.notifyDelay = delay;
		}

		public class SendHandler extends TransferHandler
		{
			protected String mServerIp;
			protected int mPort;
			protected File mFile;
			protected byte[] mBufferSize;
			protected T mExtra;

			public SendHandler(String serverIp, int port, File file, byte[] bufferSize, T extra)
			{
				this.mServerIp = serverIp;
				this.mPort = port;
				this.mFile = file;
				this.mBufferSize = bufferSize;
				this.mExtra = extra;
			}

			@Override
			public boolean isBreakRequested()
			{
				return super.isBreakRequested() || Send.this.onBreakRequest(this.mServerIp, this.mPort, this.mFile, this.mExtra);
			}

			@Override
			protected void onRun()
			{
				Send.this.getProcesses().offer(this);

				if (!Send.this.onStart(this.mServerIp, this.mPort, this.mFile, this.mExtra))
					return;

				try
				{
					Socket socket = new Socket();

					socket.bind(null);
					socket.connect(new InetSocketAddress(this.mServerIp, this.mPort));

					Send.this.onSocketReady(socket, this.mServerIp, this.mPort, this.mFile, this.mExtra);

					FileInputStream inputStream = new FileInputStream(this.mFile);
					OutputStream outputStream = socket.getOutputStream();

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
								Send.this.onNotify(socket, this.mServerIp, this.mPort, this.mFile, currentPercent, this.mExtra);
								progressPercent = currentPercent;
							}

							lastNotified = System.currentTimeMillis();
						}

						if (this.isBreakRequested())
							break;
					}

					outputStream.close();
					inputStream.close();
					socket.close();

					Send.this.onTransferCompleted(this.mServerIp, this.mPort, this.mFile, this.mExtra);
				} catch (Exception e)
				{
					Send.this.onError(this.mServerIp, this.mPort, this.mFile, e, this.mExtra);
				}
				finally
				{
					Send.this.onStop(this.mServerIp, this.mPort, this.mFile, this.mExtra);
					Send.this.getProcesses().remove(this);
				}
			}
		}
	}

	public abstract static class Receive<T extends Object>
	{
		private ArrayBlockingQueue<ReceiveHandler> mProcess = new ArrayBlockingQueue<ReceiveHandler>(100, true);
		private int notifyDelay = CoolTransfer.DELAY_DISABLED;

		public abstract void onError(int port, File file, Exception error, T extra);

		public abstract void onNotify(Socket socket, int port, File file, int percent, T extra);

		public abstract void onTransferCompleted(int port, File file, T extra);

		public abstract void onSocketReady(ServerSocket socket, int port, File file, T extra);

		public abstract boolean onStart(int port, File file, T extra);

		public boolean onBreakRequest(int port, File file, T extra)
		{
			return false;
		}

		public void onStop(int port, File file, T extra)
		{
		}

		public ArrayBlockingQueue<ReceiveHandler> getProcesses()
		{
			return this.mProcess;
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
			handler.transferCompleted();

			return handler;
		}

		public void setNotifyDelay(int delay)
		{
			this.notifyDelay = delay;
		}

		public class ReceiveHandler extends TransferHandler
		{
			protected int mPort;
			protected File mFile;
			protected long mFileSize;
			protected byte[] mBufferSize;
			protected int mTimeout;
			protected T mExtra;

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
			public boolean isBreakRequested()
			{
				return super.isBreakRequested() || Receive.this.onBreakRequest(this.mPort, this.mFile, this.mExtra);
			}

			@Override
			protected void onRun()
			{
				Receive.this.getProcesses().offer(this);

				if (!Receive.this.onStart(this.mPort, this.mFile, this.mExtra))
					return;

				try
				{
					ServerSocket serverSocket = new ServerSocket(this.mPort);

					Receive.this.onSocketReady(serverSocket, this.mPort, this.mFile, this.mExtra);

					Socket socket = serverSocket.accept();
					InputStream inputStream = socket.getInputStream();
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
								Receive.this.onNotify(socket, this.mPort, this.mFile, currentPercent, this.mExtra);
								progressPercent = currentPercent;
							}

							lastNotified = System.currentTimeMillis();
						}

						if ((this.mTimeout > 0 && (System.currentTimeMillis() - lastRead) > this.mTimeout) || this.isBreakRequested())
							break;
					}

					outputStream.close();
					inputStream.close();
					socket.close();
					serverSocket.close();

					if (this.mFile.length() != this.mFileSize && !this.isBreakRequested())
						Receive.this.onError(this.mPort, this.mFile, new NotYetBoundException(), this.mExtra);

					Receive.this.onTransferCompleted(this.mPort, this.mFile, this.mExtra);
				} catch (Exception e)
				{
					Receive.this.onError(this.mPort, this.mFile, e, this.mExtra);
				}
				finally
				{
					Receive.this.onStop(this.mPort, this.mFile, this.mExtra);
					Receive.this.getProcesses().remove(this);
				}
			}
		}
	}

	public abstract static class TransferHandler implements Runnable
	{
		private boolean mIsBreakRequested = false;
		private boolean mIsCompleted = false;

		protected abstract void onRun();

		public boolean isBreakRequested()
		{
			return this.mIsBreakRequested;
		}

		public boolean isCompleted()
		{
			return this.mIsCompleted;
		}

		public void requestBreak()
		{
			this.mIsBreakRequested = true;
		}

		@Override
		public void run()
		{
			this.onRun();
			this.transferCompleted();
		}

		public void transferCompleted()
		{
			this.mIsCompleted = true;
		}
	}
}
