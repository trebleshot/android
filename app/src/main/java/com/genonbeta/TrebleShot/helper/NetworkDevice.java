package com.genonbeta.TrebleShot.helper;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.DeviceRegistry;
import com.genonbeta.android.database.CursorItem;

public class NetworkDevice
{
	public String ip;
	public String brand;
	public String model;
	public String user;
	public String deviceId;
	public boolean isRestricted = false;
	public boolean isLocalAddress = false;
	public String[] availableConnections;

	public NetworkDevice(String ip)
	{
		this.ip = ip;
		this.availableConnections = new String[] {ip};
	}

	public NetworkDevice(CursorItem item)
	{
		this(item.getString(DeviceRegistry.FIELD_DEVICES_IP));

		this.user = item.getString(DeviceRegistry.FIELD_DEVICES_USER);
		this.brand = item.getString(DeviceRegistry.FIELD_DEVICES_BRAND);
		this.model = item.getString(DeviceRegistry.FIELD_DEVICES_MODEL);
		this.deviceId = item.getString(DeviceRegistry.FIELD_DEVICES_ID);
		this.isRestricted = item.getInt(DeviceRegistry.FIELD_DEVICES_ISRESTRICTED) == 1;
		this.isLocalAddress = item.getInt(DeviceRegistry.FIELD_DEVICES_ISLOCALADDRESS) == 1;

		if (item.exists(DeviceRegistry.FIELD_DEVICES_AVAILABLE_CONNECTIONS))
			this.availableConnections = item.getString(DeviceRegistry.FIELD_DEVICES_AVAILABLE_CONNECTIONS).split(":");
	}

	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();
		registerValues(values);

		return values;
	}

	public void registerValues(ContentValues values)
	{
		values.put(DeviceRegistry.FIELD_DEVICES_IP, ip);
		values.put(DeviceRegistry.FIELD_DEVICES_USER, user);
		values.put(DeviceRegistry.FIELD_DEVICES_BRAND, brand);
		values.put(DeviceRegistry.FIELD_DEVICES_MODEL, model);
		values.put(DeviceRegistry.FIELD_DEVICES_ID, deviceId);
		values.put(DeviceRegistry.FIELD_DEVICES_ISRESTRICTED, isRestricted ? 1 : 0);
		values.put(DeviceRegistry.FIELD_DEVICES_ISLOCALADDRESS, isLocalAddress ? 1 : 0);
	}

	public String toString()
	{
		return (this.model != null) ? this.model + " - " + this.ip : this.ip;
	}
}
