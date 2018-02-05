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

	public static final int TASK_ID_REFRESH = 0;

	private E mAdapter;
	private LoaderCallbackRefresh mLoaderCallbackRefresh = new LoaderCallbackRefresh();

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
		getLoaderManager().initLoader(TASK_ID_REFRESH, null, mLoaderCallbackRefresh);
		setEmptyText(getString(R.string.text_listEmpty));
	}

	public abstract E onAdapter();

	protected void onListRefreshed()
	{
	}

	protected Snackbar createSnackbar(int resId, Object... objects)
	{
		return Snackbar.make(getListView(), getString(resId, objects), Snackbar.LENGTH_LONG);
	}

	public E getAdapter()
	{
		return mAdapter;
	}

	public LoaderCallbackRefresh getLoaderCallbackRefresh()
	{
		return mLoaderCallbackRefresh;
	}

	public void refreshList()
	{
		getLoaderCallbackRefresh().requestRefresh();
	}

	private class LoaderCallbackRefresh implements LoaderManager.LoaderCallbacks<ArrayList<T>>
	{
		private boolean mRunning = false;
		private boolean mReloadRequested = false;

		@Override
		public Loader<ArrayList<T>> onCreateLoader(int id, Bundle args)
		{
			mReloadRequested = false;
			mRunning = true;

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

			if (isReloadRequested())
				refresh();

			mRunning = false;
		}

		@Override
		public void onLoaderReset(Loader<ArrayList<T>> loader)
		{

		}

		public boolean isRunning()
		{
			return mRunning;
		}

		public boolean isReloadRequested()
		{
			return mReloadRequested;
		}

		public void refresh()
		{
			getLoaderManager().restartLoader(TASK_ID_REFRESH, null, mLoaderCallbackRefresh);
		}

		public boolean requestRefresh()
		{
			if (isRunning() && isReloadRequested())
				return false;

			if (!isRunning())
				refresh();
			else
				mReloadRequested = true;

			return true;
		}
	}
}
