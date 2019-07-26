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

import com.genonbeta.TrebleShot.adapter.TransferGroupListAdapter;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;

/**
 * created by: veli
 * date: 7/24/19 6:08 PM
 */
public class PreloadedGroup extends TransferGroup implements GroupEditableListAdapter.GroupEditable
{
	public int viewType;
	public String representativeText;

	public int numberOfCompleted;
	public int numberOfTotal;
	public int numberOfOutgoing;
	public int numberOfIncoming;
	public long bytesInTotal;
	public long bytesPending;
	public long bytesInOutgoing;
	public long bytesInIncoming;
	public double percentage;
	public boolean isRunning;
	public boolean hasIssues;
	public boolean hasOutgoing;
	public boolean hasIncoming;
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

	@Override
	public boolean comparisonSupported()
	{
		return true;
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
		return bytesInTotal;
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
		String size = FileUtils.sizeExpression(bytesInTotal, false);

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

	@Override
	public String getRepresentativeText()
	{
		return representativeText;
	}

	@Override
	public void setRepresentativeText(CharSequence representativeText)
	{
		this.representativeText = String.valueOf(representativeText);
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
		bytesInTotal = ((Long) size).intValue();
	}
}
