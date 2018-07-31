package com.genonbeta.android.framework.util.listing;

import java.util.ArrayList;

/**
 * created by: Veli
 * date: 29.03.2018 01:26
 */
public class Lister<T, V extends Merger<T>>
{
	private ArrayList<V> mList = new ArrayList<>();

	public Lister()
	{
	}

	public ArrayList<V> getList()
	{
		return mList;
	}

	public void offer(T object, V merger)
	{
		int index = getList().indexOf(merger);

		if (index == -1)
			getList().add(merger);
		else
			merger = getList().get(index);

		merger.getBelongings().add(object);
	}
}
