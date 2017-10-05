package com.genonbeta.core.util;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.Executor;

public class NetworkDeviceScanner
{
	private ArrayList<String> mInterfaces = new ArrayList<String>();
	private ScannerExecutor mExecutor = new ScannerExecutor();
	private Scanner mScanner = new Scanner();
	private boolean mIsBreakRequested = false;
	private boolean mIsLockRequested = false;
	private ScannerHandler mHandler;
	private int mNumberOfThreads = 6;

	public NetworkDeviceScanner()
	{
	}

	public NetworkDeviceScanner(int numberOfThreads)
	{
		mNumberOfThreads = numberOfThreads;
	}

	public boolean interrupt()
	{
		if (!mIsBreakRequested)
			mIsBreakRequested = true;
		else
			return false;

		return true;
	}

	public boolean isScannerAvailable()
	{
		return (mInterfaces.size() == 0 && !mIsLockRequested);
	}

	private void nextThread()
	{
		if (mIsLockRequested)
			return;

		if (isScannerAvailable()) {
			// this sequence only works when threads complete the job

			mIsBreakRequested = false;

			if (mHandler != null) {
				setLock(true); // lock scanner
				mHandler.onThreadsCompleted();
				setLock(false); // release lock
			}

			return;
		}

		mScanner.updateScanner();

		for (int threadsStarted = mNumberOfThreads; threadsStarted > 0; threadsStarted--) {
			mExecutor.execute(mScanner);
		}
	}

	public boolean scan(ArrayList<String> interfaces, ScannerHandler handler)
	{
		if (!isScannerAvailable() || interfaces.size() < 1)
			return false;

		mInterfaces.addAll(interfaces);

		mHandler = handler;
		nextThread();

		return true;
	}

	public void setLock(boolean lock)
	{
		mIsLockRequested = lock;
	}

	public static interface ScannerHandler
	{
		public void onDeviceFound(InetAddress address);

		public void onThreadsCompleted();
	}

	protected class Scanner implements Runnable
	{
		private String mAddressPrefix = "192.168.0.";
		private boolean[] mDevices = new boolean[256];
		private int mThreadsExited = 0;

		public Scanner()
		{
		}

		public void updateScanner()
		{
			String ipAddress = NetworkDeviceScanner.this.mInterfaces.get(0);

			mAddressPrefix = NetworkUtils.getAddressPrefix(ipAddress);
			mDevices = new boolean[256];
			mThreadsExited = NetworkDeviceScanner.this.mNumberOfThreads;
		}

		@Override
		public void run()
		{
			for (int mPosition = 0; mPosition < mDevices.length; mPosition++) {
				synchronized (mDevices) {
					if (mDevices[mPosition] || mPosition == 0 || NetworkDeviceScanner.this.mIsBreakRequested)
						continue;

					mDevices[mPosition] = true;
				}

				try {
					InetAddress inet = InetAddress.getByName(mAddressPrefix + mPosition);

					if (inet.isReachable(300) && NetworkDeviceScanner.this.mHandler != null)
						NetworkDeviceScanner.this.mHandler.onDeviceFound(inet);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			mThreadsExited--;

			if (mThreadsExited == 0) {
				NetworkDeviceScanner.this.mInterfaces.remove(0);
				NetworkDeviceScanner.this.nextThread();
			}
		}
	}

	protected class ScannerExecutor implements Executor
	{
		@Override
		public void execute(Runnable scanner)
		{
			new Thread(scanner).start();
		}
	}
}
