package com.genonbeta.android.framework.util.listing;

import java.util.ArrayList;

/**
 * created by: Veli
 * date: 29.03.2018 01:26
 */
abstract public class Merger<T>
{
	public ArrayList<T> mBelongings = new ArrayList<>();

	abstract public boolean equals(Object obj);

	public ArrayList<T> getBelongings()
	{
		return mBelongings;
	}
}
