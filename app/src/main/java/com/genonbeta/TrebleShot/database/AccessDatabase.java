package com.genonbeta.TrebleShot.database;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

import com.genonbeta.android.database.CursorItem;
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

	public static final String TABLE_TRANSFER = "transfer";
	public static final String FIELD_TRANSFER_ID = "id";
	public static final String FIELD_TRANSFER_FILE = "file";
	public static final String FIELD_TRANSFER_NAME = "name";
	public static final String FIELD_TRANSFER_SIZE = "size";
	public static final String FIELD_TRANSFER_MIME = "mime";
	public static final String FIELD_TRANSFER_TYPE = "type";
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
	public static final String FIELD_DEVICES_BUILDNUMBER = "buildNumber";
	public static final String FIELD_DEVICES_LASTUSAGETIME = "lastUsedTime";
	public static final String FIELD_DEVICES_ISRESTRICTED = "isRestricted";
	public static final String FIELD_DEVICES_ISLOCALADDRESS = "isLocalAddress";

	public static final String TABLE_DEVICECONNECTION = "deviceConnection";
	public static final String FIELD_DEVICECONNECTION_IPADDRESS = "ipAddress";
	public static final String FIELD_DEVICECONNECTION_DEVICEID = "deviceId";
	public static final String FIELD_DEVICECONNECTION_ADAPTERNAME = "adapterName";

	private Context mContext;

	public AccessDatabase(Context context)
	{
		super(context, DATABASE_NAME, null, 3);
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
				.define(new SQLValues.Column(FIELD_DEVICES_BUILDNUMBER, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_DEVICES_LASTUSAGETIME, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_DEVICES_ISRESTRICTED, SQLType.INTEGER, false))
				.define(new SQLValues.Column(FIELD_DEVICES_ISLOCALADDRESS, SQLType.INTEGER, false));

		sqlValues.defineTable(TABLE_DEVICECONNECTION)
				.define(new SQLValues.Column(FIELD_DEVICECONNECTION_IPADDRESS, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICECONNECTION_DEVICEID, SQLType.TEXT, false))
				.define(new SQLValues.Column(FIELD_DEVICECONNECTION_ADAPTERNAME, SQLType.TEXT, false));

		SQLQuery.createTables(db, sqlValues);
	}

	@Override
	public void onUpgrade(android.database.sqlite.SQLiteDatabase db, int old, int current)
	{
		if (old != current) {
			db.execSQL("DROP TABLE `" + TABLE_DEVICES + "`");
			db.execSQL("DROP TABLE `" + TABLE_TRANSFER + "`");

			onCreate(db);
		}
	}

	public <T extends FlexibleObject> ArrayList<T> castQuery(SQLQuery.Select select, final Class<T> clazz)
	{
		ArrayList<T> returnedList = new ArrayList<>();
		ArrayList<CursorItem> itemList = getTable(select);

		try {
			for (CursorItem item : itemList) {
				T newClazz = clazz.cast(clazz.newInstance());

				newClazz.reconstruct(item);
				returnedList.add(newClazz);
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}

		return returnedList;
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

	public void publish(FlexibleObject object)
	{
		if (getFirstFromTable(object.getWhere()) != null) {
			object.onUpdateObject(this);
			update(object.getWhere(), object.getValues());
		} else {
			object.onCreateObject(this);
			getWritableDatabase().insert(object.getWhere().tableName, null, object.getValues());
		}
	}

	public void remove(FlexibleObject object)
	{
		object.onRemoveObject(this);
		delete(object.getWhere());
	}

	public void reconstruct(FlexibleObject object) throws Exception
	{
		CursorItem item = getFirstFromTable(object.getWhere());

		if (item == null)
			throw new Exception("No data was returned from the query");

		object.reconstruct(item);
	}
}
