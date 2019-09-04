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
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

/**
 * created by: veli
 * date: 7/31/19 11:02 AM
 */
public class TransferGroupV12 implements DatabaseObject<NetworkDeviceV12>
{
	public long groupId;
	public long dateCreated;
	public String savePath;
	public boolean isServedOnWeb;

	public TransferGroupV12()
	{
	}

	public TransferGroupV12(long groupId)
	{
		this.groupId = groupId;
	}

	public TransferGroupV12(ContentValues item)
	{
		reconstruct(item);
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof TransferGroupV12 && ((TransferGroupV12) obj).groupId == groupId;
	}

	@Override
	public void reconstruct(ContentValues item)
	{
		this.groupId = item.getAsLong(AccessDatabase.FIELD_TRANSFERGROUP_ID);
		this.savePath = item.getAsString(AccessDatabase.FIELD_TRANSFERGROUP_SAVEPATH);
		this.dateCreated = item.getAsLong(AccessDatabase.FIELD_TRANSFERGROUP_DATECREATED);
		this.isServedOnWeb = item.getAsInteger(AccessDatabase.FIELD_TRANSFERGROUP_ISSHAREDONWEB) == 1;
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
	public void onCreateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDeviceV12 parent)
	{
		this.dateCreated = System.currentTimeMillis();
	}

	@Override
	public void onUpdateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDeviceV12 parent)
	{

	}

	@Override
	public void onRemoveObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDeviceV12 parent)
	{
		database.remove(new SQLQuery.Select(Migration.v12.TABLE_DIVISTRANSFER)
				.setWhere(String.format("%s = ?", AccessDatabase.FIELD_TRANSFER_GROUPID), String.valueOf(groupId)));

		database.remove(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
				.setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(groupId)));

		database.removeAsObject(dbInstance, new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
				.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(groupId)), TransferObjectV12.class, null, this);
	}
}