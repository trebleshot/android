package com.genonbeta.TrebleShot.app;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;

/**
 * created by: veli
 * date: 26.03.2018 11:45
 */

abstract public class RecyclerViewFragment<T, V extends RecyclerViewAdapter.ViewHolder, E extends RecyclerViewAdapter<T, V>>
		extends ListFragment<RecyclerView, T, E>
{
	private RecyclerView mRecyclerView;

	final private Handler mHandler = new Handler();

	final private Runnable mRequestFocus = new Runnable()
	{
		@Override
		public void run()
		{
			mRecyclerView.focusableViewAvailable(mRecyclerView);
		}
	};

	@Override
	protected void onListRefreshed()
	{
		super.onListRefreshed();

		boolean isEmpty = getAdapter().getCount() == 0;

		getEmptyView().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
		getListView().setVisibility(isEmpty ? View.GONE : View.VISIBLE);
	}

	public RecyclerView.LayoutManager onLayoutManager()
	{
		return getDefaultLayoutManager();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = super.onCreateView(inflater, container, savedInstanceState);

		mRecyclerView = view.findViewById(R.id.customListFragment_listView);

		if (mRecyclerView == null)
			mRecyclerView = onListView(getContainer(), getListViewContainer());

		return view;
	}

	@Override
	protected RecyclerView onListView(View mainContainer, ViewGroup listViewContainer)
	{
		RecyclerView recyclerView = new RecyclerView(getContext());

		recyclerView.setLayoutManager(onLayoutManager());

		recyclerView.setLayoutParams(new GridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));

		listViewContainer.addView(recyclerView);

		return recyclerView;
	}

	@Override

	protected void onEnsureList()
	{
		mHandler.post(mRequestFocus);
	}

	@Override
	public boolean onSetListAdapter(E adapter)
	{
		if (mRecyclerView == null)
			return false;

		mRecyclerView.setAdapter(adapter);

		return true;
	}

	public RecyclerView.LayoutManager getDefaultLayoutManager()
	{
		return new LinearLayoutManager(getContext());
	}

	@Override
	public RecyclerView getListView()
	{
		return mRecyclerView;
	}
}
