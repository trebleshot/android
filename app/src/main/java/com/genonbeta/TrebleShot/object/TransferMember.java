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

package com.genonbeta.TrebleShot.object;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.util.Transfers;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.KuickDb;
import com.genonbeta.android.database.Progress;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.exception.ReconstructionFailedException;

/**
 * created by: veli
 * date: 8/3/19 1:35 PM
 */
public class TransferMember implements DatabaseObject<Transfer>
{
    public long transferId;
    public String deviceId;
    public TransferItem.Type type;

    public TransferMember()
    {

    }

    public TransferMember(long transferId, String deviceId, TransferItem.Type type)
    {
        this.transferId = transferId;
        this.deviceId = deviceId;
        this.type = type;
    }

    public TransferMember(@NonNull Transfer transfer, @NonNull Device device, @NonNull TransferItem.Type type)
    {
        this(transfer.id, device.uid, type);
    }

    @Override
    public boolean equals(@Nullable Object obj)
    {
        if (obj instanceof TransferMember) {
            TransferMember otherMember = (TransferMember) obj;
            return otherMember.transferId == transferId && deviceId.equals(otherMember.deviceId)
                    && type.equals(otherMember.type);
        }

        return super.equals(obj);
    }

    @Override
    public SQLQuery.Select getWhere()
    {
        return new SQLQuery.Select(Kuick.TABLE_TRANSFERMEMBER).setWhere(
                Kuick.FIELD_TRANSFERMEMBER_DEVICEID + "=? AND "
                        + Kuick.FIELD_TRANSFERMEMBER_TRANSFERID + "=? AND "
                        + Kuick.FIELD_TRANSFERMEMBER_TYPE + "=?", deviceId,
                String.valueOf(transferId), type.toString());
    }

    @Override
    public ContentValues getValues()
    {
        ContentValues values = new ContentValues();

        values.put(Kuick.FIELD_TRANSFERMEMBER_DEVICEID, deviceId);
        values.put(Kuick.FIELD_TRANSFERMEMBER_TRANSFERID, transferId);
        values.put(Kuick.FIELD_TRANSFERMEMBER_TYPE, type.toString());

        return values;
    }

    @Override
    public void reconstruct(SQLiteDatabase db, KuickDb kuick, ContentValues item)
    {
        this.deviceId = item.getAsString(Kuick.FIELD_TRANSFERMEMBER_DEVICEID);
        this.transferId = item.getAsLong(Kuick.FIELD_TRANSFERMEMBER_TRANSFERID);

        // Added in DB version 13 and might be null and may throw an error since ContentValues doesn't like it when
        // when the requested column name doesn't exist or has type different than requested.
        if (item.containsKey(Kuick.FIELD_TRANSFERMEMBER_TYPE))
            this.type = TransferItem.Type.valueOf(item.getAsString(Kuick.FIELD_TRANSFERMEMBER_TYPE));
    }

    @Override
    public void onCreateObject(SQLiteDatabase db, KuickDb kuick, Transfer parent, Progress.Listener listener)
    {

    }

    @Override
    public void onUpdateObject(SQLiteDatabase db, KuickDb kuick, Transfer parent, Progress.Listener listener)
    {

    }

    @Override
    public void onRemoveObject(SQLiteDatabase db, KuickDb kuick, Transfer parent, Progress.Listener listener)
    {
        if (!TransferItem.Type.INCOMING.equals(type))
            return;

        try {
            if (parent == null) {
                parent = new Transfer(transferId);
                kuick.reconstruct(db, parent);
            }

            SQLQuery.Select selection = Transfers.createIncomingSelection(transferId, TransferItem.Flag.INTERRUPTED,
                    true);

            kuick.removeAsObject(db, selection, TransferItem.class, parent, listener, null);
        } catch (ReconstructionFailedException e) {
            e.printStackTrace();
        }
    }
}