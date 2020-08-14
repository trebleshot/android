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

import java.util.Locale;

public class LoadedMember extends TransferMember implements Editable
{
    public Device device;

    public LoadedMember()
    {

    }

    public LoadedMember(long transferId, String deviceId, TransferItem.Type type)
    {
        super(transferId, deviceId, type);
    }

    @Override
    public boolean applyFilter(String[] filteringKeywords)
    {
        return false;
    }

    @Override
    public long getId()
    {
        return String.format(Locale.getDefault(), "%s_%d", deviceId, transferId).hashCode();
    }

    @Override
    public void setId(long id)
    {

    }

    @Override
    public boolean comparisonSupported()
    {
        return false;
    }

    @Override
    public String getComparableName()
    {
        return device.username;
    }

    @Override
    public long getComparableDate()
    {
        return device.lastUsageTime;
    }

    @Override
    public long getComparableSize()
    {
        return 0;
    }

    @Override
    public String getSelectableTitle()
    {
        return device.username;
    }

    @Override
    public boolean isSelectableSelected()
    {
        return false;
    }

    @Override
    public boolean setSelectableSelected(boolean selected)
    {
        return false;
    }
}
