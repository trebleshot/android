package com.genonbeta.TrebleShot.widget;

import android.content.Context;

import com.genonbeta.TrebleShot.object.Selectable;

import java.util.ArrayList;

/**
 * created by: Veli
 * date: 12.01.2018 16:55
 */

abstract public class EditableListAdapter<T extends Selectable> extends ListAdapter<T>
{
	private PowerfulActionMode.SelectorConnection mSelectionConnection;

	public EditableListAdapter(Context context)
	{
		super(context);
	}

	public EditableListAdapter(Context context, PowerfulActionMode.SelectorConnection<T> selectorConnection)
	{
		super(context);
		setSelectionConnection(selectorConnection);
	}

	public PowerfulActionMode.SelectorConnection<T> getSelectionConnection()
	{
		return mSelectionConnection;
	}

	public void setSelectionConnection(PowerfulActionMode.SelectorConnection<T> selectionConnection)
	{
		mSelectionConnection = selectionConnection;
	}
}
