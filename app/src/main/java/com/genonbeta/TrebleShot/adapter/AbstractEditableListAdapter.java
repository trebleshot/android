package com.genonbeta.TrebleShot.adapter;

import android.content.Context;

import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.widget.ListAdapter;

abstract public class AbstractEditableListAdapter<T> extends ListAdapter<T>
{
	private String mSearchWord;

	public AbstractEditableListAdapter(Context context)
	{
		super(context);
	}

	public String getSearchWord()
	{
		return mSearchWord;
	}

	protected boolean applySearch(String searchWord)
	{
		if (getSearchWord() == null || getSearchWord().length() == 0)
			return true;

		return ApplicationHelper.searchWord(searchWord, getSearchWord());
	}

	public boolean search(String word)
	{
		mSearchWord = word;
		return true;
	}
}
