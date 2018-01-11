package com.genonbeta.TrebleShot.object;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.exception.ConnectionNotFoundException;
import com.genonbeta.TrebleShot.exception.DeviceNotFoundException;
import com.genonbeta.TrebleShot.exception.TransactionGroupNotFoundException;
import com.genonbeta.TrebleShot.util.NetworkDeviceInfoLoader;
import com.genonbeta.TrebleShot.util.NetworkDeviceScanner;

/**
 * created by: Veli
 * date: 9.01.2018 18:40
 */

public class TransferInstance
{
	public NetworkDevice.Connection mConnection;
	public NetworkDevice mDevice;
	public TransactionObject.Group mGroup;

	public TransferInstance(AccessDatabase database, int groupId) throws TransactionGroupNotFoundException, DeviceNotFoundException, ConnectionNotFoundException
	{
		initialize(database, groupId);

		mConnection = new NetworkDevice.Connection(mGroup.deviceId, mGroup.connectionAdapter);

		try {
			database.reconstruct(mConnection);
		} catch (Exception e) {
			throw new ConnectionNotFoundException();
		}
	}

	public TransferInstance(AccessDatabase database, int groupId, String currentConnection) throws DeviceNotFoundException, TransactionGroupNotFoundException
	{
		initialize(database, groupId);

		mConnection = NetworkDeviceInfoLoader.processConnection(database, mDevice, currentConnection);

		if (!mConnection.adapterName.equals(mGroup.connectionAdapter)) {
			mGroup.connectionAdapter = mConnection.adapterName;
			database.publish(mGroup);
		}
	}

	protected void initialize(AccessDatabase database, int groupId) throws TransactionGroupNotFoundException, DeviceNotFoundException
	{
		mGroup = new TransactionObject.Group(groupId);

		try {
			database.reconstruct(mGroup);
		} catch (Exception e) {
			throw new TransactionGroupNotFoundException();
		}

		mDevice = new NetworkDevice(mGroup.deviceId);

		try {
			database.reconstruct(mDevice);
		} catch (Exception e) {
			throw new DeviceNotFoundException();
		}
	}

	public NetworkDevice.Connection getConnection()
	{
		return mConnection;
	}

	public NetworkDevice getDevice()
	{
		return mDevice;
	}

	public TransactionObject.Group getGroup()
	{
		return mGroup;
	}
}
