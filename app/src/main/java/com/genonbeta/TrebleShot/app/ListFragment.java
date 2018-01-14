package com.genonbeta.TrebleShot.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.widget.ListAdapter;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 12/3/16 9:57 AM
 */

public abstract class ListFragment<T, E extends ListAdapter<T>> extends android.support.v4.app.ListFragment
{
	public static final String TAG = "Fragment";

	public final static int TASK_ID_LOAD = 0;

	private E mAdapter;

	LoaderManager.LoaderCallbacks<ArrayList<T>> mLoaderCallbackLoad = new LoaderManager.LoaderCallbacks<ArrayList<T>>()
	{
		@Override
		public Loader<ArrayList<T>> onCreateLoader(int id, Bundle args)
		{
			if (mAdapter.getCount() == 0)
				setListShown(false);

			return new ListAdapter.Loader<>(mAdapter);
		}

		@Override
		public void onLoadFinished(Loader<ArrayList<T>> loader, ArrayList<T> data)
		{
			if (isResumed()) {
				setListShown(true);

				mAdapter.onUpdate(data);
				mAdapter.notifyDataSetChanged();

				onListRefreshed();
			}
		}

		@Override
		public void onLoaderReset(Loader<ArrayList<T>> loader)
		{

		}
	};

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mAdapter = onAdapter();
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		setListAdapter(mAdapter);
		getLoaderManager().initLoader(TASK_ID_LOAD, null, mLoaderCallbackLoad);
		setEmptyText(getString(R.string.text_listEmpty));
	}

	public abstract E onAdapter();

	protected Snackbar createSnackbar(int resId, Object... objects)
	{
		return Snackbar.make(getListView(), getString(resId, objects), Snackbar.LENGTH_LONG);
	}

	public E getAdapter()
	{
		return mAdapter;
	}

	public void refreshList()
	{
		if (!getLoaderManager().hasRunningLoaders())
			getLoaderManager().restartLoader(TASK_ID_LOAD, null, mLoaderCallbackLoad);
	}

	protected void onListRefreshed()
	{
	}
}
