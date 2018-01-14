package com.genonbeta.TrebleShot.widget;

import android.content.Context;

import com.genonbeta.TrebleShot.object.Shareable;

abstract public class ShareableListAdapter<T extends Shareable> extends EditableListAdapter<T>
{
	public ShareableListAdapter(Context context)
	{
		super(context);
	}
}
