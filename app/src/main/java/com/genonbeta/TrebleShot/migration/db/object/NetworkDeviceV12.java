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

package com.genonbeta.TrebleShot.migration.db.object;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.KuickDb;
import com.genonbeta.android.database.Progress;
import com.genonbeta.android.database.SQLQuery;

import java.util.List;

/**
 * created by: veli
 * date: 7/31/19 11:03 AM
 */
public class NetworkDeviceV12 implements DatabaseObject<Object>
{
    public String brand;
    public String model;
    public String nickname;
    public String deviceId;
    public String versionName;
    public int versionNumber;
    public int tmpSecureKey;
    public long lastUsageTime;
    public boolean isTrusted = false;
    public boolean isRestricted = false;
    public boolean isLocalAddress = false;

    public NetworkDeviceV12()
    {
    }

    public NetworkDeviceV12(String deviceId)
    {
        this.deviceId = deviceId;
    }

    public NetworkDeviceV12(ContentValues item)
    {
        reconstruct(item);
    }

    public String generatePictureId()
    {
        return String.format("picture_%s", deviceId);
    }

    @Override
    public SQLQuery.Select getWhere()
    {
        return new SQLQuery.Select(Kuick.TABLE_DEVICES)
                .setWhere(Kuick.FIELD_DEVICES_ID + "=?", deviceId);
    }

    public ContentValues getValues()
    {
        ContentValues values = new ContentValues();

        values.put(Kuick.FIELD_DEVICES_ID, deviceId);
        values.put(Kuick.FIELD_DEVICES_USER, nickname);
        values.put(Kuick.FIELD_DEVICES_BRAND, brand);
        values.put(Kuick.FIELD_DEVICES_MODEL, model);
        values.put(Kuick.FIELD_DEVICES_BUILDNAME, versionName);
        values.put(Kuick.FIELD_DEVICES_BUILDNUMBER, versionNumber);
        values.put(Kuick.FIELD_DEVICES_LASTUSAGETIME, lastUsageTime);
        values.put(Kuick.FIELD_DEVICES_ISRESTRICTED, isRestricted ? 1 : 0);
        values.put(Kuick.FIELD_DEVICES_ISTRUSTED, isTrusted ? 1 : 0);
        values.put(Kuick.FIELD_DEVICES_ISLOCALADDRESS, isLocalAddress ? 1 : 0);
        values.put(Kuick.FIELD_DEVICES_SECUREKEY, tmpSecureKey);

        return values;
    }

    @Override
    public void reconstruct(ContentValues item)
    {
        this.deviceId = item.getAsString(Kuick.FIELD_DEVICES_ID);
        this.nickname = item.getAsString(Kuick.FIELD_DEVICES_USER);
        this.brand = item.getAsString(Kuick.FIELD_DEVICES_BRAND);
        this.model = item.getAsString(Kuick.FIELD_DEVICES_MODEL);
        this.versionName = item.getAsString(Kuick.FIELD_DEVICES_BUILDNAME);
        this.versionNumber = item.getAsInteger(Kuick.FIELD_DEVICES_BUILDNUMBER);
        this.lastUsageTime = item.getAsLong(Kuick.FIELD_DEVICES_LASTUSAGETIME);
        this.isTrusted = item.getAsInteger(Kuick.FIELD_DEVICES_ISTRUSTED) == 1;
        this.isRestricted = item.getAsInteger(Kuick.FIELD_DEVICES_ISRESTRICTED) == 1;
        this.isLocalAddress = item.getAsInteger(Kuick.FIELD_DEVICES_ISLOCALADDRESS) == 1;
        this.tmpSecureKey = item.getAsInteger(Kuick.FIELD_DEVICES_SECUREKEY);
    }

    @Override
    public void onCreateObject(SQLiteDatabase db, KuickDb kuick, Object parent, Progress.Listener listener)
    {

    }

    @Override
    public void onUpdateObject(SQLiteDatabase db, KuickDb kuick, Object parent, Progress.Listener listener)
    {

    }

    @Override
    public void onRemoveObject(SQLiteDatabase db, KuickDb kuick, Object parent, Progress.Listener listener)
    {
        kuick.getContext().deleteFile(generatePictureId());

        kuick.remove(db, new SQLQuery.Select(Kuick.TABLE_DEVICECONNECTION)
                .setWhere(Kuick.FIELD_DEVICECONNECTION_DEVICEID + "=?", deviceId));

        List<TransferAssigneeV12> assignees = kuick.castQuery(db, new SQLQuery.Select(
                Kuick.TABLE_TRANSFERASSIGNEE).setWhere(Kuick.FIELD_TRANSFERASSIGNEE_DEVICEID + "=?",
                deviceId), TransferAssigneeV12.class, null);

        // We are ensuring that the transfer group is still valid for other devices
        for (TransferAssigneeV12 assignee : assignees) {
            kuick.remove(db, assignee, this, listener);

            try {
                TransferGroupV12 transferGroup = new TransferGroupV12(assignee.groupId);
                kuick.reconstruct(db, transferGroup);

                List<TransferAssigneeV12> relatedAssignees = kuick.castQuery(new SQLQuery.Select(
                        Kuick.TABLE_TRANSFERASSIGNEE).setWhere(Kuick.FIELD_TRANSFERASSIGNEE_GROUPID + "=?",
                        String.valueOf(transferGroup.groupId)), TransferAssigneeV12.class);

                if (relatedAssignees.size() == 0)
                    kuick.remove(db, transferGroup, this, listener);
            } catch (Exception ignored) {

            }
        }
    }
}