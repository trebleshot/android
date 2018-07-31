package com.genonbeta.TrebleShot.object;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.android.framework.object.Selectable;

/**
 * created by: veli
 * date: 06.04.2018 09:37
 */
public class TransferGroup implements DatabaseObject, Selectable
{
	public int groupId;
	public String savePath;
	public long dateCreated;

	private boolean mIsSelected = false;

	public TransferGroup()
	{
	}

	public TransferGroup(int groupId)
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
		this.groupId = item.getInt(AccessDatabase.FIELD_TRANSFERGROUP_ID);
		this.savePath = item.getString(AccessDatabase.FIELD_TRANSFERGROUP_SAVEPATH);
		this.dateCreated = item.getLong(AccessDatabase.FIELD_TRANSFERGROUP_DATECREATED);
	}

	@Override
	public boolean isSelectableSelected()
	{
		return mIsSelected;
	}

	@Override
	public String getSelectableTitle()
	{
		return String.valueOf(groupId);
	}

	@Override
	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();

		values.put(AccessDatabase.FIELD_TRANSFERGROUP_ID, groupId);
		values.put(AccessDatabase.FIELD_TRANSFERGROUP_SAVEPATH, savePath);
		values.put(AccessDatabase.FIELD_TRANSFERGROUP_DATECREATED, dateCreated);

		return values;
	}

	@Override
	public SQLQuery.Select getWhere()
	{
		return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP)
				.setWhere(AccessDatabase.FIELD_TRANSFERGROUP_ID + "=?", String.valueOf(groupId));
	}

	@Override
	public boolean setSelectableSelected(boolean selected)
	{
		mIsSelected = selected;
		return true;
	}

	@Override
	public void onCreateObject(SQLiteDatabase database)
	{
		this.dateCreated = System.currentTimeMillis();
	}

	@Override
	public void onUpdateObject(SQLiteDatabase database)
	{

	}

	@Override
	public void onRemoveObject(SQLiteDatabase database)
	{
		database.delete(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
				.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(groupId)));

		database.delete(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
				.setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(groupId)));
	}

	public static class Index
	{
		public boolean calculated = false;
		public long incoming = 0;
		public long outgoing = 0;
		public int incomingCount = 0;
		public int outgoingCount = 0;

		public void reset()
		{
			calculated = false;

			incoming = 0;
			outgoing = 0;
			incomingCount = 0;
			outgoingCount = 0;
		}
	}

	public static class Assignee implements DatabaseObject
	{
		public int groupId;
		public String deviceId;
		public String connectionAdapter;
		public boolean isClone = false;

		public Assignee()
		{

		}

		public Assignee(int groupId, String deviceId)
		{
			this.groupId = groupId;
			this.deviceId = deviceId;
		}

		public Assignee(TransferGroup group, NetworkDevice device)
		{
			this(group.groupId, device.deviceId);
		}

		public Assignee(int groupId, String deviceId, String connectionAdapter)
		{
			this(groupId, deviceId);
			this.connectionAdapter = connectionAdapter;
		}

		public Assignee(TransferGroup group, NetworkDevice device, NetworkDevice.Connection connection)
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
			values.put(AccessDatabase.FIELD_TRANSFERASSIGNEE_ISCLONE, isClone ? 1 : 0);

			return values;
		}

		@Override
		public void reconstruct(CursorItem item)
		{
			this.deviceId = item.getString(AccessDatabase.FIELD_TRANSFERASSIGNEE_DEVICEID);
			this.groupId = item.getInt(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID);
			this.connectionAdapter = item.getString(AccessDatabase.FIELD_TRANSFERASSIGNEE_CONNECTIONADAPTER);
			this.isClone = item.getInt(AccessDatabase.FIELD_TRANSFERASSIGNEE_ISCLONE) != 0;
		}

		@Override
		public void onCreateObject(SQLiteDatabase database)
		{

		}

		@Override
		public void onUpdateObject(SQLiteDatabase database)
		{

		}

		@Override
		public void onRemoveObject(SQLiteDatabase database)
		{

		}
	}
}
