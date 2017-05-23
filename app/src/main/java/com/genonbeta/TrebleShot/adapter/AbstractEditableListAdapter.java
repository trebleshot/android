package com.genonbeta.TrebleShot.adapter;

import android.content.Context;

import com.genonbeta.widget.ListAdapter;

abstract public class AbstractEditableListAdapter<T> extends ListAdapter<T>
{
	public String mSearchWord;

	public AbstractEditableListAdapter(Context context)
	{
		super(context);
	}

	public String getSearchWord()
	{
		return mSearchWord;
	}

	public boolean search(String word)
	{
		mSearchWord = word;
		return true;
	}
}
