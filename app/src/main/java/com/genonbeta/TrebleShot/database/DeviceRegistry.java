package com.genonbeta.TrebleShot.database;

import android.content.ContentValues;
import android.content.Context;

import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/16/17 2:15 PM
 */

public class DeviceRegistry extends MainDatabase
{
	public DeviceRegistry(Context context)
	{
		super(context);
	}

	public boolean exists(NetworkDevice device)
	{
		return exists(device.ip);
	}

	public boolean exists(String ipAddress)
	{
		return getFirstFromTable(new SQLQuery.Select(TABLE_DEVICES)
				.setWhere(FIELD_DEVICES_IP + "=?", ipAddress)) != null;
	}

	public ArrayList<NetworkDevice> getDeviceList()
	{
		ArrayList<NetworkDevice> deviceList = new ArrayList<>();

		for (CursorItem item : getTable(new SQLQuery.Select(TABLE_DEVICES)))
			deviceList.add(new NetworkDevice(item));

		return deviceList;
	}

	public NetworkDevice getNetworkDevice(String ipAddress)
	{
		CursorItem item = getFirstFromTable(new SQLQuery.Select(TABLE_DEVICES)
				.setWhere(FIELD_DEVICES_IP + "=?", ipAddress));

		if (item != null)
			return new NetworkDevice(item);

		return new NetworkDevice(ipAddress);
	}

	public void registerDevice(NetworkDevice device)
	{
		removeDevice(device);

		ContentValues values = new ContentValues();

		values.put(FIELD_DEVICES_IP, device.ip);
		values.put(FIELD_DEVICES_USER, device.user);
		values.put(FIELD_DEVICES_BRAND, device.brand);
		values.put(FIELD_DEVICES_MODEL, device.model);
		values.put(FIELD_DEVICES_ISRESTRICTED, device.isRestricted ? 1 : 0);
		values.put(FIELD_DEVICES_ISLOCALADDRESS, device.isLocalAddress ? 1 : 0);

		getWritableDatabase().insert(TABLE_DEVICES, null, values);
	}

	public void removeAll()
	{
		getWritableDatabase().delete(TABLE_DEVICES, null, null);
	}

	public void removeDevice(NetworkDevice device)
	{
		removeDevice(device.ip);
	}

	public void removeDevice(String ipAddress)
	{
		getWritableDatabase().delete(TABLE_DEVICES, FIELD_DEVICES_IP + "=?", new String[]{ipAddress});
	}

	public boolean updateRestriction(String ipAddress, boolean restrict)
	{
		ContentValues values = new ContentValues();
		values.put(FIELD_DEVICES_ISRESTRICTED, restrict ? 1 : 0);

		getWritableDatabase().update(TABLE_DEVICES, values, FIELD_DEVICES_IP + "=?", new String[]{ipAddress});

		return getAffectedRowCount() > 0;
	}
}