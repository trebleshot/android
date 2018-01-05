package com.genonbeta.TrebleShot.object;

import android.net.Uri;

import com.genonbeta.TrebleShot.util.TextUtils;

/**
 * created by: Veli
 * date: 19.11.2017 16:50
 */

public class Shareable implements Selectable
{
	public String friendlyName;
	public String fileName;
	public Uri uri;

	private boolean mIsSelected = false;

	public Shareable()
	{
	}

	public Shareable(String friendlyName, String fileName, Uri uri)
	{
		this.friendlyName = friendlyName;
		this.fileName = fileName;
		this.uri = uri;
	}

	@Override
	public boolean isSelectableSelected()
	{
		return mIsSelected;
	}

	@Override
	public String getSelectableFriendlyName()
	{
		return this.friendlyName;
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof Shareable ? ((Shareable) obj).uri.equals(uri) : super.equals(obj);
	}

	public boolean searchMatches(String searchWord)
	{
		return TextUtils.searchWord(this.friendlyName, searchWord);
	}

	@Override
	public void setSelectableSelected(boolean selected)
	{
		mIsSelected = selected;
	}
}
