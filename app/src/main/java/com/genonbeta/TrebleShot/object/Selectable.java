package com.genonbeta.TrebleShot.object;

/**
 * created by: Veli
 * date: 5.01.2018 10:58
 */

public interface Selectable
{
	String getSelectableFriendlyName();

	boolean isSelectableSelected();

	void setSelectableSelected(boolean selected);
}
