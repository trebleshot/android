package com.genonbeta.TrebleShot.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
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
			setListShown(false);
			return new ListAdapter.Loader<>(mAdapter);
		}

		@Override
		public void onLoadFinished(Loader<ArrayList<T>> loader, ArrayList<T> data)
		{
			if (isResumed())
			{
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
	public void onActivityCreated(@Nullable Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mAdapter = onAdapter();

		setListAdapter(mAdapter);
		getLoaderManager().initLoader(TASK_ID_LOAD, null, mLoaderCallbackLoad);
		setEmptyText(getString(R.string.list_empty_msg));
	}

	public abstract E onAdapter();

	public E getAdapter()
	{
		return mAdapter;
	}

	public void refreshList()
	{
		getLoaderManager().restartLoader(TASK_ID_LOAD, null, mLoaderCallbackLoad);
	}

	protected void onListRefreshed()
	{
	}
}
