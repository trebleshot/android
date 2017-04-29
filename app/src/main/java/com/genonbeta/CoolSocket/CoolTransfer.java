package com.genonbeta.CoolSocket;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

abstract public class CoolTransfer<T extends CoolTransfer.TransferHandler>
{
	public final static int DELAY_DISABLED = -1;
	private final ArrayList<T> mProcess = new ArrayList<>();
	private int notifyDelay = CoolTransfer.DELAY_DISABLED;

	public abstract void onError(T handler, Exception error);

	public abstract void onNotify(T handler, int percent);

	public abstract void onTransferCompleted(T handler);

	public abstract void onInterrupted(T handler);

	public abstract void onSocketReady(T handler);

	public abstract void onSocketReady(T handler, ServerSocket serverSocket);

	public abstract boolean onStart(T handler);

	public void onStop(T handler)
	{
	}

	public ArrayList<T> getProcessList()
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

	public void setNotifyDelay(int delay)
	{
		this.notifyDelay = delay;
	}

	public abstract static class TransferHandler<E> implements Runnable
	{
		private boolean mInterrupted = false;
		private CoolTransfer<TransferHandler<E>> mTransfer;
		private Socket mSocket;
		private E mExtra;

		public TransferHandler(CoolTransfer<TransferHandler<E>> transfer, E extra)
		{
			mTransfer = transfer;
			mExtra = extra;
		}

		protected abstract void onRun();

		public E getExtra()
		{
			return mExtra;
		}

		public Socket getSocket()
		{
			return mSocket;
		}

		public CoolTransfer<TransferHandler<E>> getTransfer()
		{
			return mTransfer;
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
			this.onRun();
		}

		protected void setSocket(Socket mSocket)
		{
			this.mSocket = mSocket;
		}
	}
}
