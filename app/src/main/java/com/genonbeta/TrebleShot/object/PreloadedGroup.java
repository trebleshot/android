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

import android.content.Context;
import android.util.Log;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.TransferGroupListAdapter;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;

/**
 * created by: veli
 * date: 7/24/19 6:08 PM
 */
public class PreloadedGroup extends TransferGroup implements GroupEditableListAdapter.GroupEditable
{
    public static final String TAG = PreloadedGroup.class.getSimpleName();

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
    public ShowingAssignee[] assignees = new ShowingAssignee[0];

    public PreloadedGroup()
    {
    }

    public PreloadedGroup(long id)
    {
        super(id);
    }

    public PreloadedGroup(String representativeText)
    {
        this.viewType = TransferGroupListAdapter.VIEW_TYPE_REPRESENTATIVE;
        this.representativeText = representativeText;
    }

    @Override
    public boolean applyFilter(String[] filteringKeywords)
    {
        ShowingAssignee[] copyAssignees = assignees;

        for (String keyword : filteringKeywords)
            for (ShowingAssignee assignee : copyAssignees)
                if (assignee.device.nickname.toLowerCase().contains(keyword.toLowerCase()))
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

    public boolean hasIncoming()
    {
        return numberOfIncoming > 0;
    }

    public boolean hasOutgoing()
    {
        return numberOfOutgoing > 0;
    }

    public String getAssigneesAsTitle()
    {
        ShowingAssignee[] copyAssignees = assignees;
        StringBuilder title = new StringBuilder();

        for (ShowingAssignee assignee : copyAssignees) {
            if (title.length() > 0)
                title.append(", ");
            title.append(assignee.device.nickname);
        }

        return title.toString();
    }

    public String getAssigneesAsTitle(Context context)
    {
        ShowingAssignee[] copyAssignees = assignees;

        if (copyAssignees.length == 1)
            return copyAssignees[0].device.nickname;

        return context.getResources().getQuantityString(R.plurals.text_devices,
                copyAssignees.length, copyAssignees.length);
    }

    @Override
    public String getComparableName()
    {
        return getSelectableTitle();
    }

    @Override
    public long getComparableDate()
    {
        return dateCreated;
    }

    @Override
    public long getComparableSize()
    {
        return bytesTotal();
    }

    @Override
    public long getId()
    {
        return id;
    }

    @Override
    public void setId(long id)
    {
        this.id = id;
    }

    @Override
    public String getSelectableTitle()
    {
        String title = getAssigneesAsTitle();
        String size = FileUtils.sizeExpression(bytesOutgoing + bytesOutgoing, false);

        return title.length() > 0 ? String.format("%s (%s)", title, size) : size;
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
        dateCreated = date;
    }

    @Override
    public boolean setSelectableSelected(boolean selected)
    {
        return !isGroupRepresentative() && super.setSelectableSelected(selected);
    }

    @Override
    public void setSize(long size)
    {
        Log.e(TAG, "setSize: This is not implemented");
    }
}
