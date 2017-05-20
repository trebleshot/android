package com.genonbeta.TrebleShot.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.SearchView;
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

public abstract class AbstractEditableListFragment<T extends AbstractEditableListAdapter> extends ListFragment
{
	private T mAdapter;
	private ActionMode mActionMode;
	private ActionModeListener mActionModeListener;
	private boolean mSearchSupport = true;
	private boolean mIsLoading = false;

	private Runnable mNotifyListChanges = new Runnable()
	{
		@Override
		public void run()
		{
			getAdapter().notifyDataSetChanged();
			setEmptyText(getString(R.string.list_empty_msg));

			if (mActionModeListener != null)
				mActionModeListener.clearSelectionList();

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
		setHasOptionsMenu(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mAdapter = onAdapter();
		mActionModeListener = onActionModeListener();

		setListAdapter(mAdapter);

		if (mActionModeListener != null)
		{
			getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
			getListView().setMultiChoiceModeListener(mActionModeListener);
		}

		getListView().setDividerHeight(0);

		GAnimater.applyLayoutAnimation(getListView(), GAnimater.APPEAR);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		updateInBackground();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);

		if (mSearchSupport)
		{
			inflater.inflate(R.menu.search_menu, menu);

			((SearchView) menu.findItem(R.id.search).getActionView())
					.setOnQueryTextListener(mSearchComposer);
		}
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
		return mAdapter;
	}

	public boolean isLoading()
	{
		return mIsLoading;
	}

	public void openFile(Uri uri, String type, String chooserText)
	{
		Intent openIntent = new Intent(Intent.ACTION_VIEW);

		openIntent.setDataAndType(uri, type);

		startActivity(Intent.createChooser(openIntent, chooserText));
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
		mActionModeListener.clearSelectionList();

		for (int i = 0; i < getListView().getCount(); i++)
			getListView().setItemChecked(i, check);
	}

	public void setSearchSupport(boolean searchSupport)
	{
		mSearchSupport = searchSupport;
	}

	public boolean updateInBackground()
	{
		if (getActivity() == null || isLoading())
			return false;

		mIsLoading = true;

		setEmptyText(getString(R.string.loading));

		new Thread(mUpdateList).start();

		return true;
	}

	protected abstract class ActionModeListener implements AbsListView.MultiChoiceModeListener
	{
		private ArrayList<Uri> mCheckedList = new ArrayList<>();
		private ArrayMap<Uri, String> mCheckedNameList = new ArrayMap<>();
		private MenuItem mSelectAll;

		public abstract Uri onItemChecked(ActionMode mode, int position, long id, boolean isChecked);

		public String onProvideName(ActionMode mode, int position)
		{
			return null;
		}

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
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(getContext(), R.color.actionModeColorPrimary));

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
					shareIntent = new Intent(action);

					shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, mCheckedList);
					shareIntent.setType("*/*");

					if (mCheckedNameList.size() > 1)
					{
						ArrayList<CharSequence> nameExit = new ArrayList<>();

						for (Uri fileUri : mCheckedList)
							nameExit.add(mCheckedNameList.containsKey(fileUri) ? mCheckedNameList.get(fileUri) : null);

						shareIntent.putCharSequenceArrayListExtra(ShareActivity.EXTRA_FILENAME_LIST, nameExit);
					}
				}
				else if (mCheckedList.size() == 1)
				{
					Uri fileUri = (Uri) mCheckedList.toArray()[0];

					shareIntent = new Intent(action);

					shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
					shareIntent.setType("*/*");

					if (mCheckedNameList.containsKey(fileUri))
						shareIntent.putExtra(ShareActivity.EXTRA_FILENAME_LIST, mCheckedNameList.get(fileUri));
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
			Uri uri = onItemChecked(mode, position, id, isChecked);
			String name = onProvideName(mode, position);

			if (isChecked)
			{
				mCheckedList.add(uri);

				if (name != null)
					mCheckedNameList.put(uri, name);
			}
			else
			{
				if (mCheckedNameList.containsKey(uri))
					mCheckedNameList.remove(uri);

				mCheckedList.remove(uri);
			}

			mSelectAll.setIcon((mCheckedList.size() == mAdapter.getCount()) ? R.drawable.ic_unselect : R.drawable.ic_select);

			mode.setTitle(String.valueOf(getListView().getCheckedItemCount()));
		}

		@Override
		public void onDestroyActionMode(ActionMode p1)
		{
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(getContext(), android.R.color.transparent));

			mCheckedList.clear();
			mActionMode = null;
		}

		public MenuItem getQuickSelectMenuItem()
		{
			return mSelectAll;
		}

		public void clearSelectionList()
		{
			mCheckedList.clear();
		}

		public ArrayList<Uri> getSharedItemList()
		{
			return mCheckedList;
		}

		public ArrayMap<Uri, String> getSharedItemNameList()
		{
			return mCheckedNameList;
		}
	}
}
