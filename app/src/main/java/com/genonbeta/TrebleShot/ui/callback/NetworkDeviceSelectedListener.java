package com.genonbeta.TrebleShot.ui.callback;

import com.genonbeta.TrebleShot.object.NetworkDevice;

import androidx.annotation.Nullable;

/**
 * created by: veli
 * date: 16/04/18 03:18
 */
public interface NetworkDeviceSelectedListener
{
	boolean onNetworkDeviceSelected(NetworkDevice networkDevice, @Nullable NetworkDevice.Connection connection);

	boolean isListenerEffective();
}
