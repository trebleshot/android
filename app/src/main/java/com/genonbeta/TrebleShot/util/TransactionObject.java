package com.genonbeta.TrebleShot.util;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.database.FlexibleObject;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

/**
 * Created by: veli
 * Date: 4/24/17 11:50 PM
 */

public class TransactionObject implements FlexibleObject
{
	public String friendlyName;
	public String file;
	public String fileMimeType;
	public String directory;
	public int requestId;
	public int groupId;
	public int accessPort;
	public int skippedBytes;
	public long fileSize = 0;
	public DynamicNotification notification;
	public Type type = Type.INCOMING;
	public Flag flag = Flag.PENDING;

	public TransactionObject()
	{
	}

	public TransactionObject(int requestId, int groupId, String friendlyName, String file, String fileMime, long fileSize, Type type)
	{
		this.friendlyName = friendlyName;
		this.file = file;
		this.fileSize = fileSize;
		this.fileMimeType = fileMime;
		this.requestId = requestId;
		this.groupId = groupId;
		this.type = type;
	}

	public TransactionObject(int requestId)
	{
		this.requestId = requestId;
	}

	public TransactionObject(CursorItem item)
	{
		reconstruct(item);
	}

	@Override
	public SQLQuery.Select getWhere()
	{
		return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
				.setWhere(AccessDatabase.FIELD_TRANSFER_ID + "=?", String.valueOf(requestId));
	}

	@Override
	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();

		values.put(AccessDatabase.FIELD_TRANSFER_ID, requestId);
		values.put(AccessDatabase.FIELD_TRANSFER_GROUPID, groupId);
		values.put(AccessDatabase.FIELD_TRANSFER_NAME, friendlyName);
		values.put(AccessDatabase.FIELD_TRANSFER_SIZE, fileSize);
		values.put(AccessDatabase.FIELD_TRANSFER_MIME, fileMimeType);
		values.put(AccessDatabase.FIELD_TRANSFER_FLAG, flag.toString());
		values.put(AccessDatabase.FIELD_TRANSFER_TYPE, type.toString());
		values.put(AccessDatabase.FIELD_TRANSFER_FILE, file);
		values.put(AccessDatabase.FIELD_TRANSFER_ACCESSPORT, accessPort);
		values.put(AccessDatabase.FIELD_TRANSFER_SKIPPEDBYTES, skippedBytes);
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
		this.requestId = item.getInt(AccessDatabase.FIELD_TRANSFER_ID);
		this.groupId = item.getInt(AccessDatabase.FIELD_TRANSFER_GROUPID);
		this.type = Type.valueOf(item.getString(AccessDatabase.FIELD_TRANSFER_TYPE));
		this.flag = Flag.valueOf(item.getString(AccessDatabase.FIELD_TRANSFER_FLAG));
		this.accessPort = item.getInt(AccessDatabase.FIELD_TRANSFER_ACCESSPORT);
		this.skippedBytes = item.getInt(AccessDatabase.FIELD_TRANSFER_SKIPPEDBYTES);
		this.directory = item.getString(AccessDatabase.FIELD_TRANSFER_DIRECTORY);
	}

	@Override
	public void onCreateObject(AccessDatabase database)
	{

	}

	@Override
	public void onUpdateObject(AccessDatabase database)
	{

	}

	@Override
	public void onRemoveObject(AccessDatabase database)
	{

	}

	public static class Group implements FlexibleObject
	{
		public int groupId;
		public String deviceId;
		public String connectionAdapter;
		public String savePath;
		public long dateCreated;

		public Group()
		{
		}

		public Group(int groupId, String deviceId, String connectionAdapter)
		{
			this.groupId = groupId;
			this.deviceId = deviceId;
			this.connectionAdapter = connectionAdapter;
		}

		public Group(int groupId)
		{
			this.groupId = groupId;
		}

		public Group(CursorItem item)
		{
			reconstruct(item);
		}

		@Override
		public void reconstruct(CursorItem item)
		{
			this.groupId = item.getInt(AccessDatabase.FIELD_TRANSFERGROUP_ID);
			this.deviceId = item.getString(AccessDatabase.FIELD_TRANSFERGROUP_DEVICEID);
			this.connectionAdapter = item.getString(AccessDatabase.FIELD_TRANSFERGROUP_CONNECTIONADAPTER);
			this.savePath = item.getString(AccessDatabase.FIELD_TRANSFERGROUP_SAVEPATH);
			this.dateCreated = item.getLong(AccessDatabase.FIELD_TRANSFERGROUP_DATECREATED);
		}

		@Override
		public void onCreateObject(AccessDatabase database)
		{
			this.dateCreated = System.currentTimeMillis();
		}

		@Override
		public void onUpdateObject(AccessDatabase database)
		{

		}

		@Override
		public void onRemoveObject(AccessDatabase database)
		{
			database.delete(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
					.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(groupId)));
		}

		@Override
		public SQLQuery.Select getWhere()
		{
			return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP)
					.setWhere(AccessDatabase.FIELD_TRANSFERGROUP_ID + "=?", String.valueOf(groupId));
		}

		@Override
		public ContentValues getValues()
		{
			ContentValues values = new ContentValues();

			values.put(AccessDatabase.FIELD_TRANSFERGROUP_ID, groupId);
			values.put(AccessDatabase.FIELD_TRANSFERGROUP_DEVICEID, deviceId);
			values.put(AccessDatabase.FIELD_TRANSFERGROUP_CONNECTIONADAPTER, connectionAdapter);
			values.put(AccessDatabase.FIELD_TRANSFERGROUP_SAVEPATH, savePath);
			values.put(AccessDatabase.FIELD_TRANSFERGROUP_DATECREATED, dateCreated);

			return values;
		}
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
		RESUME,
		RUNNING,
		REMOVED
	}
}
