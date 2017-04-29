package com.genonbeta.CoolSocket;

import java.io.File;

/**
 * Created by: veli
 * Date: 4/29/17 10:50 PM
 */

abstract public class CoolTransferReceive<T> extends CoolTransfer<CoolTransferReceiveHandler<T>>
{
	public CoolTransferReceiveHandler<T> receive(int port, File file, long fileSize, byte[] bufferSize, int timeOut, T extra)
	{
		CoolTransferReceiveHandler<T> handler = new CoolTransferReceiveHandler<>((CoolTransfer)this, extra, port, file, fileSize, bufferSize, timeOut);
		Thread thread = new Thread(handler);

		thread.start();

		return handler;
	}

	public CoolTransferReceiveHandler<T> receiveOnCurrentThread(int port, File file, long fileSize, byte[] bufferSize, int timeOut, T extra)
	{
		CoolTransferReceiveHandler<T> handler = new CoolTransferReceiveHandler<>((CoolTransfer)this, extra, port, file, fileSize, bufferSize, timeOut);

		handler.onRun();

		return handler;
	}
}
