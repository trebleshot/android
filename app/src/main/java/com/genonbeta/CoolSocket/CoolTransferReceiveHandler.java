package com.genonbeta.CoolSocket;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.NotYetBoundException;

/**
 * Created by: veli
 * Date: 4/29/17 10:50 PM
 */

public class CoolTransferReceiveHandler<T> extends CoolTransfer.TransferHandler<T>
{
	private int mPort;
	private File mFile;
	private long mFileSize;
	private byte[] mBufferSize;
	private int mTimeout;
	private Socket mSocket;
	private T mExtra;

	public CoolTransferReceiveHandler(CoolTransfer<CoolTransfer.TransferHandler<T>> transfer, T extra, int port, File file, long fileSize, byte[] bufferSize, int timeout)
	{
		super(transfer, extra);

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
		getTransfer().getProcessList().add(this);

		if (!getTransfer().onStart(this))
			return;

		try
		{
			ServerSocket serverSocket = new ServerSocket(this.mPort);

			getTransfer().onSocketReady(this, serverSocket);

			mSocket = serverSocket.accept();

			InputStream inputStream = mSocket.getInputStream();
			FileOutputStream outputStream = new FileOutputStream(this.mFile);

			int len;
			int progressPercent = -1;
			long lastRead = System.currentTimeMillis();
			long lastNotified = System.currentTimeMillis();

			Log.d("CoolTransfer", "Filesize: " + this.mFile.length());

			if(this.mFile.length() > 0)
				outputStream.getChannel().position(this.mFile.length());

			while (this.mFile.length() != this.mFileSize)
			{
				if ((len = inputStream.read(this.mBufferSize)) > 0)
				{
					outputStream.write(this.mBufferSize, 0, len);
					outputStream.flush();

					lastRead = System.currentTimeMillis();
				}

				if (getTransfer().getNotifyDelay() == -1 || (System.currentTimeMillis() - lastNotified) > getTransfer().getNotifyDelay())
				{
					int currentPercent = (int) (((float) 100 / this.mFileSize) * outputStream.getChannel().position());

					if (currentPercent > progressPercent)
					{
						getTransfer().onNotify(this, currentPercent);
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
				getTransfer().onInterrupted(this);
			else
			{
				if (this.mFile.length() != this.mFileSize)
					throw new NotYetBoundException();
				else
					getTransfer().onTransferCompleted(this);
			}
		} catch (Exception e)
		{
			getTransfer().onError(this, e);
		}
		finally
		{
			getTransfer().onStop(this);
			getTransfer().getProcessList().remove(this);
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
}
