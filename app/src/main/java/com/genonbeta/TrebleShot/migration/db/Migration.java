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
import androidx.collection.ArrayMap;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.migration.db.object.TransferAssigneeV12;
import com.genonbeta.TrebleShot.migration.db.object.TransferObjectV12;
import com.genonbeta.TrebleShot.migration.db.object.WritablePathObjectV12;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.TransferAssignee;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLType;
import com.genonbeta.android.database.SQLValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.genonbeta.TrebleShot.database.Kuick.*;

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

        String TABLE_WRITABLEPATH = "writablePath";
        String FIELD_WRITABLEPATH_TITLE = "title";
        String FIELD_WRITABLEPATH_PATH = "path";

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

    public static void migrate(Kuick kuick, SQLiteDatabase db, int old, int current)
    {
        SQLValues tables = Kuick.tables();
        SQLValues tables12 = v12.tables(tables);

        switch (old) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                for (String tableName : tables.getTables().keySet())
                    db.execSQL("DROP TABLE IF EXISTS `" + tableName + "`");

                SQLQuery.createTables(db, tables12);
                break; // Database has already been recreated. No need for fallthrough.
            case 6:
                SQLValues.Table groupTable = tables12.getTable(TABLE_TRANSFERGROUP);
                SQLValues.Table devicesTable = tables12.getTable(TABLE_DEVICES);
                SQLValues.Table targetDevicesTable = tables12.getTable(TABLE_TRANSFERASSIGNEE);

                db.execSQL("DROP TABLE IF EXISTS `" + groupTable.getName() + "`");
                db.execSQL("DROP TABLE IF EXISTS `" + devicesTable.getName() + "`");

                SQLQuery.createTable(db, groupTable);
                SQLQuery.createTable(db, devicesTable);
                SQLQuery.createTable(db, targetDevicesTable);
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
                    List<TransferAssigneeV12> availableAssignees = kuick.castQuery(db,
                            new SQLQuery.Select(TABLE_TRANSFERASSIGNEE),
                            TransferAssigneeV12.class, null);
                    List<TransferObjectV12> availableTransfers = kuick.castQuery(db,
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

                    db.execSQL("DROP TABLE IF EXISTS `" + tableTransfer.getName() + "`");
                    SQLQuery.createTable(db, tableTransfer);
                    SQLQuery.createTable(db, divisTransfer);
                    kuick.insert(db, supportedItems, null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            case 11:
                SQLValues.Table tableFileBookmark = tables12.getTable(TABLE_FILEBOOKMARK);
                SQLQuery.createTable(db, tableFileBookmark);
            case 12:
                List<TransferGroup> totalGroupList = kuick.castQuery(db, new SQLQuery.Select(
                        TABLE_TRANSFERGROUP), TransferGroup.class, null);
                SQLValues.Table tableTransferGroup = tables12.getTable(TABLE_TRANSFERGROUP);

                db.execSQL("DROP TABLE IF EXISTS `" + tableTransferGroup.getName() + "`");
                SQLQuery.createTable(db, tableTransferGroup);
                kuick.insert(db, totalGroupList, null, null);
            case 13: {
                {
                    SQLValues.Table table = tables.getTable(TABLE_DEVICES);
                    SQLValues.Column typeColumn = table.getColumn(FIELD_DEVICES_TYPE);
                    SQLValues.Column clientVerCol = table.getColumn(FIELD_DEVICES_CLIENTVERSION);

                    // Added: Type
                    db.execSQL("ALTER TABLE " + table.getName() + " ADD " + typeColumn.getName()
                            + " " + typeColumn.getType().toString() + (typeColumn.isNullable() ? " NOT" : "")
                            + " NULL DEFAULT " + Device.Type.NORMAL.toString());

                    // Added: ClientVersion
                    db.execSQL("ALTER TABLE " + table.getName() + " ADD " + clientVerCol.getName()
                            + " " + clientVerCol.getType().toString() + (clientVerCol.isNullable() ? " NOT" : "")
                            + " NULL DEFAULT 0");
                }

                {
                    List<TransferAssigneeV12> oldList = kuick.castQuery(db, new SQLQuery.Select(
                            TABLE_TRANSFERASSIGNEE), TransferAssigneeV12.class, null);

                    // Added: Type, Removed: IsClone
                    db.execSQL("DROP TABLE IF EXISTS `" + TABLE_TRANSFERASSIGNEE + "`");
                    SQLQuery.createTable(db, tables.getTable(TABLE_TRANSFERASSIGNEE));

                    List<TransferAssignee> newAssignees = new ArrayList<>();

                    // The `transfer` table will be removed below. We can use the old versions
                    // columns still.
                    for (TransferAssigneeV12 assigneeV12 : oldList) {
                        SQLQuery.Select selection = new SQLQuery.Select(TABLE_TRANSFER);
                        selection.setWhere(FIELD_TRANSFER_TYPE + "=? AND "
                                + FIELD_TRANSFER_GROUPID + "=? AND " + v12.FIELD_TRANSFER_DEVICEID
                                + "=?", TransferObjectV12.Type.INCOMING.toString(), String.valueOf(
                                assigneeV12.groupId), assigneeV12.deviceId);

                        if (kuick.getFirstFromTable(db, selection) != null) {
                            TransferAssignee incomingAssignee = new TransferAssignee();
                            incomingAssignee.reconstruct(db, kuick, assigneeV12.getValues());
                            incomingAssignee.type = TransferObject.Type.INCOMING;
                            newAssignees.add(incomingAssignee);
                        }

                        selection.setWhere(FIELD_TRANSFER_TYPE + "=? AND "
                                + FIELD_TRANSFER_GROUPID + "=? AND " + v12.FIELD_TRANSFER_DEVICEID
                                + "=?", TransferObjectV12.Type.OUTGOING.toString(), String.valueOf(
                                assigneeV12.groupId), assigneeV12.deviceId);

                        if (kuick.getFirstFromTable(db, selection) != null) {
                            TransferAssignee outgoingAssignee = new TransferAssignee();
                            outgoingAssignee.reconstruct(db, kuick, assigneeV12.getValues());
                            outgoingAssignee.type = TransferObject.Type.OUTGOING;
                            newAssignees.add(outgoingAssignee);
                        }
                    }

                    if (newAssignees.size() > 0)
                        kuick.insert(db, newAssignees, null, null);
                }

                {
                    SQLValues.Table table = tables.getTable(TABLE_TRANSFER);

                    // Changed Flag as Flag[] for Type.OUTGOING objects
                    List<TransferObjectV12> outgoingBaseObjects = kuick.castQuery(db, new SQLQuery.Select(
                            v12.TABLE_DIVISTRANSFER), TransferObjectV12.class, null);

                    List<TransferObjectV12> outgoingMirrorObjects = kuick.castQuery(db, new SQLQuery.Select(
                            TABLE_TRANSFER).setWhere(FIELD_TRANSFER_TYPE + "=?",
                            TransferObjectV12.Type.OUTGOING.toString()), TransferObjectV12.class, null);

                    List<TransferObjectV12> incomingObjects = kuick.castQuery(db, new SQLQuery.Select(
                            TABLE_TRANSFER).setWhere(FIELD_TRANSFER_TYPE + "=?",
                            TransferObjectV12.Type.INCOMING.toString()), TransferObjectV12.class, null);

                    // Remove: Table `divisTransfer`
                    db.execSQL("DROP TABLE IF EXISTS `" + v12.TABLE_DIVISTRANSFER + "`");

                    // Added: LastChangeTime, Removed: AccessPort, SkippedBytes
                    db.execSQL("DROP TABLE IF EXISTS `" + TABLE_TRANSFER + "`");
                    SQLQuery.createTable(db, table);

                    if (outgoingBaseObjects.size() > 0) {
                        Map<Long, TransferObject> newObjects = new ArrayMap<>();

                        for (TransferObjectV12 objectV12 : outgoingBaseObjects) {
                            TransferObject object = newObjects.get(objectV12.requestId);

                            if (object != null)
                                continue;

                            object = new TransferObject();
                            object.reconstruct(db, kuick, objectV12.getValues());

                            newObjects.put(objectV12.requestId, object);
                        }

                        for (TransferObjectV12 objectV12 : outgoingMirrorObjects) {
                            TransferObject object = newObjects.get(objectV12.requestId);

                            if (object == null)
                                continue;

                            try {
                                object.putFlag(objectV12.deviceId, TransferObject.Flag.valueOf(
                                        objectV12.flag.toString()));
                            } catch (Exception ignored) {
                            }
                        }

                        if (newObjects.size() > 0)
                            kuick.insert(db, new ArrayList<>(newObjects.values()), null, null);
                    }

                    if (incomingObjects.size() > 0) {
                        List<TransferObject> newIncomingInstances = new ArrayList<>();

                        for (TransferObjectV12 objectV12 : incomingObjects) {
                            TransferObject newObject = new TransferObject();
                            newObject.reconstruct(db, kuick, objectV12.getValues());
                        }

                        kuick.insert(db, newIncomingInstances, null, null);
                    }
                }

                {
                    SQLValues.Table table = tables.getTable(TABLE_TRANSFERGROUP);
                    SQLValues.Column column = table.getColumn(FIELD_TRANSFERGROUP_ISPAUSED);

                    // Added: IsPaused
                    db.execSQL("ALTER TABLE " + table.getName() + " ADD " + column.getName()
                            + " " + column.getType().toString() + (column.isNullable() ? " NOT" : "")
                            + " NULL DEFAULT " + Device.Type.NORMAL.toString());
                }

                {
                    // Writable path and bookmark objects have been dropped.
                    // The remaining table (of bookmarks) is used to store both, while the object
                    // driving them is reduced to FileHolder.
                    List<WritablePathObjectV12> pathObjectList = kuick.castQuery(db, new SQLQuery.Select(
                            v12.TABLE_WRITABLEPATH), WritablePathObjectV12.class, null);

                    if (pathObjectList.size() > 0) {
                        List<FileListAdapter.FileHolder> fileHolderList = new ArrayList<>();
                        for (WritablePathObjectV12 pathObject : pathObjectList) {
                            FileListAdapter.FileHolder fileHolder = new FileListAdapter.FileHolder();
                            fileHolder.reconstruct(db, kuick, pathObject.getValues());
                            fileHolderList.add(fileHolder);
                        }

                        kuick.insert(db, fileHolderList, null, null);
                    }

                    db.execSQL("DROP TABLE IF EXISTS `" + v12.TABLE_WRITABLEPATH + "`");
                }
            }
        }
    }
}
