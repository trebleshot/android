package com.genonbeta.TrebleShot.util;

import android.net.Uri;

/**
 * created by: Veli
 * date: 19.11.2017 16:50
 */

public class Shareable
{
	public String friendlyName;
	public String fileName;
	public Uri uri;

	public Shareable(String friendlyName, String fileName, Uri uri)
	{
		this.friendlyName = friendlyName;
		this.fileName = fileName;
		this.uri = uri;
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
}
