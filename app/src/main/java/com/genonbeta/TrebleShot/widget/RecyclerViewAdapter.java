package com.genonbeta.TrebleShot.widget;

import android.support.v7.widget.RecyclerView;

import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter;

/**
 * created by: veli
 * date: 26.03.2018 11:46
 */

abstract public class RecyclerViewAdapter<T, V extends RecyclerView.ViewHolder>
		extends RecyclerView.Adapter<V>
		implements ListAdapterImpl<T>
{
}
