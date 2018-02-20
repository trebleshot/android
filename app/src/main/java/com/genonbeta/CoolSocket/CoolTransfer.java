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
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

abstract public class CoolTransfer<T>
{
	public final static int DELAY_DISABLED = -1;

	private final ArrayList<TransferHandler<T>> mProcess = new ArrayList<>();
	private ExecutorService mExecutor;
	private int mNotifyDelay = CoolTransfer.DELAY_DISABLED;
	private Object mBlockingObject = new Object();

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

	public void onOrientatingStreams(TransferHandler<T> handler, InputStream inputStream, OutputStream outputStream)
	{
	}

	public void onProcessListChanged(ArrayList<TransferHandler<T>> processList, TransferHandler<T> handler, boolean isAdded)
	{
	}

	protected void addProcess(TransferHandler<T> processHandler)
	{
		synchronized (getProcessList()) {
			getProcessList().add(processHandler);
			onProcessListChanged(getProcessList(), processHandler, true);
		}
	}

	public Object getBlockingObject()
	{
		return mBlockingObject;
	}

	public ExecutorService getExecutor()
	{
		if (mExecutor == null)
			mExecutor = Executors.newFixedThreadPool(10);

		return mExecutor;
	}

	public int getNotifyDelay()
	{
		return mNotifyDelay;
	}

	public ArrayList<TransferHandler<T>> getProcessList()
	{
		return mProcess;
	}

	public void setBlockingObject(Object blockingObject)
	{
		mBlockingObject = blockingObject;
	}

	public void setExecutor(ExecutorService executor)
	{
		mExecutor = executor;
	}

	public void setNotifyDelay(int delay)
	{
		mNotifyDelay = delay;
	}

	protected void removeProcess(TransferHandler<T> processHandler)
	{
		synchronized (getProcessList()) {
			getProcessList().remove(processHandler);
			onProcessListChanged(getProcessList(), processHandler, false);
		}
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
		private byte[] mBuffer;
		private long mSkippedBytes = 0;

		public TransferHandler(int port, long fileSize, byte[] bufferSize, T extra)
		{
			mExtra = extra;
			mPort = port;
			mFileSize = fileSize;
			mBuffer = bufferSize;
		}

		protected abstract void onRun();

		public void interrupt()
		{
			getTransferProgress().interrupt();
		}

		public boolean isInterrupted()
		{
			return getTransferProgress().isInterrupted();
		}

		public byte[] getBuffer()
		{
			return mBuffer;
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

		public long getSkippedBytes()
		{
			return mSkippedBytes;
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

		public void setTransferProgress(TransferProgress<T> transferProgress)
		{
			mTransferProgress = transferProgress;
		}

		public void skipBytes(long bytes) throws IOException
		{
			if (mSkippedBytes > 0)
				getTransferProgress().decrementTransferredByte(mSkippedBytes);

			getTransferProgress().incrementTransferredByte(mSkippedBytes = bytes);
		}

		@Override
		public void run()
		{
			setStatus(Status.RUNNING);
			onRun();
			setStatus(Status.INTERRUPTED);
		}
	}

	public static abstract class Receive<T> extends CoolTransfer<T>
	{
		public abstract Flag onSocketReady(TransferHandler<T> handler, ServerSocket serverSocket);

		public Handler receive(int port, File file, long fileSize, int bufferSize, int timeOut, T extra, boolean currentThread) throws FileNotFoundException
		{
			return receive(port, new FileOutputStream(file, true), fileSize, bufferSize, timeOut, extra, currentThread);
		}

		public Handler receive(int port, OutputStream outputStream, long fileSize, int bufferSize, int timeOut, T extra, boolean currentThread)
		{
			Handler handler = new Handler(extra, port, outputStream, fileSize, new byte[bufferSize], timeOut);

			if (currentThread)
				handler.run();
			else
				getExecutor().submit(handler);

			return handler;
		}

		public class Handler extends CoolTransfer.TransferHandler<T>
		{
			private int mTimeout;
			private OutputStream mStream;
			private ServerSocket mServerSocket;


			public Handler(T extra, int port, OutputStream stream, long fileSize, byte[] bufferSize, int timeout)
			{
				super(port, fileSize, bufferSize, extra);

				mStream = stream;
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
								onOrientatingStreams(this, inputStream, getOutputStream());

								int len = 0;
								long lastRead = System.currentTimeMillis();

								while (len != -1) {
									synchronized (getBlockingObject()) {
										if ((len = inputStream.read(getBuffer())) > 0) {
											getOutputStream().write(getBuffer(), 0, len);
											getOutputStream().flush();

											lastRead = System.currentTimeMillis();

											getTransferProgress().incrementTransferredByte(len);
										}
									}

									getTransferProgress().doNotify(Receive.this, this);

									if ((mTimeout > 0 && (System.currentTimeMillis() - lastRead) > mTimeout) || isInterrupted()) {
										System.out.println("CoolTransfer: Timed out... Exiting.");
										break;
									}
								}

								getOutputStream().close();
								inputStream.close();
							}
						}

						if (!Flag.CANCEL_CURRENT.equals(getFlag()))
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
					try {
						if (getSocket() != null && !getSocket().isClosed())
							getSocket().close();
					} catch (IOException e) {
						e.printStackTrace();
					}

					try {
						if (getServerSocket() != null && !getServerSocket().isClosed())
							getServerSocket().close();
					} catch (IOException e) {
						e.printStackTrace();
					}

					onStop(this);

					if (!Flag.CANCEL_ALL.equals(getFlag()))
						onPrepareNext(this);

					removeProcess(this);
				}
			}

			public OutputStream getOutputStream()
			{
				return mStream;
			}

			public ServerSocket getServerSocket()
			{
				return mServerSocket;
			}

			public int getTimeout()
			{
				return mTimeout;
			}

			public void setServerSocket(ServerSocket serverSocket)
			{
				mServerSocket = serverSocket;
			}

			@Override
			public void skipBytes(long bytes) throws IOException
			{
				super.skipBytes(bytes);
			}
		}
	}

	public static abstract class Send<T> extends CoolTransfer<T>
	{
		public Handler send(String serverIp, int port, InputStream stream, long totalByte, int bufferSize, T extra, boolean currentThread)
		{
			Handler handler = new Handler(serverIp, port, stream, totalByte, new byte[bufferSize], extra);

			if (currentThread)
				handler.run();
			else
				getExecutor().submit(handler);

			return handler;
		}

		public Handler send(String serverIp, int port, File file, long totalByte, int bufferSize, T extra, boolean currentThread) throws FileNotFoundException
		{
			return send(serverIp, port, new FileInputStream(file), totalByte, bufferSize, extra, currentThread);
		}

		public class Handler extends CoolTransfer.TransferHandler<T>
		{
			private String mServerIp;
			private InputStream mStream;

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
								synchronized (getBlockingObject()) {
									if ((len = getInputStream().read(getBuffer())) > 0) {
										outputStream.write(getBuffer(), 0, len);
										outputStream.flush();

										getTransferProgress().incrementTransferredByte(len);
									}
								}

								getTransferProgress().doNotify(Send.this, this);

								if (isInterrupted())
									break;
							}

							outputStream.close();
							getInputStream().close();
						}

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
					try {
						if (getSocket() != null && !getSocket().isClosed())
							getSocket().close();
					} catch (IOException e) {
						e.printStackTrace();
					}

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

			@Override
			public void skipBytes(long bytes) throws IOException
			{
				super.skipBytes(bytes);
				getInputStream().skip(bytes);
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
		private boolean mInterrupted = false;

		public int calculatePercentage(long max, long current)
		{
			return (int) (((float) 100 / max) * current);
		}

		public long decrementTransferredByte(long size)
		{
			mTransferredByte -= size;
			return mTransferredByte;
		}

		public int decrementTransferredFileCount()
		{
			mTransferredFileCount--;
			return mTransferredFileCount;
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

		public void interrupt()
		{
			mInterrupted = true;
		}

		public boolean isInterrupted()
		{
			return mInterrupted;
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
