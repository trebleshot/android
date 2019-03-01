package com.genonbeta.TrebleShot.object;

import java.util.Locale;

public class ShowingAssignee extends TransferGroup.Assignee implements Editable
{
    public NetworkDevice device;
    public NetworkDevice.Connection connection;

    public ShowingAssignee()
    {

    }

    @Override
    public boolean applyFilter(String[] filteringKeywords)
    {
        return false;
    }

    @Override
    public long getId()
    {
        return String.format(Locale.getDefault(), "%s_%d", deviceId, groupId).hashCode();
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
        return device.nickname;
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
        return device.nickname;
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
