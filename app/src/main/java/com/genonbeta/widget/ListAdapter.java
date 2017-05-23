package com.genonbeta.widget;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.view.LayoutInflater;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 12/4/16 11:54 PM
 */

abstract public class ListAdapter<T> extends android.widget.BaseAdapter
{
	public Context mContext;
	private LayoutInflater mInflater;

	public ListAdapter(Context context)
	{
		mContext = context;
		this.mInflater = LayoutInflater.from(context);
	}

	public Context getContext()
	{
		return mContext;
	}

	public LayoutInflater getInflater()
	{
		return mInflater;
	}

	public abstract ArrayList<T> onLoad();

	public abstract void onUpdate(ArrayList<T> passedItem);

	public static class Loader<E> extends AsyncTaskLoader<ArrayList<E>>
	{
		private ListAdapter<E> mAdapter;

		public Loader(ListAdapter<E> adapter)
		{
			super(adapter.getContext());
			mAdapter = adapter;
		}

		@Override
		protected void onStartLoading()
		{
			super.onStartLoading();
			forceLoad();
		}

		@Override
		public ArrayList<E> loadInBackground()
		{
			return mAdapter.onLoad();
		}
	}
}
