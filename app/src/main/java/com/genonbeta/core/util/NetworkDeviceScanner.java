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
        this.mNumberOfThreads = numberOfThreads;
    }

    public boolean interrupt()
    {
        if (!this.mIsBreakRequested)
            this.mIsBreakRequested = true;
        else
            return false;

        return true;
    }

    public boolean isScannerAvaiable()
    {
        return (mInterfaces.size() == 0 && !mIsLockRequested);
    }

    private void nextThread()
    {
        if (this.mIsLockRequested)
            return;

        if (this.isScannerAvaiable())
        {
            // this sequence only works when threads complete the job

            this.mIsBreakRequested = false;

            if (this.mHandler != null)
            {
                this.setLock(true); // lock scanner
                this.mHandler.onThreadsCompleted();
                this.setLock(false); // release lock
            }

            return;
        }

        this.mScanner.updateScanner();

        for (int threadsStarted = this.mNumberOfThreads; threadsStarted > 0; threadsStarted--)
        {
            mExecutor.execute(this.mScanner);
        }
    }

    public boolean scan(ArrayList<String> interfaces, ScannerHandler handler)
    {
        if (!this.isScannerAvaiable() || interfaces.size() < 1)
            return false;

        this.mInterfaces.addAll(interfaces);

        this.mHandler = handler;
        this.nextThread();

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
            String addressPrefix = ipAddress.substring(0, ipAddress.lastIndexOf(".") + 1);

            this.mAddressPrefix = addressPrefix;
            this.mDevices = new boolean[256];
            this.mThreadsExited = NetworkDeviceScanner.this.mNumberOfThreads;
        }

        @Override
        public void run()
        {
            for (int mPosition = 0; mPosition < mDevices.length; mPosition++)
            {
                synchronized (mDevices)
                {
                    if (mDevices[mPosition] == true || mPosition == 0 || NetworkDeviceScanner.this.mIsBreakRequested == true)
                        continue;

                    mDevices[mPosition] = true;
                }

                try
                {
                    InetAddress inet = InetAddress.getByName(mAddressPrefix + mPosition);

                    if (inet.isReachable(300) && NetworkDeviceScanner.this.mHandler != null)
                        NetworkDeviceScanner.this.mHandler.onDeviceFound(inet);
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            this.mThreadsExited--;

            if (this.mThreadsExited == 0)
            {
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
