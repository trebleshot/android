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

package com.genonbeta.TrebleShot.migration.db;

import android.database.sqlite.SQLiteDatabase;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.migration.db.object.TransferAssigneeV12;
import com.genonbeta.TrebleShot.migration.db.object.TransferObjectV12;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferAssignee;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLType;
import com.genonbeta.android.database.SQLValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.collection.ArrayMap;

import static com.genonbeta.TrebleShot.database.AccessDatabase.FIELD_DEVICES_TYPE;
import static com.genonbeta.TrebleShot.database.AccessDatabase.FIELD_TRANSFERASSIGNEE_CONNECTIONADAPTER;
import static com.genonbeta.TrebleShot.database.AccessDatabase.FIELD_TRANSFERASSIGNEE_DEVICEID;
import static com.genonbeta.TrebleShot.database.AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID;
import static com.genonbeta.TrebleShot.database.AccessDatabase.FIELD_TRANSFERASSIGNEE_TYPE;
import static com.genonbeta.TrebleShot.database.AccessDatabase.FIELD_TRANSFER_DIRECTORY;
import static com.genonbeta.TrebleShot.database.AccessDatabase.FIELD_TRANSFER_FILE;
import static com.genonbeta.TrebleShot.database.AccessDatabase.FIELD_TRANSFER_FLAG;
import static com.genonbeta.TrebleShot.database.AccessDatabase.FIELD_TRANSFER_GROUPID;
import static com.genonbeta.TrebleShot.database.AccessDatabase.FIELD_TRANSFER_ID;
import static com.genonbeta.TrebleShot.database.AccessDatabase.FIELD_TRANSFER_LASTCHANGETIME;
import static com.genonbeta.TrebleShot.database.AccessDatabase.FIELD_TRANSFER_MIME;
import static com.genonbeta.TrebleShot.database.AccessDatabase.FIELD_TRANSFER_NAME;
import static com.genonbeta.TrebleShot.database.AccessDatabase.FIELD_TRANSFER_SIZE;
import static com.genonbeta.TrebleShot.database.AccessDatabase.FIELD_TRANSFER_TYPE;
import static com.genonbeta.TrebleShot.database.AccessDatabase.TABLE_DEVICES;
import static com.genonbeta.TrebleShot.database.AccessDatabase.TABLE_FILEBOOKMARK;
import static com.genonbeta.TrebleShot.database.AccessDatabase.TABLE_TRANSFER;
import static com.genonbeta.TrebleShot.database.AccessDatabase.TABLE_TRANSFERASSIGNEE;
import static com.genonbeta.TrebleShot.database.AccessDatabase.TABLE_TRANSFERGROUP;

/**
 * created by: veli
 * date: 7/31/19 12:02 PM
 */
public class Migration
{
	public interface v12
	{
		String TABLE_DIVISTRANSFER = "divisionTransfer";

		String FIELD_TRANSFERASSIGNEE_ISCLONE = "isClone";

		String FIELD_TRANSFER_DEVICEID = "deviceId";
		String FIELD_TRANSFER_ACCESSPORT = "accessPort";
		String FIELD_TRANSFER_SKIPPEDBYTES = "skippedBytes";

		static SQLValues tables(SQLValues currentValues)
		{
			SQLValues values = new SQLValues();

			values.getTables().putAll(currentValues.getTables());

			values.defineTable(TABLE_TRANSFER)
					.define(new SQLValues.Column(FIELD_TRANSFER_ID, SQLType.LONG, false))
					.define(new SQLValues.Column(FIELD_TRANSFER_GROUPID, SQLType.LONG, false))
					.define(new SQLValues.Column(v12.FIELD_TRANSFER_DEVICEID, SQLType.TEXT, true))
					.define(new SQLValues.Column(FIELD_TRANSFER_FILE, SQLType.TEXT, true))
					.define(new SQLValues.Column(FIELD_TRANSFER_NAME, SQLType.TEXT, false))
					.define(new SQLValues.Column(FIELD_TRANSFER_SIZE, SQLType.INTEGER, true))
					.define(new SQLValues.Column(FIELD_TRANSFER_MIME, SQLType.TEXT, true))
					.define(new SQLValues.Column(FIELD_TRANSFER_TYPE, SQLType.TEXT, false))
					.define(new SQLValues.Column(FIELD_TRANSFER_DIRECTORY, SQLType.TEXT, true))
					.define(new SQLValues.Column(v12.FIELD_TRANSFER_ACCESSPORT, SQLType.INTEGER, true))
					.define(new SQLValues.Column(v12.FIELD_TRANSFER_SKIPPEDBYTES, SQLType.LONG, false))
					.define(new SQLValues.Column(FIELD_TRANSFER_FLAG, SQLType.TEXT, true));

			// define the transfer division table based on the transfer table
			SQLValues.Table transferTable = values.getTables().get(TABLE_TRANSFER);
			SQLValues.Table transDivisionTable = new SQLValues.Table(v12.TABLE_DIVISTRANSFER);
			transDivisionTable.getColumns().putAll(transferTable.getColumns());

			values.defineTable(TABLE_TRANSFERASSIGNEE)
					.define(new SQLValues.Column(FIELD_TRANSFERASSIGNEE_GROUPID, SQLType.LONG, false))
					.define(new SQLValues.Column(FIELD_TRANSFERASSIGNEE_DEVICEID, SQLType.TEXT, false))
					.define(new SQLValues.Column(FIELD_TRANSFERASSIGNEE_CONNECTIONADAPTER, SQLType.TEXT, true))
					.define(new SQLValues.Column(v12.FIELD_TRANSFERASSIGNEE_ISCLONE, SQLType.INTEGER, true));

			return values;
		}
	}

	public static void migrate(AccessDatabase db, SQLiteDatabase instance, int old, int current)
	{
		SQLValues tables = AccessDatabase.tables();
		SQLValues tables12 = v12.tables(tables);

		switch (old) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
				for (String tableName : tables.getTables().keySet())
					instance.execSQL("DROP TABLE IF EXISTS `" + tableName + "`");

				SQLQuery.createTables(instance, tables12);
				break; // Database has already been recreated. No need for fallthrough.
			case 6:
				SQLValues.Table groupTable = tables12.getTable(TABLE_TRANSFERGROUP);
				SQLValues.Table devicesTable = tables12.getTable(TABLE_DEVICES);
				SQLValues.Table targetDevicesTable = tables12.getTable(TABLE_TRANSFERASSIGNEE);

				instance.execSQL(String.format("DROP TABLE IF EXISTS `%s`", groupTable.getName()));
				instance.execSQL(String.format("DROP TABLE IF EXISTS `%s`", devicesTable.getName()));

				SQLQuery.createTable(instance, groupTable);
				SQLQuery.createTable(instance, devicesTable);
				SQLQuery.createTable(instance, targetDevicesTable);
			case 7:
			case 8:
			case 9:
			case 10:
				// With version 9, I added deviceId column to the transfer table
				// With version 10, DIVISION section added for TABLE_TRANSFER and made deviceId nullable
				// to allow users distinguish individual transfer file

				try {
					SQLValues.Table tableTransfer = tables12.getTable(TABLE_TRANSFER);
					SQLValues.Table divisTransfer = tables12.getTable(v12.TABLE_DIVISTRANSFER);
					Map<Long, String> mapDist = new ArrayMap<>();
					List<TransferObjectV12> supportedItems = new ArrayList<>();
					List<TransferAssigneeV12> availableAssignees = db.castQuery(instance,
							new SQLQuery.Select(TABLE_TRANSFERASSIGNEE),
							TransferAssigneeV12.class, null);
					List<TransferObjectV12> availableTransfers = db.castQuery(instance,
							new SQLQuery.Select(TABLE_TRANSFER), TransferObjectV12.class, null);

					for (TransferAssigneeV12 assignee : availableAssignees) {
						if (!mapDist.containsKey(assignee.groupId))
							mapDist.put(assignee.groupId, assignee.deviceId);
					}

					for (TransferObjectV12 transferObject : availableTransfers) {
						transferObject.deviceId = mapDist.get(transferObject.groupId);

						if (transferObject.deviceId != null)
							supportedItems.add(transferObject);
					}

					instance.execSQL(String.format("DROP TABLE IF EXISTS `%s`", tableTransfer
							.getName()));
					SQLQuery.createTable(instance, tableTransfer);
					SQLQuery.createTable(instance, divisTransfer);
					db.insert(instance, supportedItems, null, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			case 11:
				SQLValues.Table tableFileBookmark = tables12.getTable(TABLE_FILEBOOKMARK);
				SQLQuery.createTable(instance, tableFileBookmark);
			case 12:
				List<TransferGroup> totalGroupList = db.castQuery(instance, new SQLQuery.Select(
						TABLE_TRANSFERGROUP), TransferGroup.class, null);
				SQLValues.Table tableTransferGroup = tables12.getTable(TABLE_TRANSFERGROUP);

				instance.execSQL(String.format("DROP TABLE IF EXISTS `%s`", tableTransferGroup
						.getName()));
				SQLQuery.createTable(instance, tableTransferGroup);
				db.insert(instance, totalGroupList, null, null);
			case 13: {
				{
					SQLValues.Table table = tables.getTable(TABLE_DEVICES);
					SQLValues.Column column = table.getColumn(FIELD_DEVICES_TYPE);
					instance.execSQL("ALTER TABLE " + table.getName() + " ADD " + column.getName()
							+ " " + column.getType().toString() + (column.isNullable() ? " NOT" : "")
							+ " NULL DEFAULT " + NetworkDevice.Type.NORMAL.toString());
				}

				{
					SQLValues.Table table = tables.getTable(TABLE_TRANSFER);
					SQLValues.Column column = table.getColumn(FIELD_TRANSFER_LASTCHANGETIME);
					instance.execSQL("ALTER TABLE " + table.getName() + " ADD " + column.getName()
							+ " " + column.getType().toString() + (column.isNullable() ? " NOT" : "")
							+ " NULL DEFAULT 0	");
				}

				{
					SQLValues.Table table = tables.getTable(TABLE_TRANSFERASSIGNEE);
					SQLValues.Column column = table.getColumn(FIELD_TRANSFERASSIGNEE_TYPE);
					instance.execSQL("ALTER TABLE " + table.getName() + " ADD " + column.getName()
							+ " " + column.getType().toString() + (column.isNullable() ? " NOT" : "")
							+ " NULL DEFAULT 0");

					instance.execSQL("ALTER TABLE " + table.getName() + " DELETE " + column.getName()
							+ " " + column.getType().toString() + (column.isNullable() ? " NOT" : "")
							+ " NULL DEFAULT 0");
				}
			}


			// TODO: 7/14/19 Changes: TransferObject {Added LastChangeTime, Changed Flag as Flag[]}, Assignee {Added Type, Removed isClone]
		}
	}

}
