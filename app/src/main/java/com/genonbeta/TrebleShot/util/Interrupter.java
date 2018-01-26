package com.genonbeta.TrebleShot.util;

/**
 * created by: Veli
 * date: 20.11.2017 00:15
 */

public class Interrupter
{
	private boolean mInterrupted = false;
	private Closer mCloser;

	public boolean interrupted()
	{
		return mInterrupted;
	}

	public void interrupt()
	{
		mInterrupted = true;

		if (mCloser != null)
			mCloser.onClose();
	}

	public void reset()
	{
		mInterrupted = false;
		mCloser = null;
	}

	public void useCloser(Closer closer)
	{
		mCloser = closer;
	}

	public interface Closer
	{
		void onClose();
	}
}
