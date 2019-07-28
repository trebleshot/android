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

package com.genonbeta.TrebleShot.database;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLType;
import com.genonbeta.android.database.SQLValues;
import com.genonbeta.android.database.SQLiteDatabase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

	public static final int DATABASE_VERSION = 13;

	public static final String TAG = AccessDatabase.class.getSimpleName();
	public static final String DATABASE_NAME = AccessDatabase.class.getSimpleName() + ".db";

	public static final String ACTION_DATABASE_CHANGE = "com.genonbeta.intent.action.DATABASE_CHANGE";
	public static final String EXTRA_BROADCAST_DATA = "extraBroadcastData";
	public static final String TYPE_REMOVE = "typeRemove";
	public static final String TYPE_INSERT = "typeInsert";
	public static final String TYPE_UPDATE = "typeUpdate";

	public static final String TABLE_CLIPBOARD = "clipboard";
	public static final String FIELD_CLIPBOARD_ID = "id";
	public static final String FIELD_CLIPBOARD_TEXT = "text";
	public static final String FIELD_CLIPBOARD_TIME = "time";

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
	// not required for the desktop version
	public static final String FIELD_DEVICES_EXTRA_TYPE = "type";

	public static final String TABLE_DEVICECONNECTION = "deviceConnection";
	public static final String FIELD_DEVICECONNECTION_IPADDRESS = "ipAddress";
	public static final String FIELD_DEVICECONNECTION_DEVICEID = "deviceId";
	public static final String FIELD_DEVICECONNECTION_ADAPTERNAME = "adapterName";
	public static final String FIELD_DEVICECONNECTION_LASTCHECKEDDATE = "lastCheckedDate";

	public static final String TABLE_FILEBOOKMARK = "fileBookmark";
	public static final String FIELD_FILEBOOKMARK_TITLE = "title";
	public static final String FIELD_FILEBOOKMARK_PATH = "path";

	public static final String TABLE_TRANSFERASSIGNEE = "transferAssignee";
	public static final String FIELD_TRANSFERASSIGNEE_GROUPID = "groupId";
	public static final String FIELD_TRANSFERASSIGNEE_DEVICEID = "deviceId";
	public static final String FIELD_TRANSFERASSIGNEE_CONNECTIONADAPTER = "connectionAdapter";
	public static final String FIELD_TRANSFERASSIGNEE_TYPE = "type";

	public static final String TABLE_TRANSFER = "transfer";
	public static final String FIELD_TRANSFER_ID = "id";
	public static final String FIELD_TRANSFER_NAME = "name";
	public static final String FIELD_TRANSFER_SIZE = "size";
	public static final String FIELD_TRANSFER_MIME = "mime";
	public static final String FIELD_TRANSFER_TYPE = "type";
	public static final String FIELD_TRANSFER_GROUPID = "groupId";
	public static final String FIELD_TRANSFER_FILE = "file";
	public static final String FIELD_TRANSFER_DIRECTORY = "directory";
	public static final String FIELD_TRANSFER_LASTCHANGETIME = "lastAccessTime";
	public static final String FIELD_TRANSFER_FLAG = "flag";

	public static final String TABLE_TRANSFERGROUP = "transferGroup";
	public static final String FIELD_TRANSFERGROUP_ID = "id";
	public static final String FIELD_TRANSFERGROUP_SAVEPATH = "savePath";
	public static final String FIELD_TRANSFERGROUP_DATECREATED = "dateCreated";
	public static final String FIELD_TRANSFERGROUP_ISSHAREDONWEB = "isSharedOnWeb";

	public static final String TABLE_WRITABLEPATH = "writablePath";
	public static final String FIELD_WRITABLEPATH_TITLE = "title";
	public static final String FIELD_WRITABLEPATH_PATH = "path";

	private final List<BroadcastData> mBroadcastOverhead = new ArrayList<>();
	private final Handler mMessenger = new Handler(Looper.getMainLooper());
	private boolean mMessageQueued = false;

	public AccessDatabase(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public static CursorItem convertValues(ContentValues values)
	{
		CursorItem cursorItem = new CursorItem();

		for (String key : values.keySet())
			cursorItem.put(key, values.get(key));

		return cursorItem;
	}

	@Override
	public void onCreate(android.database.sqlite.SQLiteDatabase db)
	{
		SQLQuery.createTables(db, getDatabaseTables());
	}

	@Override
	public void onUpgrade(android.database.sqlite.SQLiteDatabase database, int old, int current)
	{
		// Database Migration Rules

		/*
		 * Version 6 was until version 1.2.5.12 and we don't have any new changes compared to version
		 * 6, so we only included version 5 which we did not note the changes
		 */

		SQLValues databaseTables = getDatabaseTables();

		if (old <= 5) {
			for (String tableName : getDatabaseTables().getTables().keySet())
				database.execSQL("DROP TABLE IF EXISTS `" + tableName + "`");

			SQLQuery.createTables(database, databaseTables);
		} else {
			if (old <= 6) {
				SQLValues.Table groupTable = databaseTables.getTables().get(TABLE_TRANSFERGROUP);
				SQLValues.Table devicesTable = databaseTables.getTables().get(TABLE_DEVICES);
				SQLValues.Table targetDevicesTable = databaseTables.getTables().get(TABLE_TRANSFERASSIGNEE);

				database.execSQL(String.format("DROP TABLE IF EXISTS `%s`", groupTable.getName()));
				database.execSQL(String.format("DROP TABLE IF EXISTS `%s`", devicesTable.getName()));

				SQLQuery.createTable(database, groupTable);
				SQLQuery.createTable(database, devicesTable);
				SQLQuery.createTable(database, targetDevicesTable);
			}

			if (old < 7) {
				// Is there any??
			}

			if (old < 10) {
				// With version 9, I added deviceId column to the transfer table
				// With version 10, DIVISION section added for TABLE_TRANSFER and made deviceId nullable
				// to allow users distinguish individual transfer file

				try {
					// TODO: 7/12/19 Database change
                    /*
                    SQLValues.Table tableTransfer = databaseTables.getTables().get(TABLE_TRANSFER);
                    Map<Long, String> mapDist = new ArrayMap<>();
                    List<TransferObject> supportedItems = new ArrayList<>();
                    List<TransferGroup.Assignee> availableAssignees = castQuery(database,
                            new SQLQuery.Select(TABLE_TRANSFERASSIGNEE),
                            TransferGroup.Assignee.class, null);
                    List<TransferObject> availableTransfers = castQuery(database,
                            new SQLQuery.Select(TABLE_TRANSFER), TransferObject.class, null);

                    for (TransferGroup.Assignee assignee : availableAssignees) {
                        if (!mapDist.containsKey(assignee.groupId))
                            mapDist.put(assignee.groupId, assignee.deviceId);
                    }

                    for (TransferObject transferObject : availableTransfers) {
                        transferObject.deviceId = mapDist.get(transferObject.groupId);

                        if (transferObject.deviceId != null)
                            supportedItems.add(transferObject);
                    }

                    database.execSQL(String.format("DROP TABLE IF EXISTS `%s`", tableTransfer
                            .getName()));
                    SQLQuery.createTable(database, tableTransfer);
                    SQLQuery.createTable(database, divisTransfer);
                    insert(database, supportedItems, null, null);
                    */
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (old < 11) {
				SQLValues.Table tableFileBookmark = databaseTables.getTables().get(TABLE_FILEBOOKMARK);
				SQLQuery.createTable(database, tableFileBookmark);
			}

			if (old < 12) {
				List<TransferGroup> totalGroupList = castQuery(database, new SQLQuery.Select(
						TABLE_TRANSFERGROUP), TransferGroup.class, null);
				SQLValues.Table tableTransferGroup = databaseTables.getTables()
						.get(TABLE_TRANSFERGROUP);

				database.execSQL(String.format("DROP TABLE IF EXISTS `%s`", tableTransferGroup
						.getName()));
				SQLQuery.createTable(database, tableTransferGroup);
				insert(database, totalGroupList, null, null);
			}

			if (old < 13) {
				database.execSQL("ALTER TABLE " + TABLE_DEVICES + " ADD "
						+ FIELD_DEVICES_EXTRA_TYPE + " " + SQLType.TEXT.toString()
						+ " NOT NULL DEFAULT " + NetworkDevice.Type.NORMAL.toString());
			}
			// TODO: 7/14/19 Changes: TransferObject {Added LastChangeTime, Changed Flag as Flag[]}, Assignee {Added Type, Removed isClone]
		}
	}

	public synchronized void append(android.database.sqlite.SQLiteDatabase dbInstance,
									String tableName, String changeType)
	{
		BroadcastData data = null;

		synchronized (mBroadcastOverhead) {
			for (BroadcastData testedData : mBroadcastOverhead) {
				if (tableName.equals(testedData.tableName)) {
					data = testedData;
					break;
				}
			}

			if (data == null) {
				data = new BroadcastData(tableName);
				mBroadcastOverhead.add(data);
			}
		}

		switch (changeType) {
			case TYPE_INSERT:
				data.inserted = true;
				break;
			case TYPE_REMOVE:
				data.removed = true;
				break;
			case TYPE_UPDATE:
				data.updated = true;
		}

		data.affectedRowCount += getAffectedRowCount(dbInstance);

		if (!mMessageQueued) {
			mMessageQueued = true;

			Log.d(TAG, "append: Message queued");
			mMessenger.postDelayed(() -> {
				mMessageQueued = false;
				Log.d(TAG, "append: Message executed");
				broadcast();
			}, 5000);
		}
	}

	public synchronized void broadcast()
	{
		synchronized (mBroadcastOverhead) {
			for (BroadcastData data : mBroadcastOverhead) {
				getContext().sendBroadcast(new Intent(ACTION_DATABASE_CHANGE)
						.putExtra(EXTRA_BROADCAST_DATA, data));
			}

			mBroadcastOverhead.clear();
		}
	}

	public long getAffectedRowCount(android.database.sqlite.SQLiteDatabase database)
	{
		Cursor cursor = null;
		long returnCount = 0;

		try {
			cursor = database.rawQuery("SELECT changes() AS affected_row_count", null);

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

		sqlValues.defineTable(TABLE_CLIPBOARD)
				.define(new SQLValues.Column(FIELD_CLIPBOARD_ID, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_CLIPBOARD_TEXT, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_CLIPBOARD_TIME, SQLType.LONG, false));

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
				.define(new SQLValues.Column(FIELD_DEVICES_TMPSECUREKEY, SQLType.INTEGER, true))
				.define(new SQLValues.Column(FIELD_DEVICES_EXTRA_TYPE, SQLType.TEXT, false));

		sqlValues.defineTable(TABLE_DEVICECONNECTION)
				.define(new SQLValues.Column(FIELD_DEVICECONNECTION_IPADDRESS, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICECONNECTION_DEVICEID, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICECONNECTION_ADAPTERNAME, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICECONNECTION_LASTCHECKEDDATE, SQLType.INTEGER, false));

		sqlValues.defineTable(TABLE_FILEBOOKMARK)
				.define(new SQLValues.Column(FIELD_FILEBOOKMARK_TITLE, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_FILEBOOKMARK_PATH, SQLType.TEXT, false));

		sqlValues.defineTable(TABLE_TRANSFER)
				.define(new SQLValues.Column(FIELD_TRANSFER_ID, SQLType.LONG, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_GROUPID, SQLType.LONG, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_DIRECTORY, SQLType.TEXT, true))
				.define(new SQLValues.Column(FIELD_TRANSFER_FILE, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_NAME, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_SIZE, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_MIME, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_TYPE, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_FLAG, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_TRANSFER_LASTCHANGETIME, SQLType.LONG, false));

		sqlValues.defineTable(TABLE_TRANSFERASSIGNEE)
				.define(new SQLValues.Column(FIELD_TRANSFERASSIGNEE_GROUPID, SQLType.LONG, false))
				.define(new SQLValues.Column(FIELD_TRANSFERASSIGNEE_DEVICEID, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_TRANSFERASSIGNEE_CONNECTIONADAPTER, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_TRANSFERASSIGNEE_TYPE, SQLType.TEXT, false));

		sqlValues.defineTable(TABLE_TRANSFERGROUP)
				.define(new SQLValues.Column(FIELD_TRANSFERGROUP_ID, SQLType.LONG, false))
				.define(new SQLValues.Column(FIELD_TRANSFERGROUP_DATECREATED, SQLType.LONG, false))
				.define(new SQLValues.Column(FIELD_TRANSFERGROUP_SAVEPATH, SQLType.TEXT, true))
				.define(new SQLValues.Column(FIELD_TRANSFERGROUP_ISSHAREDONWEB, SQLType.INTEGER, true));

		sqlValues.defineTable(TABLE_WRITABLEPATH)
				.define(new SQLValues.Column(FIELD_WRITABLEPATH_TITLE, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_WRITABLEPATH_PATH, SQLType.TEXT, false));

		return sqlValues;
	}

	@Override
	public long insert(android.database.sqlite.SQLiteDatabase database, String tableName, String nullColumnHack, ContentValues contentValues)
	{
		long returnedItems = super.insert(database, tableName, nullColumnHack, contentValues);
		append(database, tableName, TYPE_INSERT);
		return returnedItems;
	}

	@Override
	public <T, V extends DatabaseObject<T>> void insert(android.database.sqlite.SQLiteDatabase openDatabase, List<V> objects, ProgressUpdater updater, T parent)
	{
		super.insert(openDatabase, objects, updater, parent);
		Set<String> tableList = explodePerTable(objects).keySet();
		for (String tableName : tableList)
			append(openDatabase, tableName, TYPE_INSERT);
	}

	@Override
	public int remove(android.database.sqlite.SQLiteDatabase database, SQLQuery.Select select)
	{
		int returnedItems = super.remove(database, select);
		append(database, select.tableName, TYPE_REMOVE);
		return returnedItems;
	}

	@Override
	public <T, V extends DatabaseObject<T>> void remove(android.database.sqlite.SQLiteDatabase openDatabase, List<V> objects, ProgressUpdater updater, T parent)
	{
		super.remove(openDatabase, objects, updater, parent);
		Set<String> tableList = explodePerTable(objects).keySet();
        for (String tableName : tableList)
            append(openDatabase, tableName, TYPE_REMOVE);
	}

	public void removeAsynchronous(Activity activity, final DatabaseObject object)
	{
		removeAsynchronous(activity, () -> remove(object));
	}

	public void removeAsynchronous(Activity activity, final List<? extends DatabaseObject> objects)
	{
		removeAsynchronous(activity, () -> remove(objects));
	}

	private void removeAsynchronous(Activity activity, final Runnable runnable)
	{
		if (activity == null || activity.isFinishing())
			return;

		new WorkerService.RunningTask()
		{
			@Override
			protected void onRun()
			{
				if (getService() != null)
					publishStatusText("-");

				runnable.run();
				broadcast();
			}
		}.setTitle(activity.getString(R.string.mesg_removing))
				.run(activity);
	}

	@Override
	public int update(android.database.sqlite.SQLiteDatabase database, SQLQuery.Select select, ContentValues values)
	{
		int returnedItems = super.update(database, select, values);
		append(database, select.tableName, TYPE_UPDATE);
		return returnedItems;
	}

	@Override
	public <T, V extends DatabaseObject<T>> void update(android.database.sqlite.SQLiteDatabase openDatabase, List<V> objects, ProgressUpdater updater, T parent)
	{
		super.update(openDatabase, objects, updater, parent);

		Set<String> tableList = explodePerTable(objects).keySet();

        for (String tableName : tableList)
            append(openDatabase, tableName, TYPE_UPDATE);
	}

	public static BroadcastData toData(Intent intent) {
		return (BroadcastData) intent.getSerializableExtra(EXTRA_BROADCAST_DATA);
	}

	public static class BroadcastData implements Serializable
	{
		public int affectedRowCount = 0;
		public boolean inserted = false;
		public boolean removed = false;
		public boolean updated = false;
		public String tableName;

		BroadcastData(String tableName)
		{
			this.tableName = tableName;
		}
	}
}

