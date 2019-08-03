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

package com.genonbeta.TrebleShot.migration.db.object;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.migration.db.Migration;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

import androidx.annotation.NonNull;

/**
 * created by: veli
 * date: 8/3/19 1:14 PM
 */
public class TransferAssigneeV12 implements DatabaseObject<NetworkDeviceV12>
{
	public long groupId;
	public String deviceId;
	public String connectionAdapter;

	public TransferAssigneeV12()
	{

	}

	public TransferAssigneeV12(long groupId, String deviceId)
	{
		this.groupId = groupId;
		this.deviceId = deviceId;
	}

	public TransferAssigneeV12(@NonNull TransferGroupV12 group, @NonNull NetworkDeviceV12 device)
	{
		this(group.groupId, device.deviceId);
	}

	public TransferAssigneeV12(long groupId, String deviceId, String connectionAdapter)
	{
		this(groupId, deviceId);
		this.connectionAdapter = connectionAdapter;
	}

	public TransferAssigneeV12(@NonNull TransferGroupV12 group, @NonNull NetworkDeviceV12 device,
							   @NonNull DeviceConnection connection)
	{
		this(group.groupId, device.deviceId, connection.adapterName);
	}

	@Override
	public SQLQuery.Select getWhere()
	{
		return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
				.setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_DEVICEID + "=? AND " + AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", deviceId, String.valueOf(groupId));
	}

	@Override
	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();

		values.put(AccessDatabase.FIELD_TRANSFERASSIGNEE_DEVICEID, deviceId);
		values.put(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID, groupId);
		values.put(AccessDatabase.FIELD_TRANSFERASSIGNEE_CONNECTIONADAPTER, connectionAdapter);
		values.put(Migration.v12.FIELD_TRANSFERASSIGNEE_ISCLONE, 1);

		return values;
	}

	@Override
	public void reconstruct(CursorItem item)
	{
		this.deviceId = item.getString(AccessDatabase.FIELD_TRANSFERASSIGNEE_DEVICEID);
		this.groupId = item.getLong(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID);
		this.connectionAdapter = item.getString(AccessDatabase.FIELD_TRANSFERASSIGNEE_CONNECTIONADAPTER);
	}

	@Override
	public void onCreateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDeviceV12 parent)
	{

	}

	@Override
	public void onUpdateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDeviceV12 parent)
	{

	}

	@Override
	public void onRemoveObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDeviceV12 parent)
	{

	}
}