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

import android.util.Log;
import androidx.annotation.NonNull;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class NetworkDeviceScanner implements Runnable
{
    public static final String TAG = NetworkDeviceScanner.class.getSimpleName();

    private final List<NetworkInterface> mInterfaces = new ArrayList<>();
    private final ScannerExecutor mExecutor = new ScannerExecutor();
    private boolean mInterrupted = false;
    private volatile ScannerHandler mHandler;
    private int mMaxThreads = 6;
    private NetworkInterface mActiveInterface;
    private volatile short mActiveThreads = 0;
    private volatile byte[] mAddressPrefix = new byte[4];
    private volatile boolean[] mIPRange = new boolean[256];

    public NetworkDeviceScanner()
    {
    }

    public NetworkDeviceScanner(int numberOfThreads)
    {
        mMaxThreads = numberOfThreads;
    }

    public boolean interrupt()
    {
        if (mInterrupted)
            return false;

        mInterrupted = true;
        return true;
    }

    public boolean isBusy()
    {
        synchronized (mInterfaces) {
            return mInterfaces.size() > 0;
        }
    }

    private synchronized boolean nextThread()
    {
        synchronized (mInterfaces) {
            mActiveInterface = mInterfaces.size() > 0 ? mInterfaces.get(0) : null;
        }

        if (mActiveInterface == null)
            return false;

        mAddressPrefix = NetworkUtils.getFirstInet4Address(mActiveInterface).getAddress();

        for (short i = 0; i < mIPRange.length; i++)
            mIPRange[i] = false;

        for (int i = 0; i < mMaxThreads; i++)
            mExecutor.execute(this);

        return true;
    }

    public boolean scan(List<NetworkInterface> interfaceList, ScannerHandler handler)
    {
        if (isBusy() || interfaceList.size() <= 0)
            return false;

        mHandler = handler;

        synchronized (mInterfaces) {
            mInterfaces.clear();
            mInterfaces.addAll(interfaceList);
        }

        return nextThread();
    }

    private synchronized short updateThreadCounter(boolean add) {
        if (add)
            return ++mActiveThreads;
        else
            return --mActiveThreads;
    }

    @Override
    public void run()
    {
        updateThreadCounter(true);

        byte[] prefix = mAddressPrefix;
        NetworkInterface networkInterface = mActiveInterface;

        for (short pos = 1; pos < mIPRange.length; pos++) {
            if (mInterrupted)
                break;

            if (mIPRange[pos])
                continue;

            mIPRange[pos] = true;

            try {
                prefix[3] = (byte) pos;
                InetAddress inetAddress = InetAddress.getByAddress(prefix);

                if (inetAddress.isReachable(300) && mHandler != null)
                    mHandler.onDeviceFound(inetAddress, networkInterface);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mActiveThreads > 0 && updateThreadCounter(false) == 0) {
            synchronized (mInterfaces) {
                if (mInterrupted) {
                    mInterfaces.clear();
                    mInterrupted = false;
                } else
                    mInterfaces.remove(0);
            }

            if (mHandler != null)
                mHandler.onThreadsCompleted();

            nextThread();
        }
    }

    public interface ScannerHandler
    {
        void onDeviceFound(InetAddress address, NetworkInterface networkInterface);

        void onThreadsCompleted();
    }

    protected static class ScannerExecutor implements Executor
    {
        @Override
        public void execute(@NonNull Runnable scanner)
        {
            new Thread(scanner).start();
        }
    }
}
