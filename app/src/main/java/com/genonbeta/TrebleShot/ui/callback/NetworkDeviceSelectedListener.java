package com.genonbeta.TrebleShot.ui.callback;

import android.support.annotation.Nullable;

import com.genonbeta.TrebleShot.object.NetworkDevice;

/**
 * created by: veli
 * date: 16/04/18 03:18
 */
public interface NetworkDeviceSelectedListener
{
	boolean onNetworkDeviceSelected(NetworkDevice networkDevice, @Nullable NetworkDevice.Connection connection);

	boolean isListenerEffective();
}
