package com.genonbeta.TrebleShot.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.core.util.NetworkUtils;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/16/17 2:15 PM
 */

public class DeviceRegistry extends MainDatabase
{
	public static final String ACTION_DEVICE_UPDATED = "com.genonbeta.TrebleShot.intent.action.DEVICE_UPDATED";
	public static final String ACTION_DEVICE_REMOVED = "com.genonbeta.TrebleShot.intent.action.DEVICE_REMOVED";

	public static final String FIELD_DEVICES_AVAILABLE_CONNECTIONS = "availableConnections";

	public SQLQuery.Select.LoadListener mLoadListenerAvailableConnections = new SQLQuery.Select.LoadListener()
	{
		@Override
		public void onOpen(SQLiteDatabase db, Cursor cursor)
		{

		}

		@Override
		public void onLoad(SQLiteDatabase db, Cursor cursor, CursorItem item)
		{
			StringBuilder builder = new StringBuilder();

			for (CursorItem deviceItem : getTable(new SQLQuery.Select(TABLE_DEVICES)
					.setWhere(FIELD_DEVICES_ID + "=?", item.getString(FIELD_DEVICES_ID))))
			{
				if (builder.length() > 0)
					builder.append(":");

				builder.append(deviceItem.getString(FIELD_DEVICES_IP));
			}

			item.put(FIELD_DEVICES_AVAILABLE_CONNECTIONS, builder.toString());
		}
	};

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
		final ArrayList<NetworkDevice> deviceList = new ArrayList<>();
		ArrayList<CursorItem> tableIndex = getTable(new SQLQuery.Select(TABLE_DEVICES)
				.setGroupBy(FIELD_DEVICES_ID)
				.setLoadListener(mLoadListenerAvailableConnections));

		for (CursorItem item : tableIndex)
			deviceList.add(new NetworkDevice(item));

		return deviceList;
	}

	public NetworkDevice getNetworkDeviceById(String deviceId)
	{
		CursorItem item = getFirstFromTable(new SQLQuery.Select(TABLE_DEVICES)
				.setWhere(FIELD_DEVICES_ID + "=?", deviceId)
				.setLoadListener(mLoadListenerAvailableConnections));

		if (item != null)
			return new NetworkDevice(item);

		return null;
	}

	public NetworkDevice getNetworkDevice(String ipAddress)
	{
		CursorItem item = getFirstFromTable(new SQLQuery.Select(TABLE_DEVICES)
				.setWhere(FIELD_DEVICES_IP + "=?", ipAddress)
				.setLoadListener(mLoadListenerAvailableConnections));

		if (item != null)
			return new NetworkDevice(item);

		return new NetworkDevice(ipAddress);
	}

	public long notifyRemoved()
	{
		getContext().sendBroadcast(new Intent(ACTION_DEVICE_REMOVED));
		return getAffectedRowCount();
	}

	public long notifyUpdated()
	{
		getContext().sendBroadcast(new Intent(ACTION_DEVICE_UPDATED));
		return getAffectedRowCount();
	}

	public boolean registerDevice(NetworkDevice device)
	{
		removeDevice(device);
		getWritableDatabase().insert(TABLE_DEVICES, null, device.getValues());

		return notifyUpdated() > 0;
	}

	public boolean removeDeviceWithInstances(NetworkDevice device)
	{
		return removeDeviceWithInstances(device.deviceId);
	}

	public boolean removeDeviceWithInstances(String deviceId)
	{
		getWritableDatabase().delete(TABLE_DEVICES, FIELD_DEVICES_ID + "=?", new String[]{deviceId});
		return notifyRemoved() > 0;
	}

	public boolean removeDevice(NetworkDevice device)
	{
		return removeDevice(device.ip);
	}

	public boolean removeDevice(String ipAddress)
	{
		getWritableDatabase().delete(TABLE_DEVICES, FIELD_DEVICES_IP + "=?", new String[]{ipAddress});
		return notifyRemoved() > 0;
	}

	public boolean updateRestriction(NetworkDevice device, boolean restrict)
	{
		device.isRestricted = restrict;
		return updateRestriction(device.ip, restrict);
	}

	public boolean updateRestriction(String ipAddress, boolean restrict)
	{
		ContentValues values = new ContentValues();
		values.put(FIELD_DEVICES_ISRESTRICTED, restrict ? 1 : 0);

		getWritableDatabase().update(TABLE_DEVICES, values, FIELD_DEVICES_IP + "=?", new String[]{ipAddress});

		return notifyUpdated() > 0;
	}
}