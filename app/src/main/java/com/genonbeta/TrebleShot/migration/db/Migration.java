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

import com.genonbeta.TrebleShot.migration.db.v13.TransferObject;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLType;
import com.genonbeta.android.database.SQLValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.collection.ArrayMap;

/**
 * created by: veli
 * date: 7/31/19 12:02 PM
 */
public class Migration
{
	public static final String FIELD_TRANSFERASSIGNEE_ISCLONE = "isClone";
	public static final String DIVIS_TRANSFER = "divisionTransfer";
	public static final String FIELD_TRANSFER_DEVICEID = "deviceId";
	public static final String FIELD_TRANSFER_ACCESSPORT = "accessPort";
	public static final String FIELD_TRANSFER_SKIPPEDBYTES = "skippedBytes";

	public static boolean migrate(SQLiteDatabase database, int old, int current)
	{
		switch (old) {
			case 0, 1, 2, 3, 4, 5:
				for (String tableName : getDatabaseTables().getTables().keySet())
					database.execSQL("DROP TABLE IF EXISTS `" + tableName + "`");

				SQLQuery.createTables(database, databaseTables);
				return true; // Database has already been recreated. No need for fallback.
			case 6:
				SQLValues.Table groupTable = databaseTables.getTables().get(TABLE_TRANSFERGROUP);
				SQLValues.Table devicesTable = databaseTables.getTables().get(TABLE_DEVICES);
				SQLValues.Table targetDevicesTable = databaseTables.getTables().get(TABLE_TRANSFERASSIGNEE);

				database.execSQL(String.format("DROP TABLE IF EXISTS `%s`", groupTable.getName()));
				database.execSQL(String.format("DROP TABLE IF EXISTS `%s`", devicesTable.getName()));

				SQLQuery.createTable(database, groupTable);
				SQLQuery.createTable(database, devicesTable);
				SQLQuery.createTable(database, targetDevicesTable);
			case 7, 8, 9, 10:
				// With version 9, I added deviceId column to the transfer table
				// With version 10, DIVISION section added for TABLE_TRANSFER and made deviceId nullable
				// to allow users distinguish individual transfer file

				try {
					SQLValues.Table tableTransfer = databaseTables.getTables().get(TABLE_TRANSFER);
					SQLValues.Table divisTransfer = databaseTables.getTables().get(DIVIS_TRANSFER);
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
				} catch (Exception e) {
					e.printStackTrace();
				}
			case 11:
				SQLValues.Table tableFileBookmark = databaseTables.getTables().get(TABLE_FILEBOOKMARK);
				SQLQuery.createTable(database, tableFileBookmark);
			case 12:
				List<TransferGroup> totalGroupList = castQuery(database, new SQLQuery.Select(
						TABLE_TRANSFERGROUP), TransferGroup.class, null);
				SQLValues.Table tableTransferGroup = databaseTables.getTables()
						.get(TABLE_TRANSFERGROUP);

				database.execSQL(String.format("DROP TABLE IF EXISTS `%s`", tableTransferGroup
						.getName()));
				SQLQuery.createTable(database, tableTransferGroup);
				insert(database, totalGroupList, null, null);

			case 13:
				database.execSQL("ALTER TABLE " + TABLE_DEVICES + " ADD "
						+ FIELD_DEVICES_EXTRA_TYPE + " " + SQLType.TEXT.toString()
						+ " NOT NULL DEFAULT " + NetworkDevice.Type.NORMAL.toString());

				return true; // always keep at the end of 'switch' statement
		}

		return false;
	}

	public static SQLValues tables()
	{

	}
}
