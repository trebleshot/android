package com.genonbeta.TrebleShot.callback;

import com.genonbeta.TrebleShot.object.NetworkDevice;

import java.util.ArrayList;

public interface OnDeviceSelectedListener
{
    void onDeviceSelected(NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> availableInterfaces);
}
