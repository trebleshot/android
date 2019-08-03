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

/**
 * created by: veli
 * date: 7/31/19 11:00 AM
 */

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.migration.db.Migration;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

import androidx.annotation.NonNull;


public class TransferObjectV12 implements DatabaseObject<TransferGroupV12>
{
	public String friendlyName;
	public String file;
	public String fileMimeType;
	public String directory;
	public String deviceId;
	public long requestId;
	public long groupId;
	public long skippedBytes;
	public long fileSize = 0;
	public int accessPort;
	public Type type = Type.INCOMING;
	public Flag flag = Flag.PENDING;

	public TransferObjectV12()
	{
	}

	public TransferObjectV12(long requestId, long groupId, String friendlyName, String file, String fileMime, long fileSize, Type type)
	{
		this(requestId, groupId, null, friendlyName, file, fileMime, fileSize, type);
	}

	public TransferObjectV12(long requestId, long groupId, String deviceId, String friendlyName, String file, String fileMime, long fileSize, Type type)
	{
		this.friendlyName = friendlyName;
		this.file = file;
		this.fileSize = fileSize;
		this.fileMimeType = fileMime;
		this.deviceId = deviceId;
		this.requestId = requestId;
		this.groupId = groupId;
		this.type = type;
	}

	public TransferObjectV12(long requestId, String deviceId, Type type)
	{
		this.requestId = requestId;
		this.deviceId = deviceId;
		this.type = type;
	}

	public TransferObjectV12(CursorItem item)
	{
		reconstruct(item);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof TransferObjectV12))
			return super.equals(obj);

		TransferObjectV12 otherObject = (TransferObjectV12) obj;

		return otherObject.requestId == requestId
				&& type.equals(otherObject.type) && ((deviceId == null
				&& otherObject.deviceId == null) || (deviceId != null
				&& deviceId.equals(otherObject.deviceId)));
	}

	public boolean isDivisionObject()
	{
		return deviceId == null;
	}

	@Override
	public SQLQuery.Select getWhere()
	{
		String whereClause = isDivisionObject()
				? String.format("%s = ? AND %s = ?", AccessDatabase.FIELD_TRANSFER_ID, AccessDatabase.FIELD_TRANSFER_TYPE)
				: String.format("%s = ? AND %s = ? AND %s = ?", AccessDatabase.FIELD_TRANSFER_ID,
				AccessDatabase.FIELD_TRANSFER_TYPE, Migration.v12.FIELD_TRANSFER_DEVICEID);

		return isDivisionObject()
				? new SQLQuery.Select(Migration.v12.TABLE_DIVISTRANSFER).setWhere(whereClause, String.valueOf(requestId), type.toString())
				: new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER).setWhere(whereClause, String.valueOf(requestId), type.toString(), deviceId);
	}

	@Override
	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();

		values.put(AccessDatabase.FIELD_TRANSFER_ID, requestId);
		values.put(AccessDatabase.FIELD_TRANSFER_GROUPID, groupId);
		values.put(Migration.v12.FIELD_TRANSFER_DEVICEID, deviceId);
		values.put(AccessDatabase.FIELD_TRANSFER_NAME, friendlyName);
		values.put(AccessDatabase.FIELD_TRANSFER_SIZE, fileSize);
		values.put(AccessDatabase.FIELD_TRANSFER_MIME, fileMimeType);
		values.put(AccessDatabase.FIELD_TRANSFER_FLAG, flag.toString());
		values.put(AccessDatabase.FIELD_TRANSFER_TYPE, type.toString());
		values.put(AccessDatabase.FIELD_TRANSFER_FILE, file);
		values.put(Migration.v12.FIELD_TRANSFER_ACCESSPORT, accessPort);
		values.put(Migration.v12.FIELD_TRANSFER_SKIPPEDBYTES, skippedBytes);
		values.put(AccessDatabase.FIELD_TRANSFER_DIRECTORY, directory);

		return values;
	}

	@Override
	public void reconstruct(CursorItem item)
	{
		this.friendlyName = item.getString(AccessDatabase.FIELD_TRANSFER_NAME);
		this.file = item.getString(AccessDatabase.FIELD_TRANSFER_FILE);
		this.fileSize = item.getLong(AccessDatabase.FIELD_TRANSFER_SIZE);
		this.fileMimeType = item.getString(AccessDatabase.FIELD_TRANSFER_MIME);
		this.requestId = item.getLong(AccessDatabase.FIELD_TRANSFER_ID);
		this.groupId = item.getLong(AccessDatabase.FIELD_TRANSFER_GROUPID);
		this.deviceId = item.getString(Migration.v12.FIELD_TRANSFER_DEVICEID);
		this.type = Type.valueOf(item.getString(AccessDatabase.FIELD_TRANSFER_TYPE));

		// We may have put long in that field indicating that the file was / is in progress so generate
		try {
			this.flag = Flag.valueOf(item.getString(AccessDatabase.FIELD_TRANSFER_FLAG));
		} catch (Exception e) {
			this.flag = Flag.IN_PROGRESS;
			this.flag.setBytesValue(item.getLong(AccessDatabase.FIELD_TRANSFER_FLAG));
		}

		this.accessPort = item.getInt(Migration.v12.FIELD_TRANSFER_ACCESSPORT);
		this.skippedBytes = item.getLong(Migration.v12.FIELD_TRANSFER_SKIPPEDBYTES);
		this.directory = item.getString(AccessDatabase.FIELD_TRANSFER_DIRECTORY);
	}

	@Override
	public void onCreateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, TransferGroupV12 parent)
	{

	}

	@Override
	public void onUpdateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, TransferGroupV12 parent)
	{

	}

	@Override
	public void onRemoveObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, TransferGroupV12 parent)
	{

	}

	public enum Type
	{
		INCOMING,
		OUTGOING
	}

	public enum Flag
	{
		INTERRUPTED,
		PENDING,
		REMOVED,
		IN_PROGRESS,
		DONE;

		private long bytesValue;

		public long getBytesValue()
		{
			return bytesValue;
		}

		public void setBytesValue(long bytesValue)
		{
			this.bytesValue = bytesValue;
		}

		@NonNull
		@Override
		public String toString()
		{
			return getBytesValue() > 0
					? String.valueOf(getBytesValue())
					: super.toString();
		}
	}
}
