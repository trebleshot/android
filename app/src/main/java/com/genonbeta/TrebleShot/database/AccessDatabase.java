package com.genonbeta.TrebleShot.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;

import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLType;
import com.genonbeta.android.database.SQLValues;
import com.genonbeta.android.database.SQLiteDatabase;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 4/14/17 11:47 PM
 */

public class AccessDatabase extends SQLiteDatabase
{
	public static final String TAG = AccessDatabase.class.getSimpleName();

	public static final String DATABASE_NAME = AccessDatabase.class.getSimpleName() + ".db";

	public static final String ACTION_DATABASE_CHANGE = "com.genonbeta.intent.action.DATABASE_CHANGE";
	public static final String EXTRA_TABLE_NAME = "tableName";
	public static final String EXTRA_AFFECTED_ITEM_COUNT = "affectedItemCount";
	public static final String EXTRA_CHANGE_TYPE = "changeType";
	public static final String TYPE_REMOVE = "typeRemove";
	public static final String TYPE_INSERT = "typeInsert";
	public static final String TYPE_UPDATE = "typeUpdate";

	public static final String TABLE_TRANSFER = "transfer";
	public static final String FIELD_TRANSFER_ID = "id";
	public static final String FIELD_TRANSFER_FILE = "file";
	public static final String FIELD_TRANSFER_NAME = "name";
	public static final String FIELD_TRANSFER_SIZE = "size";
	public static final String FIELD_TRANSFER_MIME = "mime";
	public static final String FIELD_TRANSFER_TYPE = "type";
	public static final String FIELD_TRANSFER_DIRECTORY = "directory";
	public static final String FIELD_TRANSFER_SKIPPEDBYTES = "skippedBytes";
	public static final String FIELD_TRANSFER_GROUPID = "groupId";
	public static final String FIELD_TRANSFER_FLAG = "flag";
	public static final String FIELD_TRANSFER_ACCESSPORT = "accessPort";

	public static final String TABLE_TRANSFERGROUP = "transferGroup";
	public static final String FIELD_TRANSFERGROUP_ID = "id";
	public static final String FIELD_TRANSFERGROUP_DEVICEID = "deviceId";
	public static final String FIELD_TRANSFERGROUP_CONNECTIONADAPTER = "connectionAdapter";
	public static final String FIELD_TRANSFERGROUP_SAVEPATH = "savePath";
	public static final String FIELD_TRANSFERGROUP_DATECREATED = "dateCreated";

	public static final String TABLE_DEVICES = "devices";
	public static final String FIELD_DEVICES_ID = "deviceId";
	public static final String FIELD_DEVICES_USER = "user";
	public static final String FIELD_DEVICES_BRAND = "brand";
	public static final String FIELD_DEVICES_MODEL = "model";
	public static final String FIELD_DEVICES_BUILDNAME = "buildName";
	public static final String FIELD_DEVICES_BUILDNUMBER = "buildNumber";
	public static final String FIELD_DEVICES_LASTUSAGETIME = "lastUsedTime";
	public static final String FIELD_DEVICES_ISRESTRICTED = "isRestricted";
	public static final String FIELD_DEVICES_ISTRUSTED = "isTrusted";
	public static final String FIELD_DEVICES_ISLOCALADDRESS = "isLocalAddress";

	public static final String TABLE_DEVICECONNECTION = "deviceConnection";
	public static final String FIELD_DEVICECONNECTION_IPADDRESS = "ipAddress";
	public static final String FIELD_DEVICECONNECTION_DEVICEID = "deviceId";
	public static final String FIELD_DEVICECONNECTION_ADAPTERNAME = "adapterName";
	public static final String FIELD_DEVICECONNECTION_LASTCHECKEDDATE = "lastCheckedDate";

	public static final String TABLE_CLIPBOARD = "clipboard";
	public static final String FIELD_CLIPBOARD_ID = "id";
	public static final String FIELD_CLIPBOARD_TEXT = "text";
	public static final String FIELD_CLIPBOARD_TIME = "time";

	private Context mContext;

	public AccessDatabase(Context context)
	{
		super(context, DATABASE_NAME, null, 5);
		mContext = context;
	}

	@Override
	public void onCreate(android.database.sqlite.SQLiteDatabase db)
	{
		SQLValues sqlValues = new SQLValues();

		sqlValues.defineTable(TABLE_TRANSFER)
				.define(new SQLValues.Column(FIELD_TRANSFER_ID, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_GROUPID, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_FILE, SQLType.TEXT, true))
				.define(new SQLValues.Column(FIELD_TRANSFER_NAME, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_SIZE, SQLType.INTEGER, true))
				.define(new SQLValues.Column(FIELD_TRANSFER_MIME, SQLType.TEXT, true))
				.define(new SQLValues.Column(FIELD_TRANSFER_TYPE, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_DIRECTORY, SQLType.TEXT, true))
				.define(new SQLValues.Column(FIELD_TRANSFER_ACCESSPORT, SQLType.INTEGER, true))
				.define(new SQLValues.Column(FIELD_TRANSFER_SKIPPEDBYTES, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_FLAG, SQLType.TEXT, true));

		sqlValues.defineTable(TABLE_TRANSFERGROUP)
				.define(new SQLValues.Column(FIELD_TRANSFERGROUP_ID, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_TRANSFERGROUP_DEVICEID, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_TRANSFERGROUP_CONNECTIONADAPTER, SQLType.TEXT, true))
				.define(new SQLValues.Column(FIELD_TRANSFERGROUP_DATECREATED, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_TRANSFERGROUP_SAVEPATH, SQLType.TEXT, true));

		sqlValues.defineTable(TABLE_DEVICES)
				.define(new SQLValues.Column(FIELD_DEVICES_ID, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICES_USER, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICES_BRAND, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICES_MODEL, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICES_BUILDNAME, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICES_BUILDNUMBER, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_DEVICES_LASTUSAGETIME, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_DEVICES_ISRESTRICTED, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_DEVICES_ISTRUSTED, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_DEVICES_ISLOCALADDRESS, SQLType.INTEGER, false));

		sqlValues.defineTable(TABLE_DEVICECONNECTION)
				.define(new SQLValues.Column(FIELD_DEVICECONNECTION_IPADDRESS, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICECONNECTION_DEVICEID, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICECONNECTION_ADAPTERNAME, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICECONNECTION_LASTCHECKEDDATE, SQLType.INTEGER, false));

		sqlValues.defineTable(TABLE_CLIPBOARD)
				.define(new SQLValues.Column(FIELD_CLIPBOARD_ID, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_CLIPBOARD_TEXT, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_CLIPBOARD_TIME, SQLType.LONG, false));

		SQLQuery.createTables(db, sqlValues);
	}

	@Override
	public void onUpgrade(android.database.sqlite.SQLiteDatabase db, int old, int current)
	{
		if (old != current) {
			db.execSQL("DROP TABLE `" + TABLE_TRANSFER + "`");
			db.execSQL("DROP TABLE `" + TABLE_TRANSFERGROUP + "`");
			db.execSQL("DROP TABLE `" + TABLE_DEVICES + "`");
			db.execSQL("DROP TABLE `" + TABLE_DEVICECONNECTION + "`");

			if (current > 5)
				db.execSQL("DROP TABLE `" + TABLE_CLIPBOARD + "`");

			onCreate(db);
		}
	}

	protected void broadcast(SQLQuery.Select select, String type)
	{
		getContext().sendBroadcast(new Intent(ACTION_DATABASE_CHANGE)
				.putExtra(EXTRA_TABLE_NAME, select.tableName)
				.putExtra(EXTRA_CHANGE_TYPE, type)
				.putExtra(EXTRA_AFFECTED_ITEM_COUNT, getAffectedRowCount()));
	}

	public void calculateTransactionSize(int groupId, TransactionObject.Group.Index indexObject)
	{
		indexObject.reset();

		ArrayList<TransactionObject> transactionList = castQuery(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
				.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
								+ AccessDatabase.FIELD_TRANSFER_FLAG + " != ? AND "
								+ AccessDatabase.FIELD_TRANSFER_FLAG + " != ?",
						String.valueOf(groupId),
						TransactionObject.Flag.INTERRUPTED.toString(),
						TransactionObject.Flag.REMOVED.toString()), TransactionObject.class);

		for (TransactionObject transactionObject : transactionList) {
			if (TransactionObject.Type.INCOMING.equals(transactionObject.type)) {
				indexObject.incoming += transactionObject.fileSize;
				indexObject.incomingCount++;
			} else {
				indexObject.outgoing += transactionObject.fileSize;
				indexObject.outgoingCount++;
			}
		}
	}

	@Override
	public int delete(SQLQuery.Select select)
	{
		int returnedItems = super.delete(select);

		broadcast(select, TYPE_REMOVE);

		return returnedItems;
	}

	public long getAffectedRowCount()
	{
		Cursor cursor = null;
		long returnCount = 0;

		try {
			cursor = getReadableDatabase().rawQuery("SELECT changes() AS affected_row_count", null);

			if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst())
				returnCount = cursor.getLong(cursor.getColumnIndex("affected_row_count"));
		} catch (SQLException e) {
			// Handle exception here.
		} finally {
			if (cursor != null)
				cursor.close();
		}

		return returnCount;
	}

	public Context getContext()
	{
		return mContext;
	}

	@Override
	public long insert(String tableName, String nullColumnHack, ContentValues contentValues)
	{
		long returnedItems = super.insert(tableName, nullColumnHack, contentValues);

		broadcast(new SQLQuery.Select(tableName), TYPE_INSERT);

		return returnedItems;
	}

	@Override
	public int update(SQLQuery.Select select, ContentValues values)
	{
		int returnedItems = super.update(select, values);

		broadcast(select, TYPE_UPDATE);

		return returnedItems;
	}
}
