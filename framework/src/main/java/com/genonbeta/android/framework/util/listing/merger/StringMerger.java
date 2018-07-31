package com.genonbeta.android.framework.util.listing.merger;

import android.support.annotation.NonNull;

import com.genonbeta.android.framework.util.listing.ComparableMerger;

/**
 * created by: Veli
 * date: 29.03.2018 01:44
 */
public class StringMerger<T> extends ComparableMerger<T>
{
	private String mString;

	public StringMerger(String string)
	{
		mString = string;
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj.equals(mString);
	}

	public String getString()
	{
		return mString;
	}

	@Override
	public int compareTo(@NonNull ComparableMerger<T> o)
	{
		if (!(o instanceof StringMerger))
			return -1;

		return ((StringMerger) o).getString().compareToIgnoreCase(getString());
	}
}
