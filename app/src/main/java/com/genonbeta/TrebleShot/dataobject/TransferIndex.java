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

package com.genonbeta.TrebleShot.dataobject;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.TransferListAdapter;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.KuickDb;
import com.genonbeta.android.database.Progress;
import com.genonbeta.android.database.SQLQuery;

/**
 * created by: veli
 * date: 7/24/19 6:08 PM
 */
public final class TransferIndex implements GroupEditableListAdapter.GroupEditable, DatabaseObject<Device>
{
    public static final String TAG = TransferIndex.class.getSimpleName();

    public int viewType;
    public String representativeText;

    public int numberOfOutgoing;
    public int numberOfIncoming;
    public int numberOfOutgoingCompleted;
    public int numberOfIncomingCompleted;
    public long bytesOutgoing;
    public long bytesIncoming;
    public long bytesOutgoingCompleted;
    public long bytesIncomingCompleted;
    public boolean isRunning;
    public boolean hasIssues;
    public Transfer transfer = new Transfer();
    public LoadedMember[] members = new LoadedMember[0];

    private boolean mIsSelected = false;

    public TransferIndex()
    {
        transfer = new Transfer();
    }

    public TransferIndex(Transfer transfer)
    {
        this.transfer = transfer;
    }

    public TransferIndex(String representativeText)
    {
        this.viewType = TransferListAdapter.VIEW_TYPE_REPRESENTATIVE;
        this.representativeText = representativeText;
    }

    @Override
    public boolean applyFilter(String[] filteringKeywords)
    {
        LoadedMember[] copyMembers = members;

        for (String keyword : filteringKeywords)
            for (LoadedMember member : copyMembers)
                if (member.device.username.toLowerCase().contains(keyword.toLowerCase()))
                    return true;

        return false;
    }

    public long bytesTotal()
    {
        return bytesOutgoing + bytesIncoming;
    }

    public long bytesCompleted()
    {
        return bytesOutgoingCompleted + bytesIncomingCompleted;
    }

    public long bytesPending()
    {
        return bytesTotal() - bytesCompleted();
    }

    @Override
    public boolean comparisonSupported()
    {
        return true;
    }

    @Override
    public boolean equals(@Nullable Object obj)
    {
        if (obj instanceof TransferIndex)
            return transfer.equals(((TransferIndex) obj).transfer);
        return super.equals(obj);
    }

    public boolean hasIncoming()
    {
        return numberOfIncoming > 0;
    }

    public boolean hasOutgoing()
    {
        return numberOfOutgoing > 0;
    }

    public String getMemberAsTitle()
    {
        LoadedMember[] copyMembers = members;
        StringBuilder title = new StringBuilder();

        for (LoadedMember member : copyMembers) {
            if (title.length() > 0)
                title.append(", ");
            title.append(member.device.username);
        }

        return title.toString();
    }

    public String getMemberAsTitle(Context context)
    {
        LoadedMember[] copyMembers = members;

        if (copyMembers.length == 1)
            return copyMembers[0].device.username;

        return context.getResources().getQuantityString(R.plurals.text_devices,
                copyMembers.length, copyMembers.length);
    }

    @Override
    public String getComparableName()
    {
        return getSelectableTitle();
    }

    @Override
    public long getComparableDate()
    {
        return transfer.dateCreated;
    }

    @Override
    public long getComparableSize()
    {
        return bytesTotal();
    }

    @Override
    public long getId()
    {
        return transfer.id;
    }

    @Override
    public void setId(long id)
    {
        transfer.id = id;
    }

    @Override
    public String getSelectableTitle()
    {
        String title = getMemberAsTitle();
        String size = FileUtils.sizeExpression(bytesOutgoing + bytesOutgoing, false);

        return title.length() > 0 ? String.format("%s (%s)", title, size) : size;
    }

    @Override
    public boolean isSelectableSelected()
    {
        return mIsSelected;
    }

    @Override
    public int getRequestCode()
    {
        return 0;
    }

    @Override
    public int getViewType()
    {
        return viewType;
    }

    public int numberOfTotal()
    {
        return numberOfOutgoing + numberOfIncoming;
    }

    public int numberOfCompleted()
    {
        return numberOfOutgoingCompleted + numberOfIncomingCompleted;
    }

    public double percentage()
    {
        long total = bytesTotal();
        long completed = bytesCompleted();
        return total == 0 ? 1 : (completed == 0 ? 0 : (double) completed / total);
    }

    @Override
    public String getRepresentativeText()
    {
        return representativeText;
    }

    @Override
    public void setRepresentativeText(CharSequence text)
    {
        this.representativeText = String.valueOf(text);
    }

    @Override
    public boolean isGroupRepresentative()
    {
        return representativeText != null;
    }

    @Override
    public void setDate(long date)
    {
        transfer.dateCreated = date;
    }

    @Override
    public boolean setSelectableSelected(boolean selected)
    {
        if (isGroupRepresentative())
            return false;
        mIsSelected = selected;
        return true;
    }

    @Override
    public void setSize(long size)
    {
        Log.e(TAG, "setSize: This is not implemented");
    }

    @Override
    public void onCreateObject(SQLiteDatabase db, KuickDb kuick, Device parent, Progress.Listener listener)
    {
        transfer.onCreateObject(db, kuick, parent, listener);
    }

    @Override
    public void onUpdateObject(SQLiteDatabase db, KuickDb kuick, Device parent, Progress.Listener listener)
    {
        transfer.onUpdateObject(db, kuick, parent, listener);
    }

    @Override
    public void onRemoveObject(SQLiteDatabase db, KuickDb kuick, Device parent, Progress.Listener listener)
    {
        transfer.onRemoveObject(db, kuick, parent, listener);
    }

    @Override
    public ContentValues getValues()
    {
        return transfer.getValues();
    }

    @Override
    public SQLQuery.Select getWhere()
    {
        return transfer.getWhere();
    }

    @Override
    public void reconstruct(SQLiteDatabase db, KuickDb kuick, ContentValues item)
    {
        transfer.reconstruct(db, kuick, item);
    }
}
