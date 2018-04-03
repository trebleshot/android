package com.genonbeta.TrebleShot.widget;

import android.content.Context;

import com.genonbeta.TrebleShot.object.Shareable;

abstract public class ShareableListAdapter<T extends Shareable, V extends EditableListAdapter.EditableViewHolder>
		extends EditableListAdapter<T, V>
{
	public ShareableListAdapter(Context context)
	{
		super(context);
	}
}
