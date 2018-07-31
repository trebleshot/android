package com.genonbeta.android.framework.widget;

import android.content.Context;
import android.view.LayoutInflater;

import java.util.ArrayList;

/**
 * created by: veli
 * date: 26.03.2018 11:12
 */

public interface ListAdapterImpl<T>
{
	void onDataSetChanged();

	ArrayList<T> onLoad();

	void onUpdate(ArrayList<T> passedItem);

	Context getContext();

	int getCount();

	LayoutInflater getInflater();

	ArrayList<T> getList();
}
