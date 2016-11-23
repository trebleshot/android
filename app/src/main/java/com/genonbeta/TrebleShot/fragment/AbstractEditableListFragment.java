package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.adapter.AbstractEditableListAdapter;
import com.genonbeta.TrebleShot.helper.GAnimater;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class AbstractEditableListFragment<T extends AbstractEditableListAdapter> extends ListFragment
{
	private T mAdapter;
	private ActionMode mActionMode;
	private ActionModeListener mActionModeListener;
	private SearchView mSearchView;
	private boolean mIsLoading = false;

	private Runnable mNotifyListChanges = new Runnable()
	{
		@Override
		public void run()
		{
			getAdapter().notifyDataSetChanged();
			setEmptyText(getString(R.string.list_empty_msg));

			if (mActionMode != null)
				for (int i = 0; i < getListView().getCount(); i++)
					if (getListView().isItemChecked(i))
						mActionModeListener.onItemCheckedStateChanged(mActionMode, i, 0, true);

		}
	};

	private Runnable mUpdateList = new Runnable()
	{
		@Override
		public void run()
		{
			boolean updateSucceed = getAdapter().update();

			if (updateSucceed && getActivity() != null && !isDetached())
				getActivity().runOnUiThread(mNotifyListChanges);

			mIsLoading = false;
		}
	};

	private SearchView.OnQueryTextListener mSearchComposer = new SearchView.OnQueryTextListener()
	{
		@Override
		public boolean onQueryTextSubmit(String word)
		{
			return false;
		}

		@Override
		public boolean onQueryTextChange(String word)
		{
			search(word);
			return false;
		}
	};


	protected abstract T onAdapter();

	protected abstract ActionModeListener onActionModeListener();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setHasOptionsMenu(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		this.mAdapter = this.onAdapter();
		this.mActionModeListener = this.onActionModeListener();

		this.setListAdapter(mAdapter);

		this.getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
		this.getListView().setMultiChoiceModeListener(this.mActionModeListener);
		this.getListView().setPadding(20, 0, 20, 0);

		GAnimater.applyLayoutAnimation(getListView(), GAnimater.APPEAR);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		this.updateInBackground();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.search_menu, menu);

		mSearchView = (SearchView) menu.findItem(R.id.search).getActionView();
		mSearchView.setOnQueryTextListener(mSearchComposer);
	}

	@Override
	public void onDetach()
	{
		super.onDetach();

		if (mActionMode != null)
		{
			mActionMode.finish();
			mActionMode = null;
		}
	}

	protected T getAdapter()
	{
		return this.mAdapter;
	}
	public boolean isLoading()
	{
		return mIsLoading;
	}

	public void openFile(Uri uri, String type, String chooserText)
	{
		Intent openIntent = new Intent(Intent.ACTION_VIEW);

		openIntent.setDataAndType(uri, type);

		this.startActivity(Intent.createChooser(openIntent, chooserText));
	}

	public void search(String word)
	{
		if (word.equals(""))
			word = null;
		else
			word = word.toLowerCase();

		getAdapter().search(word);
		updateInBackground();
	}

	public void setItemsChecked(boolean check)
	{
		for (int i = 0; i < getListView().getCount(); i++)
			getListView().setItemChecked(i, check);
	}

	public boolean updateInBackground()
	{
		if (getActivity() == null || isLoading())
			return false;

		this.mIsLoading = true;

		setEmptyText(getString(R.string.loading));

		new Thread(this.mUpdateList).start();

		return true;
	}

	protected abstract class ActionModeListener implements AbsListView.MultiChoiceModeListener
	{
		protected HashSet<Uri> mCheckedList = new HashSet<Uri>();
		protected MenuItem mSelectAll;

		public abstract void onItemChecked(ActionMode mode, int position, long id, boolean isChecked);

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu)
		{
			mode.getMenuInflater().inflate(R.menu.share_actions, menu);

			mSelectAll = menu.findItem(R.id.file_actions_select);
			mActionMode = mode;

			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu)
		{
			mCheckedList.clear();

			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item)
		{
			if (item.getItemId() == R.id.file_actions_share || item.getItemId() == R.id.file_actions_share_trebleshot)
			{
				Intent shareIntent = null;
				String action = (item.getItemId() == R.id.file_actions_share) ? (mCheckedList.size() > 1 ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND) : (mCheckedList.size() > 1 ? ShareActivity.ACTION_SEND_MULTIPLE : ShareActivity.ACTION_SEND);

				if (mCheckedList.size() > 1)
				{
					ArrayList<Uri> uris = new ArrayList<Uri>();

					for (Object uri : mCheckedList)
						uris.add((Uri) uri);

					shareIntent = new Intent(action);

					shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
					shareIntent.setType("*/*");
				}
				else if (mCheckedList.size() == 1)
				{
					Uri fileUri = (Uri) mCheckedList.toArray()[0];

					shareIntent = new Intent(action);

					shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
					shareIntent.setType("*/*");
				}

				if (shareIntent != null)
				{
					startActivity((item.getItemId() == R.id.file_actions_share) ? Intent.createChooser(shareIntent, getString(R.string.file_share_app_chooser_msg)) : shareIntent);
					return true;
				}
			}
			else if (item.getItemId() == R.id.file_actions_select)
			{
				setItemsChecked(mCheckedList.size() != getListView().getCount());
				return true;
			}

			return false;
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean isChecked)
		{
			onItemChecked(mode, position, id, isChecked);

			mSelectAll.setIcon((mCheckedList.size() == getListView().getCount()) ? R.drawable.ic_unselect : R.drawable.ic_select);

			mode.setTitle(String.valueOf(getListView().getCheckedItemCount()));
		}

		@Override
		public void onDestroyActionMode(ActionMode p1)
		{
			mCheckedList.clear();
			mActionMode = null;
		}
	}
}
