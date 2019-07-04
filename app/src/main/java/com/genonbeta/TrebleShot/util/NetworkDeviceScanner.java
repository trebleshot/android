/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class NetworkDeviceScanner
{
    private List<AddressedInterface> mInterfaces = new ArrayList<>();
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

    public boolean scan(List<AddressedInterface> interfaceList, ScannerHandler handler)
    {
        if (!isScannerAvailable() || interfaceList.size() < 1)
            return false;

        mInterfaces.addAll(interfaceList);
        mHandler = handler;

        nextThread();

        return true;
    }

    public void setLock(boolean lock)
    {
        mIsLockRequested = lock;
    }

    public interface ScannerHandler
    {
        void onDeviceFound(InetAddress address, NetworkInterface networkInterface);

        void onThreadsCompleted();
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
            mAddressPrefix = NetworkUtils.getAddressPrefix(mInterfaces.get(0).getAssociatedAddress());
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
                    InetAddress inetAddress = InetAddress.getByName(mAddressPrefix + mPosition);

                    if (inetAddress.isReachable(300) && NetworkDeviceScanner.this.mHandler != null)
                        NetworkDeviceScanner.this.mHandler.onDeviceFound(inetAddress, mInterfaces.get(0).getNetworkInterface());
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
