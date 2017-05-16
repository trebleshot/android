package com.genonbeta.TrebleShot.helper;

import com.genonbeta.TrebleShot.database.DeviceRegistry;
import com.genonbeta.android.database.CursorItem;

public class NetworkDevice
{
	public String ip;
	public String brand;
	public String model;
	public String user;
	public boolean isRestricted = false;
	public boolean isLocalAddress = false;

	public NetworkDevice(String ip, String brand, String model, String user)
	{
		this.ip = ip;
		this.brand = brand;
		this.model = model;
		this.user = user;
	}

	public NetworkDevice(String ip)
	{
		this.ip = ip;
	}

	public NetworkDevice(CursorItem item)
	{
		this.ip = item.getString(DeviceRegistry.FIELD_DEVICES_IP);
		this.user = item.getString(DeviceRegistry.FIELD_DEVICES_USER);
		this.brand = item.getString(DeviceRegistry.FIELD_DEVICES_BRAND);
		this.model = item.getString(DeviceRegistry.FIELD_DEVICES_MODEL);
		this.isRestricted = item.getInt(DeviceRegistry.FIELD_DEVICES_ISRESTRICTED) == 1;
		this.isLocalAddress = item.getInt(DeviceRegistry.FIELD_DEVICES_ISLOCALADDRESS) == 1;
	}

	public NetworkDevice()
	{
	}

	public String toString()
	{
		return (this.model != null) ? this.model + " - " + this.ip : this.ip;
	}
}
