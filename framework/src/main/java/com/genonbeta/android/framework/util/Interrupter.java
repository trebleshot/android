package com.genonbeta.android.framework.util;

import java.util.ArrayList;

/**
 * created by: Veli
 * date: 20.11.2017 00:15
 */

public class Interrupter
{
	private boolean mInterrupted = false;
	private boolean mInterruptedByUser = false;
	private ArrayList<Closer> mClosers = new ArrayList<>();

	public boolean addCloser(Closer closer)
	{
		synchronized (getClosers()) {
			return getClosers().add(closer);
		}
	}

	public boolean hasCloser(Closer closer)
	{
		synchronized (getClosers()) {
			return getClosers().contains(closer);
		}
	}

	public ArrayList<Closer> getClosers()
	{
		return mClosers;
	}

	public boolean interrupted()
	{
		return mInterrupted;
	}

	public boolean interruptedByUser()
	{
		return mInterruptedByUser;
	}

	public boolean interrupt()
	{
		return interrupt(true);
	}

	public boolean interrupt(boolean userAction)
	{
		if (userAction)
			mInterruptedByUser = true;

		if (interrupted())
			return false;

		mInterrupted = true;

		synchronized (getClosers()) {
			for (Closer closer : getClosers())
				closer.onClose(userAction);
		}

		return true;
	}

	public boolean removeCloser(Closer closer)
	{
		synchronized (getClosers()) {
			return getClosers().remove(closer);
		}
	}

	public void reset()
	{
		reset(true);
	}

	public void reset(boolean resetClosers)
	{
		mInterrupted = false;
		mInterruptedByUser = false;

		if (resetClosers)
			removeClosers();
	}

	public void removeClosers()
	{
		synchronized (getClosers()) {
			getClosers().clear();
		}
	}

	public interface Closer
	{
		void onClose(boolean userAction);
	}
}
