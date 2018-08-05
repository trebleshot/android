package com.genonbeta.TrebleShot.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;

import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
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
	/*
	 * New database versioning notes;
	 * A- Do not remove all the available tables whenever the database is updated
	 * B- Starting with the version 7, it is decided that individual changes to the database are healthier
	 * C- From now on, the changes to the database will be separated to sections, one of which is belonging to
	 * 		below version 6 and the other which is new generation 7
	 */

	public static final int DATABASE_VERSION = 7;

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
	public static final String FIELD_DEVICES_TMPSECUREKEY = "tmpSecureKey";

	public static final String TABLE_DEVICECONNECTION = "deviceConnection";
	public static final String FIELD_DEVICECONNECTION_IPADDRESS = "ipAddress";
	public static final String FIELD_DEVICECONNECTION_DEVICEID = "deviceId";
	public static final String FIELD_DEVICECONNECTION_ADAPTERNAME = "adapterName";
	public static final String FIELD_DEVICECONNECTION_LASTCHECKEDDATE = "lastCheckedDate";

	public static final String TABLE_CLIPBOARD = "clipboard";
	public static final String FIELD_CLIPBOARD_ID = "id";
	public static final String FIELD_CLIPBOARD_TEXT = "text";
	public static final String FIELD_CLIPBOARD_TIME = "time";

	public static final String TABLE_WRITABLEPATH = "writablePath";
	public static final String FIELD_WRITABLEPATH_TITLE = "title";
	public static final String FIELD_WRITABLEPATH_PATH = "path";

	public static final String TABLE_TRANSFERASSIGNEE = "transferAssignee";
	public static final String FIELD_TRANSFERASSIGNEE_GROUPID = "groupId";
	public static final String FIELD_TRANSFERASSIGNEE_DEVICEID = "deviceId";
	public static final String FIELD_TRANSFERASSIGNEE_CONNECTIONADAPTER = "connectionAdapter";
	public static final String FIELD_TRANSFERASSIGNEE_ISCLONE = "isClone";

	public AccessDatabase(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(android.database.sqlite.SQLiteDatabase db)
	{
		SQLQuery.createTables(db, getDatabaseTables());
	}

	@Override
	public void onUpgrade(android.database.sqlite.SQLiteDatabase db, int old, int current)
	{
		/*
		 * Version 6 was until version 1.2.5.12 and we don't have any new changes compared to version
		 * 6, so we only included version 5 which we did not note the changes
		 */

		SQLValues sqlValues = getDatabaseTables();

		if (old <= 5) {
			for (String tableName : getDatabaseTables().getTables().keySet())
				db.execSQL("DROP TABLE IF EXISTS `" + tableName + "`");

			SQLQuery.createTables(db, sqlValues);
		}

		if (old == 6) {
			SQLValues.Table groupTable = sqlValues.getTables().get(TABLE_TRANSFERGROUP);
			SQLValues.Table devicesTable = sqlValues.getTables().get(TABLE_DEVICES);
			SQLValues.Table targetDevicesTable = sqlValues.getTables().get(TABLE_TRANSFERASSIGNEE);

			db.execSQL("DROP TABLE IF EXISTS `" + groupTable.getName() + "`");
			db.execSQL("DROP TABLE IF EXISTS `" + devicesTable.getName() + "`");

			SQLQuery.createTable(db, groupTable);
			SQLQuery.createTable(db, devicesTable);
			SQLQuery.createTable(db, targetDevicesTable);
		}
	}

	protected void broadcast(SQLQuery.Select select, String type)
	{
		getContext().sendBroadcast(new Intent(ACTION_DATABASE_CHANGE)
				.putExtra(EXTRA_TABLE_NAME, select.tableName)
				.putExtra(EXTRA_CHANGE_TYPE, type)
				.putExtra(EXTRA_AFFECTED_ITEM_COUNT, getAffectedRowCount()));
	}

	public void calculateTransactionSize(long groupId, TransferGroup.Index indexObject)
	{
		indexObject.reset();

		ArrayList<TransferObject> transactionList = castQuery(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
				.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
								+ AccessDatabase.FIELD_TRANSFER_FLAG + " != ?",
						String.valueOf(groupId),
						TransferObject.Flag.REMOVED.toString()), TransferObject.class);

		for (TransferObject transferObject : transactionList) {
			if (TransferObject.Type.INCOMING.equals(transferObject.type)) {
				indexObject.incoming += transferObject.fileSize;
				indexObject.incomingCount++;
			} else {
				indexObject.outgoing += transferObject.fileSize;
				indexObject.outgoingCount++;
			}
		}

		indexObject.calculated = true;
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

	public SQLValues getDatabaseTables()
	{
		SQLValues sqlValues = new SQLValues();

		sqlValues.defineTable(TABLE_TRANSFER)
				.define(new SQLValues.Column(FIELD_TRANSFER_ID, SQLType.LONG, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_GROUPID, SQLType.LONG, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_FILE, SQLType.TEXT, true))
				.define(new SQLValues.Column(FIELD_TRANSFER_NAME, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_SIZE, SQLType.INTEGER, true))
				.define(new SQLValues.Column(FIELD_TRANSFER_MIME, SQLType.TEXT, true))
				.define(new SQLValues.Column(FIELD_TRANSFER_TYPE, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_DIRECTORY, SQLType.TEXT, true))
				.define(new SQLValues.Column(FIELD_TRANSFER_ACCESSPORT, SQLType.INTEGER, true))
				.define(new SQLValues.Column(FIELD_TRANSFER_SKIPPEDBYTES, SQLType.LONG, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_FLAG, SQLType.TEXT, true));

		sqlValues.defineTable(TABLE_TRANSFERGROUP)
				.define(new SQLValues.Column(FIELD_TRANSFERGROUP_ID, SQLType.LONG, false))
				.define(new SQLValues.Column(FIELD_TRANSFERGROUP_DATECREATED, SQLType.LONG, false))
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
				.define(new SQLValues.Column(FIELD_DEVICES_ISLOCALADDRESS, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_DEVICES_TMPSECUREKEY, SQLType.INTEGER, true));

		sqlValues.defineTable(TABLE_DEVICECONNECTION)
				.define(new SQLValues.Column(FIELD_DEVICECONNECTION_IPADDRESS, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICECONNECTION_DEVICEID, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICECONNECTION_ADAPTERNAME, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICECONNECTION_LASTCHECKEDDATE, SQLType.INTEGER, false));

		sqlValues.defineTable(TABLE_CLIPBOARD)
				.define(new SQLValues.Column(FIELD_CLIPBOARD_ID, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_CLIPBOARD_TEXT, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_CLIPBOARD_TIME, SQLType.LONG, false));

		sqlValues.defineTable(TABLE_WRITABLEPATH)
				.define(new SQLValues.Column(FIELD_WRITABLEPATH_TITLE, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_WRITABLEPATH_PATH, SQLType.TEXT, false));

		sqlValues.defineTable(TABLE_TRANSFERASSIGNEE)
				.define(new SQLValues.Column(FIELD_TRANSFERASSIGNEE_GROUPID, SQLType.LONG, false))
				.define(new SQLValues.Column(FIELD_TRANSFERASSIGNEE_DEVICEID, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_TRANSFERASSIGNEE_CONNECTIONADAPTER, SQLType.TEXT, true))
				.define(new SQLValues.Column(FIELD_TRANSFERASSIGNEE_ISCLONE, SQLType.INTEGER, true));

		return sqlValues;
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
