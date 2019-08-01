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

package com.genonbeta.TrebleShot.migration.db.v13;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.migration.db.Migration;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

import androidx.annotation.NonNull;

/**
 * created by: veli
 * date: 7/31/19 11:02 AM
 */
public class TransferGroup implements DatabaseObject<NetworkDevice>
{
	public long groupId;
	public long dateCreated;
	public String savePath;
	public boolean isServedOnWeb;

	public TransferGroup()
	{
	}

	public TransferGroup(long groupId)
	{
		this.groupId = groupId;
	}

	public TransferGroup(CursorItem item)
	{
		reconstruct(item);
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof TransferGroup && ((TransferGroup) obj).groupId == groupId;
	}

	@Override
	public void reconstruct(CursorItem item)
	{
		this.groupId = item.getLong(AccessDatabase.FIELD_TRANSFERGROUP_ID);
		this.savePath = item.getString(AccessDatabase.FIELD_TRANSFERGROUP_SAVEPATH);
		this.dateCreated = item.getLong(AccessDatabase.FIELD_TRANSFERGROUP_DATECREATED);
		this.isServedOnWeb = item.getInt(AccessDatabase.FIELD_TRANSFERGROUP_ISSHAREDONWEB) == 1;
	}

	@Override
	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();

		values.put(AccessDatabase.FIELD_TRANSFERGROUP_ID, groupId);
		values.put(AccessDatabase.FIELD_TRANSFERGROUP_SAVEPATH, savePath);
		values.put(AccessDatabase.FIELD_TRANSFERGROUP_DATECREATED, dateCreated);
		values.put(AccessDatabase.FIELD_TRANSFERGROUP_ISSHAREDONWEB, isServedOnWeb ? 1 : 0);

		return values;
	}

	@Override
	public SQLQuery.Select getWhere()
	{
		return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP)
				.setWhere(AccessDatabase.FIELD_TRANSFERGROUP_ID + "=?", String.valueOf(groupId));
	}

	@Override
	public void onCreateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDevice parent)
	{
		this.dateCreated = System.currentTimeMillis();
	}

	@Override
	public void onUpdateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDevice parent)
	{

	}

	@Override
	public void onRemoveObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDevice parent)
	{
		database.remove(new SQLQuery.Select(Migration.DIVIS_TRANSFER)
				.setWhere(String.format("%s = ?", AccessDatabase.FIELD_TRANSFER_GROUPID), String.valueOf(groupId)));

		database.remove(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
				.setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(groupId)));

		database.removeAsObject(dbInstance, new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
				.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(groupId)), TransferObject.class, null, this);
	}

	public static class Assignee implements DatabaseObject<NetworkDevice>
	{
		public long groupId;
		public String deviceId;
		public String connectionAdapter;

		public Assignee()
		{

		}

		public Assignee(long groupId, String deviceId)
		{
			this.groupId = groupId;
			this.deviceId = deviceId;
		}

		public Assignee(@NonNull TransferGroup group, @NonNull NetworkDevice device)
		{
			this(group.groupId, device.deviceId);
		}

		public Assignee(long groupId, String deviceId, String connectionAdapter)
		{
			this(groupId, deviceId);
			this.connectionAdapter = connectionAdapter;
		}

		public Assignee(@NonNull TransferGroup group, @NonNull NetworkDevice device,
						@NonNull NetworkDevice.Connection connection)
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
			values.put(Migration.FIELD_TRANSFERASSIGNEE_ISCLONE, 1);

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
}