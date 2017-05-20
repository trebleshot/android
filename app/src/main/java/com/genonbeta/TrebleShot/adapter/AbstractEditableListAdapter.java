package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;

abstract public class AbstractEditableListAdapter extends BaseAdapter
{
	protected boolean mLockRequested = false;
	protected Context mContext;
	protected LayoutInflater mInflater;

	public AbstractEditableListAdapter(Context context)
	{
		mContext = context;
		mInflater = LayoutInflater.from(context);
	}

	protected abstract void onSearch(String word);

	protected abstract void onUpdate();

	public Context getContext()
	{
		return mContext;
	}

	public LayoutInflater getInflater()
	{
		return mInflater;
	}

	public boolean search(String word)
	{
		onSearch(word);
		return true;
	}

	public boolean update()
	{
		if (mLockRequested)
			return false;

		mLockRequested = true;
		onUpdate();
		mLockRequested = false;

		return true;
	}
}
