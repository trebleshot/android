package com.genonbeta.TrebleShot.widget;

import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.android.framework.widget.ListAdapterImpl;

import java.util.ArrayList;

/**
 * created by: veli
 * date: 14/04/18 00:51
 */
public interface EditableListAdapterImpl<T extends Editable> extends ListAdapterImpl<T>
{
	void notifyAllSelectionChanges();

	void notifyItemChanged(int position);

	void notifyItemRangeChanged(int positionStart, int itemCount);

	void syncSelectionList();

	void syncSelectionList(ArrayList<T> itemList);
}
