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

abstract public class CoolTransfer<T>
{
	public final static int DELAY_DISABLED = -1;

	public enum Flag
	{
		CONTINUE,
		CANCEL_ALL,
		CANCEL_CURRENT
	}

	public enum Status
	{
		INTERRUPTED,
		RUNNING,
		PENDING,
	}

	private final ArrayList<TransferHandler<T>> mProcess = new ArrayList<>();
	private int notifyDelay = CoolTransfer.DELAY_DISABLED;

	public abstract Flag onError(TransferHandler<T> handler, Exception error);

	public abstract void onNotify(TransferHandler<T> handler, int percent);

	public abstract void onTransferCompleted(TransferHandler<T> handler);

	public abstract void onInterrupted(TransferHandler<T> handler);

	public abstract Flag onSocketReady(TransferHandler<T> handler);

	public abstract Flag onStart(TransferHandler<T> handler);

	public void onPrepareNext(TransferHandler<T> handler)
	{
	}

	public void onStop(TransferHandler<T> handler)
	{
	}

	public void onProcessListChanged(ArrayList<TransferHandler<T>> processList, TransferHandler<T> handler, boolean isAdded)
	{
	}

	protected void addProcess(TransferHandler<T> processHandler)
	{
		getProcessList().add(processHandler);
		onProcessListChanged(getProcessList(), processHandler, true);
	}

	public ArrayList<TransferHandler<T>> getProcessList()
	{
		synchronized (this.mProcess)
		{
			return this.mProcess;
		}
	}

	public int getNotifyDelay()
	{
		return notifyDelay;
	}

	protected void removeProcess(TransferHandler<T> processHandler)
	{
		getProcessList().remove(processHandler);
		onProcessListChanged(getProcessList(), processHandler, false);
	}

	public void setNotifyDelay(int delay)
	{
		this.notifyDelay = delay;
	}

	public abstract static class TransferHandler<T> implements Runnable
	{
		private boolean mInterrupted = false;
		private Socket mSocket;
		private T mExtra;
		private byte[] mBufferSize;
		private int mPort;
		private File mFile;
		private Status mStatus = Status.PENDING;

		public TransferHandler(int port, File file, byte[] bufferSize, T extra)
		{
			mExtra = extra;
			this.mPort = port;
			this.mFile = file;
			this.mBufferSize = bufferSize;
		}

		protected abstract void onRun();

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

		public Socket getSocket()
		{
			return mSocket;
		}

		public Status getStatus()
		{
			return mStatus;
		}

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

			this.setStatus(Status.RUNNING);
			this.onRun();
			this.setStatus(Status.INTERRUPTED);
		}

		protected void setSocket(Socket mSocket)
		{
			this.mSocket = mSocket;
		}

		public void setStatus(Status status)
		{
			this.mStatus = status;
		}
	}

	public static abstract class Receive<T> extends CoolTransfer<T>
	{
		public abstract Flag onSocketReady(Handler handler, ServerSocket serverSocket);

		public void onOrientatingStreams(Handler handler, InputStream inputStream, FileOutputStream fileOutputStream)
		{

		}

		public Handler receive(int port, File file, long fileSize, byte[] bufferSize, int timeOut, T extra)
		{
			Handler handler = new Handler(extra, port, file, fileSize, bufferSize, timeOut);
			Thread thread = new Thread(handler);

			thread.start();

			return handler;
		}

		public Handler receiveOnCurrentThread(int port, File file, long fileSize, byte[] bufferSize, int timeOut, T extra)
		{
			Handler handler = new Handler(extra, port, file, fileSize, bufferSize, timeOut);

			handler.run();

			return handler;
		}

		public class Handler extends CoolTransfer.TransferHandler<T>
		{
			private long mFileSize;
			private int mTimeout;

			public Handler(T extra, int port, File file, long fileSize, byte[] bufferSize, int timeout)
			{
				super(port, file, bufferSize, extra);

				this.mFileSize = fileSize;
				this.mTimeout = timeout;
			}

			@Override
			protected void onRun()
			{
				addProcess(this);

				Flag flag = onStart(this);

				try
				{
					if (Flag.CONTINUE.equals(flag))
					{
						ServerSocket serverSocket = new ServerSocket(getPort());

						flag = onSocketReady(this, serverSocket);

						if (Flag.CONTINUE.equals(flag))
						{
							setSocket(serverSocket.accept());

							flag = onSocketReady(this);

							if (Flag.CONTINUE.equals(flag))
							{
								InputStream inputStream = getSocket().getInputStream();
								FileOutputStream outputStream = new FileOutputStream(getFile(), getFile().length() > 0);

								onOrientatingStreams(this, inputStream, outputStream);

								int len;
								int progressPercent = -1;
								long lastRead = System.currentTimeMillis();
								long lastNotified = System.currentTimeMillis();

								while (getFile().length() != this.mFileSize)
								{
									if ((len = inputStream.read(getBufferSize())) > 0)
									{
										outputStream.write(getBufferSize(), 0, len);
										outputStream.flush();

										lastRead = System.currentTimeMillis();
									}

									if (getNotifyDelay() == -1 || (System.currentTimeMillis() - lastNotified) > getNotifyDelay())
									{
										int currentPercent = (int) (((float) 100 / this.mFileSize) * outputStream.getChannel().position());

										if (currentPercent > progressPercent)
										{
											onNotify(this, currentPercent);
											progressPercent = currentPercent;
										}

										lastNotified = System.currentTimeMillis();
									}

									if ((this.mTimeout > 0 && (System.currentTimeMillis() - lastRead) > this.mTimeout) || this.isInterrupted())
										break;
								}

								outputStream.close();
								inputStream.close();
							}

							getSocket().close();
						}

						serverSocket.close();

						if (this.isInterrupted())
						{
							flag = Flag.CANCEL_ALL;
							onInterrupted(this);
						}
						else
						{
							if (getFile().length() != this.mFileSize)
								throw new NotYetBoundException();
							else
								onTransferCompleted(this);
						}
					}
				} catch (Exception e)
				{
					flag = onError(this, e);
				}
				finally
				{
					onStop(this);
					removeProcess(this);
				}

				if (!Flag.CANCEL_ALL.equals(flag))
					onPrepareNext(this);
			}

			public long getFileSize()
			{
				return mFileSize;
			}

			public long getTimeout()
			{
				return mTimeout;
			}
		}
	}

	public static abstract class Send<T> extends CoolTransfer<T>
	{
		public void onOrientatingStreams(Handler handler, FileInputStream fileInputStream, OutputStream outputStream)
		{

		}

		public Handler send(String serverIp, int port, File file, byte[] bufferSize, T extra)
		{
			Handler handler = new Handler(serverIp, port, file, bufferSize, extra);
			Thread thread = new Thread(handler);

			thread.start();

			return handler;
		}

		public Handler sendOnCurrentThread(String serverIp, int port, File file, byte[] bufferSize, T extra)
		{
			Handler handler = new Handler(serverIp, port, file, bufferSize, extra);

			handler.run();

			return handler;
		}

		public class Handler extends CoolTransfer.TransferHandler<T>
		{
			private String mServerIp;

			public Handler(String serverIp, int port, File file, byte[] bufferSize, T extra)
			{
				super(port, file, bufferSize, extra);

				this.mServerIp = serverIp;
			}

			@Override
			protected void onRun()
			{
				addProcess(this);

				Flag flag = onStart(this);

				try
				{
					if (Flag.CONTINUE.equals(flag))
					{
						setSocket(new Socket());

						getSocket().bind(null);
						getSocket().connect(new InetSocketAddress(getServerIp(), getPort()));

						flag = onSocketReady(this);

						if (Flag.CONTINUE.equals(flag))
						{
							FileInputStream inputStream = new FileInputStream(getFile());
							OutputStream outputStream = getSocket().getOutputStream();

							onOrientatingStreams(this, inputStream, outputStream);

							int len;
							int progressPercent = -1;
							long lastNotified = System.currentTimeMillis();

							while ((len = inputStream.read(getBufferSize())) > 0)
							{
								outputStream.write(getBufferSize(), 0, len);
								outputStream.flush();

								if (getNotifyDelay() == -1 || (System.currentTimeMillis() - lastNotified) > getNotifyDelay())
								{
									int currentPercent = (int) (((float) 100 / getFile().length()) * inputStream.getChannel().position());

									if (currentPercent > progressPercent)
									{
										onNotify(this, currentPercent);
										progressPercent = currentPercent;
									}

									lastNotified = System.currentTimeMillis();
								}

								if (this.isInterrupted())
									break;
							}

							outputStream.close();
							inputStream.close();
						}

						getSocket().close();

						if (this.isInterrupted())
						{
							flag = Flag.CANCEL_ALL;
							onInterrupted(this);
						}
						else
							onTransferCompleted(this);
					}
				} catch (Exception e)
				{
					flag = onError(this, e);
				}
				finally
				{
					onStop(this);
					removeProcess(this);
				}

				if (!Flag.CANCEL_ALL.equals(flag))
					onPrepareNext(this);
			}

			public String getServerIp()
			{
				return mServerIp;
			}
		}
	}
}
