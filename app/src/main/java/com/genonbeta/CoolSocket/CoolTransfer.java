package com.genonbeta.CoolSocket;

import android.util.Log;

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
	private int mNotifyDelay = CoolTransfer.DELAY_DISABLED;

	public abstract Flag onError(TransferHandler<T> handler, Exception error);

	public abstract void onNotify(TransferHandler<T> handler, int percentage);

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
		synchronized (mProcess) {
			return mProcess;
		}
	}

	public int getNotifyDelay()
	{
		return mNotifyDelay;
	}

	public void setNotifyDelay(int delay)
	{
		mNotifyDelay = delay;
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
		private Socket mSocket;
		private TransferProgress<T> mTransferProgress;
		private Status mStatus = Status.PENDING;
		private Flag mFlag = Flag.CANCEL_ALL;
		private T mExtra;
		private int mPort;
		private long mFileSize;
		private byte[] mBufferSize;
		private boolean mInterrupted = false;

		public TransferHandler(int port, long fileSize, byte[] bufferSize, T extra)
		{
			mExtra = extra;
			mPort = port;
			mFileSize = fileSize;
			mBufferSize = bufferSize;
		}

		protected abstract void onRun();

		public void interrupt()
		{
			mInterrupted = true;
		}

		public boolean isInterrupted()
		{
			return mInterrupted;
		}

		public byte[] getBufferSize()
		{
			return mBufferSize;
		}

		public File getFile()
		{
			return null;
		}

		public Flag getFlag()
		{
			return mFlag;
		}

		public long getFileSize()
		{
			return mFileSize;
		}

		public T getExtra()
		{
			return mExtra;
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

		public TransferProgress<T> getTransferProgress()
		{
			if (mTransferProgress == null)
				mTransferProgress = new TransferProgress<>();

			return mTransferProgress;
		}

		public TransferHandler<T> linkTo(TransferHandler<T> transferHandler)
		{
			if (transferHandler != null)
				setTransferProgress(transferHandler.getTransferProgress());

			return this;
		}

		public void setFile(File file)
		{
		}

		public void setFlag(Flag flag)
		{
			mFlag = flag;
		}

		protected void setSocket(Socket socket)
		{
			mSocket = socket;
		}

		public void setStatus(Status status)
		{
			mStatus = status;
		}

		public void setTransferProgress(TransferProgress transferProgress)
		{
			mTransferProgress = transferProgress;
		}

		@Override
		public void run()
		{
			mInterrupted = false;

			setStatus(Status.RUNNING);
			onRun();
			setStatus(Status.INTERRUPTED);
		}
	}

	public static abstract class Receive<T> extends CoolTransfer<T>
	{
		public abstract Flag onSocketReady(TransferHandler<T> handler, ServerSocket serverSocket);

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
			private int mTimeout;
			private File mFile;
			private ServerSocket mServerSocket;

			public Handler(T extra, int port, File file, long fileSize, byte[] bufferSize, int timeout)
			{
				super(port, fileSize, bufferSize, extra);

				mFile = file;
				mTimeout = timeout;
			}

			@Override
			protected void onRun()
			{
				addProcess(this);

				setFlag(onStart(this));

				try {
					if (Flag.CONTINUE.equals(getFlag())) {
						setServerSocket(new ServerSocket(getPort()));

						if (getTimeout() != CoolSocket.NO_TIMEOUT)
							getServerSocket().setSoTimeout(getTimeout());

						setFlag(onSocketReady(this, getServerSocket()));

						if (Flag.CONTINUE.equals(getFlag())) {
							setSocket(getServerSocket().accept());

							if (getTimeout() != CoolSocket.NO_TIMEOUT)
								getSocket().setSoTimeout(getTimeout());

							setFlag(onSocketReady(this));

							if (Flag.CONTINUE.equals(getFlag())) {
								InputStream inputStream = getSocket().getInputStream();
								FileOutputStream outputStream = new FileOutputStream(getFile(), getFile().length() > 0);

								getTransferProgress().incrementTransferredByte(getFile().length());
								onOrientatingStreams(this, inputStream, outputStream);

								int len = 0;
								long lastRead = System.currentTimeMillis();

								while (len != -1) {
									if ((len = inputStream.read(getBufferSize())) > 0) {
										outputStream.write(getBufferSize(), 0, len);
										outputStream.flush();

										lastRead = System.currentTimeMillis();

										getTransferProgress().incrementTransferredByte(len);
									}

									getTransferProgress().doNotify(Receive.this, this);

									if ((mTimeout > 0 && (System.currentTimeMillis() - lastRead) > mTimeout) || isInterrupted()) {
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

						if (!Flag.CANCEL_CURRENT.equals(getFlag()))
							if (isInterrupted()) {
								setFlag(Flag.CANCEL_ALL);
								onInterrupted(this);
							} else {
								if (getFile().length() != getFileSize())
									throw new NotYetBoundException();
								else {
									getTransferProgress().incrementTransferredFileCount();
									onTransferCompleted(this);
								}
							}
					}
				} catch (Exception e) {
					setFlag(onError(this, e));
				} finally {
					onStop(this);

					if (!Flag.CANCEL_ALL.equals(getFlag()))
						onPrepareNext(this);

					removeProcess(this);
				}
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
				mFile = file;
			}

			public void setServerSocket(ServerSocket serverSocket)
			{
				mServerSocket = serverSocket;
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
			private long mSkippedBytes = 0;

			public Handler(String serverIp, int port, InputStream stream, long fileSize, byte[] bufferSize, T extra)
			{
				super(port, fileSize, bufferSize, extra);

				mServerIp = serverIp;
				mStream = stream;
			}

			@Override
			protected void onRun()
			{
				addProcess(this);

				setFlag(onStart(this));

				try {
					if (Flag.CONTINUE.equals(getFlag())) {
						setSocket(new Socket());

						getSocket().bind(null);
						getSocket().connect(new InetSocketAddress(getServerIp(), getPort()));

						setFlag(onSocketReady(this));

						if (Flag.CONTINUE.equals(getFlag())) {
							OutputStream outputStream = getSocket().getOutputStream();

							onOrientatingStreams(this, getInputStream(), outputStream);

							int len = 0;

							getTransferProgress().incrementTransferredByte(getSkippedBytes());

							while (len != -1) {
								if ((len = getInputStream().read(getBufferSize())) > 0) {
									outputStream.write(getBufferSize(), 0, len);
									outputStream.flush();

									getTransferProgress().incrementTransferredByte(len);
								}

								getTransferProgress().doNotify(Send.this, this);

								if (isInterrupted())
									break;
							}

							outputStream.close();
							getInputStream().close();
						}

						getSocket().close();

						if (isInterrupted()) {
							setFlag(Flag.CANCEL_ALL);
							onInterrupted(this);
						} else {
							getTransferProgress().incrementTransferredFileCount();
							onTransferCompleted(this);
						}
					}
				} catch (Exception e) {
					setFlag(onError(this, e));
				} finally {
					onStop(this);

					if (!Flag.CANCEL_ALL.equals(getFlag()))
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

	public static class TransferProgress<T>
	{
		private long mStartTime = System.currentTimeMillis();
		private long mTransferredByte;
		private long mTotalByte;
		private long mTimeElapsed;
		private long mTimePassed;
		private long mTimeRemaining;
		private long mLastNotified;
		private int mTransferredFileCount;

		public int calculatePercentage(long max, long current)
		{
			return (int) (((float) 100 / max) * current);
		}

		public boolean doNotify(CoolTransfer<T> transfer, TransferHandler<T> handler)
		{
			if (transfer.getNotifyDelay() != -1 && (System.currentTimeMillis() - getLastNotified()) < transfer.getNotifyDelay())
				return false;

			int percentage = calculatePercentage(getTotalByte(), getTransferredByte());

			setTimeElapsed(System.currentTimeMillis() - getStartTime());

			if (getTotalByte() > 0 && getTransferredByte() > 0) {
				setTimePassed(getTimeElapsed() * getTotalByte() / getTransferredByte());
				setTimeRemaining(getTimePassed() - getTimeElapsed());
			}

			transfer.onNotify(handler, percentage);

			mLastNotified = System.currentTimeMillis();

			return true;
		}

		public long getLastNotified()
		{
			return mLastNotified;
		}

		public long getStartTime()
		{
			return mStartTime;
		}

		public long getTimeElapsed()
		{
			return mTimeElapsed;
		}

		public long getTimePassed()
		{
			return mTimePassed;
		}

		public long getTimeRemaining()
		{
			return mTimeRemaining;
		}

		public long getTotalByte()
		{
			return mTotalByte;
		}

		public int getTransferredFileCount()
		{
			return mTransferredFileCount;
		}

		public long getTransferredByte()
		{
			return mTransferredByte;
		}

		public long incrementTransferredByte(long size)
		{
			mTransferredByte += size;
			return mTransferredByte;
		}

		public int incrementTransferredFileCount()
		{
			mTransferredFileCount++;
			return mTransferredFileCount;
		}

		public void setTotalByte(long totalByte)
		{
			mTotalByte = totalByte;
		}

		public void setTransferredByte(long transferredByte)
		{
			mTransferredByte = transferredByte;
		}

		public void setTransferredFileCount(int transferredFileCount)
		{
			mTransferredFileCount = transferredFileCount;
		}

		public void setStartTime(long startTime)
		{
			mStartTime = startTime;
		}

		public void setTimeElapsed(long timeElapsed)
		{
			mTimeElapsed = timeElapsed;
		}

		public void setTimePassed(long timePassed)
		{
			mTimePassed = timePassed;
		}

		public void setTimeRemaining(long timeRemaining)
		{
			mTimeRemaining = timeRemaining;
		}
	}
}
