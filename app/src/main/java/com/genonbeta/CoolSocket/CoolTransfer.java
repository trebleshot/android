package com.genonbeta.CoolSocket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
		synchronized (this.mProcess) {
			return this.mProcess;
		}
	}

	public int getNotifyDelay()
	{
		return notifyDelay;
	}

	public void setNotifyDelay(int delay)
	{
		this.notifyDelay = delay;
	}

	protected void removeProcess(TransferHandler<T> processHandler)
	{
		getProcessList().remove(processHandler);
		onProcessListChanged(getProcessList(), processHandler, false);
	}

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

	public abstract static class TransferHandler<T> implements Runnable
	{
		private boolean mInterrupted = false;
		private Socket mSocket;
		private T mExtra;
		private byte[] mBufferSize;
		private int mPort;
		private Status mStatus = Status.PENDING;

		public TransferHandler(int port, byte[] bufferSize, T extra)
		{
			this.mExtra = extra;
			this.mPort = port;
			this.mBufferSize = bufferSize;
		}

		protected abstract void onRun();

		public void interrupt()
		{
			this.mInterrupted = true;
		}

		public boolean isInterrupted()
		{
			return this.mInterrupted;
		}

		public byte[] getBufferSize()
		{
			return mBufferSize;
		}

		public File getFile()
		{
			return null;
		}

		public T getExtra()
		{
			return mExtra;
		}

		public InputStream getInputStream()
		{
			return null;
		}

		public int getPort()
		{
			return mPort;
		}

		public Socket getSocket()
		{
			return mSocket;
		}

		protected void setSocket(Socket mSocket)
		{
			this.mSocket = mSocket;
		}

		public Status getStatus()
		{
			return mStatus;
		}

		public void setStatus(Status status)
		{
			this.mStatus = status;
		}

		public void setFile(File file)
		{
		}

		@Override
		public void run()
		{
			this.mInterrupted = false;

			this.setStatus(Status.RUNNING);
			this.onRun();
			this.setStatus(Status.INTERRUPTED);
		}
	}

	public static abstract class Receive<T> extends CoolTransfer<T>
	{
		public abstract Flag onSocketReady(Handler handler, ServerSocket serverSocket);

		public void onOrientatingStreams(Handler handler, InputStream inputStream, FileOutputStream fileOutputStream)
		{

		}

		public Handler receive(int port, File file, long fileSize, byte[] bufferSize, int timeOut, T extra, boolean currentThread)
		{
			Handler handler = new Handler(extra, port, file, fileSize, bufferSize, timeOut);

			if (currentThread)
				handler.run();
			else {
				Thread thread = new Thread(handler);
				thread.start();
			}

			return handler;
		}

		public class Handler extends CoolTransfer.TransferHandler<T>
		{
			private long mFileSize;
			private int mTimeout;
			private File mFile;
			private ServerSocket mServerSocket;

			public Handler(T extra, int port, File file, long fileSize, byte[] bufferSize, int timeout)
			{
				super(port, bufferSize, extra);

				this.mFile = file;
				this.mFileSize = fileSize;
				this.mTimeout = timeout;
			}

			@Override
			protected void onRun()
			{
				addProcess(this);

				Flag flag = onStart(this);

				try {
					if (Flag.CONTINUE.equals(flag)) {
						mServerSocket = new ServerSocket(getPort());

						if (getTimeout() != CoolSocket.NO_TIMEOUT)
							getServerSocket().setSoTimeout(getTimeout());

						flag = onSocketReady(this, getServerSocket());

						if (Flag.CONTINUE.equals(flag)) {
							setSocket(getServerSocket().accept());

							if (getTimeout() != CoolSocket.NO_TIMEOUT)
								getSocket().setSoTimeout(getTimeout());

							flag = onSocketReady(this);

							if (Flag.CONTINUE.equals(flag)) {
								InputStream inputStream = getSocket().getInputStream();
								FileOutputStream outputStream = new FileOutputStream(getFile(), getFile().length() > 0);

								onOrientatingStreams(this, inputStream, outputStream);

								int len;
								int progressPercent = -1;
								long lastRead = System.currentTimeMillis();
								long lastNotified = System.currentTimeMillis();

								while (getFile().length() != this.mFileSize) {
									if ((len = inputStream.read(getBufferSize())) > 0) {
										outputStream.write(getBufferSize(), 0, len);
										outputStream.flush();

										lastRead = System.currentTimeMillis();
									}

									if (getNotifyDelay() == -1 || (System.currentTimeMillis() - lastNotified) > getNotifyDelay()) {
										int currentPercent = (int) (((float) 100 / this.mFileSize) * outputStream.getChannel().position());

										if (currentPercent > progressPercent) {
											onNotify(this, currentPercent);
											progressPercent = currentPercent;
										}

										lastNotified = System.currentTimeMillis();
									}

									if ((this.mTimeout > 0 && (System.currentTimeMillis() - lastRead) > this.mTimeout) || this.isInterrupted()) {
										System.out.println("CoolTransfer: Timed out... Exiting.");
										break;
									}
								}

								outputStream.close();
								inputStream.close();
							}

							getSocket().close();

						}

						getServerSocket().close();

						if (!Flag.CANCEL_CURRENT.equals(flag))
							if (this.isInterrupted()) {
								flag = Flag.CANCEL_ALL;
								onInterrupted(this);
							} else {
								if (getFile().length() != this.mFileSize)
									throw new NotYetBoundException();
								else
									onTransferCompleted(this);
							}
					}
				} catch (Exception e) {
					flag = onError(this, e);
				} finally {
					onStop(this);

					if (!Flag.CANCEL_ALL.equals(flag))
						onPrepareNext(this);

					removeProcess(this);
				}
			}

			public long getFileSize()
			{
				return mFileSize;
			}

			public File getFile()
			{
				return mFile;
			}

			public ServerSocket getServerSocket()
			{
				return mServerSocket;
			}

			public int getTimeout()
			{
				return mTimeout;
			}

			public void setFile(File file)
			{
				this.mFile = file;
			}

			public void updateFile(File newAddress)
			{
				mFile = newAddress;
			}
		}
	}

	public static abstract class Send<T> extends CoolTransfer<T>
	{
		public void onOrientatingStreams(Handler handler, InputStream inputStream, OutputStream outputStream)
		{
		}

		public Handler send(String serverIp, int port, InputStream stream, long totalByte, byte[] bufferSize, T extra, boolean currentThread)
		{
			Handler handler = new Handler(serverIp, port, stream, totalByte, bufferSize, extra);

			if (currentThread)
				handler.run();
			else
				new Thread(handler).start();

			return handler;
		}

		public Handler send(String serverIp, int port, File file, long totalByte, byte[] bufferSize, T extra, boolean currentThread) throws FileNotFoundException
		{
			return send(serverIp, port, new FileInputStream(file), totalByte, bufferSize, extra, currentThread);
		}

		public class Handler extends CoolTransfer.TransferHandler<T>
		{
			private String mServerIp;
			private InputStream mStream;
			private long mTotalByte;
			private long mSkippedBytes = 0;

			public Handler(String serverIp, int port, InputStream stream, long totalLenght, byte[] bufferSize, T extra)
			{
				super(port, bufferSize, extra);

				this.mServerIp = serverIp;
				this.mStream = stream;
				this.mTotalByte = totalLenght;
			}

			@Override
			protected void onRun()
			{
				addProcess(this);

				Flag flag = onStart(this);

				try {
					if (Flag.CONTINUE.equals(flag)) {
						setSocket(new Socket());

						getSocket().bind(null);
						getSocket().connect(new InetSocketAddress(getServerIp(), getPort()));

						flag = onSocketReady(this);

						if (Flag.CONTINUE.equals(flag)) {
							OutputStream outputStream = getSocket().getOutputStream();

							onOrientatingStreams(this, getInputStream(), outputStream);

							int len;
							int progressPercent = -1;
							long lastNotified = System.currentTimeMillis();
							long countingStars = getSkippedBytes();

							while ((len = getInputStream().read(getBufferSize())) > 0) {
								outputStream.write(getBufferSize(), 0, len);
								outputStream.flush();

								countingStars += len;

								if (getNotifyDelay() == -1 || (System.currentTimeMillis() - lastNotified) > getNotifyDelay()) {
									int currentPercent = (int) (((float) 100 / getTotalByte()) * countingStars);

									if (currentPercent > progressPercent) {
										onNotify(this, currentPercent);
										progressPercent = currentPercent;
									}

									lastNotified = System.currentTimeMillis();
								}

								if (this.isInterrupted())
									break;
							}

							outputStream.close();
							getInputStream().close();
						}

						getSocket().close();

						if (this.isInterrupted()) {
							flag = Flag.CANCEL_ALL;
							onInterrupted(this);
						} else
							onTransferCompleted(this);
					}
				} catch (Exception e) {
					flag = onError(this, e);
				} finally {
					onStop(this);

					if (!Flag.CANCEL_ALL.equals(flag))
						onPrepareNext(this);

					removeProcess(this);
				}
			}

			public InputStream getInputStream()
			{
				return mStream;
			}

			public String getServerIp()
			{
				return mServerIp;
			}

			public long getTotalByte()
			{
				return mTotalByte;
			}

			public long getSkippedBytes()
			{
				return mSkippedBytes;
			}

			public long skipBytes(long bytes) throws IOException
			{
				mSkippedBytes = bytes;
				return getInputStream().skip(bytes);
			}
		}
	}
}
