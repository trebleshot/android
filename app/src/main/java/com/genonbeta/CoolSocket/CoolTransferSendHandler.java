package com.genonbeta.CoolSocket;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by: veli
 * Date: 4/29/17 10:27 PM
 */

public class CoolTransferSendHandler<T> extends CoolTransfer.TransferHandler<T>
{
	private String mServerIp;
	private int mPort;
	private File mFile;
	private byte[] mBufferSize;

	public CoolTransferSendHandler(CoolTransfer<CoolTransfer.TransferHandler<T>> transfer, String serverIp, int port, File file, byte[] bufferSize, T extra)
	{
		super(transfer, extra);

		this.mServerIp = serverIp;
		this.mPort = port;
		this.mFile = file;
		this.mBufferSize = bufferSize;
	}

	@Override
	protected void onRun()
	{
		getTransfer().getProcessList().add(this);

		if (!getTransfer().onStart(this))
			return;

		try
		{
			setSocket(new Socket());

			getSocket().bind(null);
			getSocket().connect(new InetSocketAddress(this.mServerIp, this.mPort));

			getTransfer().onSocketReady(this);

			FileInputStream inputStream = new FileInputStream(this.mFile);
			OutputStream outputStream = getSocket().getOutputStream();

			int len;
			int progressPercent = -1;
			long lastNotified = System.currentTimeMillis();

			while ((len = inputStream.read(this.mBufferSize)) > 0)
			{
				outputStream.write(this.mBufferSize, 0, len);
				outputStream.flush();

				if (getTransfer().getNotifyDelay() == -1 || (System.currentTimeMillis() - lastNotified) > getTransfer().getNotifyDelay())
				{
					int currentPercent = (int) (((float) 100 / this.mFile.length()) * inputStream.getChannel().position());

					if (currentPercent > progressPercent)
					{
						getTransfer().onNotify(this, currentPercent);
						progressPercent = currentPercent;
					}

					lastNotified = System.currentTimeMillis();
				}

				if (this.isInterrupted())
					break;
			}

			outputStream.close();
			inputStream.close();
			getSocket().close();

			if (this.isInterrupted())
				getTransfer().onInterrupted(this);
			else
				getTransfer().onTransferCompleted(this);
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
}
