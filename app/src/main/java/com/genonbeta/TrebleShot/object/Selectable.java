package com.genonbeta.TrebleShot.object;

/**
 * created by: Veli
 * date: 5.01.2018 10:58
 */

public interface Selectable
{
	public String getSelectableFriendlyName();
	public boolean isSelectableSelected();
	public void setSelectableSelected(boolean selected);
}
