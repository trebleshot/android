package com.genonbeta.CoolSocket;

import java.io.File;

/**
 * Created by: veli
 * Date: 4/29/17 9:52 PM
 */

abstract public class CoolTransferSend<T> extends CoolTransfer<CoolTransferSendHandler<T>>
{
	public CoolTransferSendHandler<T> send(String serverIp, int port, File file, byte[] bufferSize, T extra)
	{
		CoolTransferSendHandler<T> handler = new CoolTransferSendHandler<>((CoolTransfer)this, serverIp, port, file, bufferSize, extra);
		Thread thread = new Thread(handler);

		thread.start();

		return handler;
	}

	public CoolTransferSendHandler<T> sendOnCurrentThread(String serverIp, int port, File file, byte[] bufferSize, T extra)
	{
		CoolTransferSendHandler<T> handler = new CoolTransferSendHandler<>((CoolTransfer)this, serverIp, port, file, bufferSize, extra);

		handler.onRun();

		return handler;
	}
}