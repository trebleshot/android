package com.genonbeta.TrebleShot.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter;

/**
 * created by: veli
 * date: 26.03.2018 11:46
 */

abstract public class RecyclerViewAdapter<T, V extends RecyclerView.ViewHolder>
		extends RecyclerView.Adapter<V>
		implements ListAdapterImpl<T>
{
	public Context mContext;
	private LayoutInflater mInflater;

	public RecyclerViewAdapter(Context context)
	{
		mContext = context;
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public void onDataSetChanged()
	{
		notifyDataSetChanged();
	}

	public Context getContext()
	{
		return mContext;
	}

	@Override
	public int getCount()
	{
		return getItemCount();
	}

	public LayoutInflater getInflater()
	{
		return mInflater;
	}
}
