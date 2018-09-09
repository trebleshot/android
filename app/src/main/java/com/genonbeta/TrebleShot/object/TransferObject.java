package com.genonbeta.TrebleShot.object;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Created by: veli
 * Date: 4/24/17 11:50 PM
 */

public class TransferObject
		implements DatabaseObject, Editable
{
	public String friendlyName;
	public String file;
	public String fileMimeType;
	public String directory;
	public long requestId;
	public long groupId;
	public long skippedBytes;
	public long fileSize = 0;
	public int accessPort;
	public Type type = Type.INCOMING;
	public Flag flag = Flag.PENDING;

	private boolean mIsSelected = false;

	public TransferObject()
	{
	}

	public TransferObject(long requestId, long groupId, String friendlyName, String file, String fileMime, long fileSize, Type type)
	{
		this.friendlyName = friendlyName;
		this.file = file;
		this.fileSize = fileSize;
		this.fileMimeType = fileMime;
		this.requestId = requestId;
		this.groupId = groupId;
		this.type = type;
	}

	public TransferObject(long requestId)
	{
		this.requestId = requestId;
	}

	public TransferObject(CursorItem item)
	{
		reconstruct(item);
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof TransferObject && ((TransferObject) obj).requestId == requestId;
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
		this.requestId = item.getLong(AccessDatabase.FIELD_TRANSFER_ID);
		this.groupId = item.getLong(AccessDatabase.FIELD_TRANSFER_GROUPID);
		this.type = Type.valueOf(item.getString(AccessDatabase.FIELD_TRANSFER_TYPE));

		// We may  have put long in that field indicating that the file was / is progress so generate
		try {
			this.flag = Flag.valueOf(item.getString(AccessDatabase.FIELD_TRANSFER_FLAG));
		} catch (Exception e) {
			this.flag = Flag.IN_PROGRESS;
			this.flag.setBytesValue(item.getLong(AccessDatabase.FIELD_TRANSFER_FLAG));
		}

		this.accessPort = item.getInt(AccessDatabase.FIELD_TRANSFER_ACCESSPORT);
		this.skippedBytes = item.getLong(AccessDatabase.FIELD_TRANSFER_SKIPPEDBYTES);
		this.directory = item.getString(AccessDatabase.FIELD_TRANSFER_DIRECTORY);
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

	@Override
	public String getComparableName()
	{
		return getSelectableTitle();
	}

	@Override
	public long getComparableDate()
	{
		return requestId;
	}

	@Override
	public long getComparableSize()
	{
		return fileSize;
	}

	@Override
	public long getId()
	{
		return (requestId * 10) + type.ordinal();
	}

	@Override
	public String getSelectableTitle()
	{
		return friendlyName;
	}

	@Override
	public boolean isSelectableSelected()
	{
		return mIsSelected;
	}

	@Override
	public void setId(long id)
	{
		// it will && should be effective on representative text items
		this.requestId = id;
	}

	@Override
	public boolean setSelectableSelected(boolean selected)
	{
		mIsSelected = selected;
		return true;
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

		@Override
		public String toString()
		{
			return getBytesValue() > 0
					? String.valueOf(getBytesValue())
					: super.toString();
		}
	}

	@Retention(CLASS)
	@Target({METHOD, PARAMETER, FIELD, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE})
	public @interface Virtual
	{
	}
}
