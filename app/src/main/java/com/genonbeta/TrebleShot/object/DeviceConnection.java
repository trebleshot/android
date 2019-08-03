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

package com.genonbeta.TrebleShot.object;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

/**
 * created by: veli
 * date: 8/3/19 1:22 PM
 */
public class DeviceConnection implements DatabaseObject<NetworkDevice>
{
	public String adapterName;
	public String ipAddress;
	public String deviceId;
	public long lastCheckedDate;

	public DeviceConnection()
	{
	}

	public DeviceConnection(String adapterName, String ipAddress, String deviceId, long lastCheckedDate)
	{
		this.adapterName = adapterName;
		this.ipAddress = ipAddress;
		this.deviceId = deviceId;
		this.lastCheckedDate = lastCheckedDate;
	}

	public DeviceConnection(String deviceId, String adapterName)
	{
		this.deviceId = deviceId;
		this.adapterName = adapterName;
	}

	public DeviceConnection(TransferAssignee assignee)
	{
		this(assignee.deviceId, assignee.connectionAdapter);
	}

	public DeviceConnection(String ipAddress)
	{
		this.ipAddress = ipAddress;
	}

	public DeviceConnection(CursorItem item)
	{
		reconstruct(item);
	}

	@Override
	public SQLQuery.Select getWhere()
	{
		SQLQuery.Select select = new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION);

		return ipAddress == null
				? select.setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=? AND "
				+ AccessDatabase.FIELD_DEVICECONNECTION_ADAPTERNAME + "=?", deviceId, adapterName)
				: select.setWhere(AccessDatabase.FIELD_DEVICECONNECTION_IPADDRESS + "=?", ipAddress);
	}

	@Override
	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();

		values.put(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID, deviceId);
		values.put(AccessDatabase.FIELD_DEVICECONNECTION_ADAPTERNAME, adapterName);
		values.put(AccessDatabase.FIELD_DEVICECONNECTION_IPADDRESS, ipAddress);
		values.put(AccessDatabase.FIELD_DEVICECONNECTION_LASTCHECKEDDATE, lastCheckedDate);

		return values;
	}

	@Override
	public void reconstruct(CursorItem item)
	{
		this.adapterName = item.getString(AccessDatabase.FIELD_DEVICECONNECTION_ADAPTERNAME);
		this.ipAddress = item.getString(AccessDatabase.FIELD_DEVICECONNECTION_IPADDRESS);
		this.deviceId = item.getString(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID);
		this.lastCheckedDate = item.getLong(AccessDatabase.FIELD_DEVICECONNECTION_LASTCHECKEDDATE);
	}

	@Override
	public void onCreateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDevice parent)
	{

	}

	@Override
	public void onUpdateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDevice parent)
	{

	}

	@Override
	public void onRemoveObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDevice parent)
	{

	}
}
