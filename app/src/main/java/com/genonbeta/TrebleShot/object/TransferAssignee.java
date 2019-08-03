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
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.android.database.exception.ReconstructionFailedException;

import androidx.annotation.NonNull;

/**
 * created by: veli
 * date: 8/3/19 1:35 PM
 */
public class TransferAssignee implements DatabaseObject<TransferGroup>
{
	public long groupId;
	public String deviceId;
	public String connectionAdapter;
	public TransferObject.Type type;

	public TransferAssignee()
	{

	}

	public TransferAssignee(long groupId, String deviceId, TransferObject.Type type)
	{
		this.groupId = groupId;
		this.deviceId = deviceId;
		this.type = type;
	}

	public TransferAssignee(@NonNull TransferGroup group, @NonNull NetworkDevice device,
					@NonNull TransferObject.Type type)
	{
		this(group.id, device.id, type);
	}

	public TransferAssignee(long groupId, String deviceId, TransferObject.Type type, String connectionAdapter)
	{
		this(groupId, deviceId, type);
		this.connectionAdapter = connectionAdapter;
	}

	public TransferAssignee(@NonNull TransferGroup group, @NonNull NetworkDevice device,
					@NonNull TransferObject.Type type,
					@NonNull DeviceConnection connection)
	{
		this(group.id, device.id, type, connection.adapterName);
	}

	@Override
	public SQLQuery.Select getWhere()
	{
		return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE).setWhere(
				AccessDatabase.FIELD_TRANSFERASSIGNEE_DEVICEID + "=? AND "
						+ AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=? AND "
						+ AccessDatabase.FIELD_TRANSFERASSIGNEE_TYPE + "=?", deviceId,
				String.valueOf(groupId), type.toString());
	}

	@Override
	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();

		values.put(AccessDatabase.FIELD_TRANSFERASSIGNEE_DEVICEID, deviceId);
		values.put(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID, groupId);
		values.put(AccessDatabase.FIELD_TRANSFERASSIGNEE_CONNECTIONADAPTER, connectionAdapter);
		values.put(AccessDatabase.FIELD_TRANSFERASSIGNEE_TYPE, type.toString());

		return values;
	}

	@Override
	public void reconstruct(CursorItem item)
	{
		this.deviceId = item.getString(AccessDatabase.FIELD_TRANSFERASSIGNEE_DEVICEID);
		this.groupId = item.getLong(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID);
		this.connectionAdapter = item.getString(AccessDatabase.FIELD_TRANSFERASSIGNEE_CONNECTIONADAPTER);
		this.type = TransferObject.Type.valueOf(item.getString(AccessDatabase.FIELD_TRANSFERASSIGNEE_TYPE));
	}

	@Override
	public void onCreateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, TransferGroup parent)
	{

	}

	@Override
	public void onUpdateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, TransferGroup parent)
	{

	}

	@Override
	public void onRemoveObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, TransferGroup parent)
	{
		if (!TransferObject.Type.INCOMING.equals(type))
			return;

		try {
			AccessDatabase accessDatabase = AppUtils.getDatabase(database.getContext());

			if (parent == null) {
				parent = new TransferGroup(groupId);
				accessDatabase.reconstruct(parent);
			}

			SQLQuery.Select selection = TransferUtils.createIncomingSelection(groupId,
					TransferObject.Flag.INTERRUPTED, true);

			accessDatabase.removeAsObject(dbInstance, selection, TransferObject.class,
					null, parent);
		} catch (ReconstructionFailedException e) {
			e.printStackTrace();
		}
	}
}