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
import android.content.Context;
import androidx.annotation.StringRes;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.migration.db.Migration;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.android.database.*;

import java.util.List;

/**
 * Created by: veli
 * Date: 4/14/17 11:47 PM
 */

public class AccessDatabase extends SQLiteDatabase
{
    /*
     * Database migration is an important step when upgrading to an upper version. The user data
     * is always preserved.
     */

    public static final int DATABASE_VERSION = 13;

    public static final String TAG = AccessDatabase.class.getSimpleName();
    public static final String DATABASE_NAME = AccessDatabase.class.getSimpleName() + ".db";

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
    public static final String FIELD_DEVICES_CLIENTVERSION = "clientVersion";
    public static final String FIELD_DEVICES_LASTUSAGETIME = "lastUsedTime";
    public static final String FIELD_DEVICES_ISRESTRICTED = "isRestricted";
    public static final String FIELD_DEVICES_ISTRUSTED = "isTrusted";
    public static final String FIELD_DEVICES_ISLOCALADDRESS = "isLocalAddress";
    public static final String FIELD_DEVICES_SECUREKEY = "tmpSecureKey";
    // not required for the desktop version
    public static final String FIELD_DEVICES_TYPE = "type";

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
    public static final String FIELD_TRANSFERGROUP_ISPAUSED = "isPaused";

    public static final String TABLE_WRITABLEPATH = "writablePath";
    public static final String FIELD_WRITABLEPATH_TITLE = "title";
    public static final String FIELD_WRITABLEPATH_PATH = "path";

    public AccessDatabase(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(android.database.sqlite.SQLiteDatabase db)
    {
        SQLQuery.createTables(db, tables());
    }

    @Override
    public void onUpgrade(android.database.sqlite.SQLiteDatabase database, int old, int current)
    {
        Migration.migrate(this, database, old, current);
    }

    public static SQLValues tables()
    {
        SQLValues values = new SQLValues();

        values.defineTable(TABLE_CLIPBOARD)
                .define(new SQLValues.Column(FIELD_CLIPBOARD_ID, SQLType.INTEGER, false))
                .define(new SQLValues.Column(FIELD_CLIPBOARD_TEXT, SQLType.TEXT, false))
                .define(new SQLValues.Column(FIELD_CLIPBOARD_TIME, SQLType.LONG, false));

        values.defineTable(TABLE_DEVICES)
                .define(new SQLValues.Column(FIELD_DEVICES_ID, SQLType.TEXT, false))
                .define(new SQLValues.Column(FIELD_DEVICES_USER, SQLType.TEXT, false))
                .define(new SQLValues.Column(FIELD_DEVICES_BRAND, SQLType.TEXT, false))
                .define(new SQLValues.Column(FIELD_DEVICES_MODEL, SQLType.TEXT, false))
                .define(new SQLValues.Column(FIELD_DEVICES_BUILDNAME, SQLType.TEXT, false))
                .define(new SQLValues.Column(FIELD_DEVICES_BUILDNUMBER, SQLType.INTEGER, false))
                .define(new SQLValues.Column(FIELD_DEVICES_CLIENTVERSION, SQLType.INTEGER, false))
                .define(new SQLValues.Column(FIELD_DEVICES_LASTUSAGETIME, SQLType.INTEGER, false))
                .define(new SQLValues.Column(FIELD_DEVICES_ISRESTRICTED, SQLType.INTEGER, false))
                .define(new SQLValues.Column(FIELD_DEVICES_ISTRUSTED, SQLType.INTEGER, false))
                .define(new SQLValues.Column(FIELD_DEVICES_ISLOCALADDRESS, SQLType.INTEGER, false))
                .define(new SQLValues.Column(FIELD_DEVICES_SECUREKEY, SQLType.INTEGER, true))
                .define(new SQLValues.Column(FIELD_DEVICES_TYPE, SQLType.TEXT, false));

        values.defineTable(TABLE_DEVICECONNECTION)
                .define(new SQLValues.Column(FIELD_DEVICECONNECTION_IPADDRESS, SQLType.TEXT, false))
                .define(new SQLValues.Column(FIELD_DEVICECONNECTION_DEVICEID, SQLType.TEXT, false))
                .define(new SQLValues.Column(FIELD_DEVICECONNECTION_ADAPTERNAME, SQLType.TEXT, false))
                .define(new SQLValues.Column(FIELD_DEVICECONNECTION_LASTCHECKEDDATE, SQLType.INTEGER, false));

        values.defineTable(TABLE_FILEBOOKMARK)
                .define(new SQLValues.Column(FIELD_FILEBOOKMARK_TITLE, SQLType.TEXT, false))
                .define(new SQLValues.Column(FIELD_FILEBOOKMARK_PATH, SQLType.TEXT, false));

        values.defineTable(TABLE_TRANSFER)
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

        values.defineTable(TABLE_TRANSFERASSIGNEE)
                .define(new SQLValues.Column(FIELD_TRANSFERASSIGNEE_GROUPID, SQLType.LONG, false))
                .define(new SQLValues.Column(FIELD_TRANSFERASSIGNEE_DEVICEID, SQLType.TEXT, false))
                .define(new SQLValues.Column(FIELD_TRANSFERASSIGNEE_CONNECTIONADAPTER, SQLType.TEXT, false))
                .define(new SQLValues.Column(FIELD_TRANSFERASSIGNEE_TYPE, SQLType.TEXT, false));

        values.defineTable(TABLE_TRANSFERGROUP)
                .define(new SQLValues.Column(FIELD_TRANSFERGROUP_ID, SQLType.LONG, false))
                .define(new SQLValues.Column(FIELD_TRANSFERGROUP_DATECREATED, SQLType.LONG, false))
                .define(new SQLValues.Column(FIELD_TRANSFERGROUP_SAVEPATH, SQLType.TEXT, true))
                .define(new SQLValues.Column(FIELD_TRANSFERGROUP_ISSHAREDONWEB, SQLType.INTEGER, true))
                .define(new SQLValues.Column(FIELD_TRANSFERGROUP_ISPAUSED, SQLType.INTEGER, false));

        values.defineTable(TABLE_WRITABLEPATH)
                .define(new SQLValues.Column(FIELD_WRITABLEPATH_TITLE, SQLType.TEXT, false))
                .define(new SQLValues.Column(FIELD_WRITABLEPATH_PATH, SQLType.TEXT, false));

        return values;
    }

    private void doAsynchronous(Activity activity, @StringRes int textRes, final AsynchronousTask asynchronousTask)
    {
        if (activity == null || activity.isFinishing())
            return;

        new WorkerService.RunningTask<WorkerService.OnAttachListener>()
        {
            @Override
            protected void onRun()
            {
                if (getService() != null)
                    publishStatusText("-");

                asynchronousTask.perform(this);
                broadcast();
            }
        }.setTitle(activity.getString(textRes)).run(activity);
    }

    public void removeAsynchronous(Activity activity, final DatabaseObject object)
    {
        doAsynchronous(activity, R.string.mesg_removing, (task) -> remove(object));
    }

    public void removeAsynchronous(Activity activity, final List<? extends DatabaseObject> objects)
    {
        doAsynchronous(activity, R.string.mesg_removing, (task) -> remove(objects, (total, current) -> {
            task.publishStatusText(getContext().getString(R.string.text_transferStatusFiles, current, total));
            return !task.getInterrupter().interrupted();
        }));
    }

    public interface AsynchronousTask
    {
        void perform(WorkerService.RunningTask task);
    }
}

