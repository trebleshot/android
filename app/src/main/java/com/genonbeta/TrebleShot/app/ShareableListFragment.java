package com.genonbeta.TrebleShot.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.util.Shareable;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;

import java.util.ArrayList;

public abstract class ShareableListFragment<T extends Shareable, E extends ShareableListAdapter<T>>
		extends EditableListFragment<T, E>
{
	private ArrayList<T> mSelectionList = new ArrayList<>();
	private ArrayList<T> mCachedList = new ArrayList<>();
	private boolean mSearchSupport = true;

	private SearchView.OnQueryTextListener mSearchComposer = new SearchView.OnQueryTextListener()
	{
		@Override
		public boolean onQueryTextSubmit(String word)
		{
			return search(word);
		}

		@Override
		public boolean onQueryTextChange(String word)
		{
			return search(word);
		}
	};

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);

		if (getSearchSupport()) {
			inflater.inflate(R.menu.actions_search, menu);

			((SearchView) menu.findItem(R.id.search).getActionView())
					.setOnQueryTextListener(mSearchComposer);
		}
	}

	@Override
	public boolean onPrepareActionMenu(Context context, PowerfulActionMode actionMode)
	{
		super.onPrepareActionMenu(context, actionMode);
		getSelectionList().clear();
		return true;
	}

	@Override
	public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
	{
		super.onCreateActionMenu(context, actionMode, menu);
		actionMode.getMenuInflater().inflate(R.menu.action_mode_share, menu);
		return true;
	}

	@Override
	public boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.action_mode_share_trebleshot || id == R.id.action_mode_share_all_apps) {
			Intent shareIntent = null;
			String action = (item.getItemId() == R.id.action_mode_share_all_apps) ? (getSelectionList().size() > 1 ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND) : (getSelectionList().size() > 1 ? ShareActivity.ACTION_SEND_MULTIPLE : ShareActivity.ACTION_SEND);

			if (getSelectionList().size() > 1) {
				shareIntent = new Intent(action);

				shareIntent.setType("*/*");

				ArrayList<Uri> uriList = new ArrayList<>();
				ArrayList<CharSequence> nameList = new ArrayList<>();

				for (T sharedItem : getSelectionList()) {
					uriList.add(sharedItem.uri);
					nameList.add(sharedItem.fileName);
				}

				shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
				shareIntent.putCharSequenceArrayListExtra(ShareActivity.EXTRA_FILENAME_LIST, nameList);
			} else if (getSelectionList().size() == 1) {
				T sharedItem = getSelectionList().get(0);

				shareIntent = new Intent(action);

				shareIntent.putExtra(Intent.EXTRA_STREAM, sharedItem.uri);
				shareIntent.putExtra(ShareActivity.EXTRA_FILENAME_LIST, sharedItem.fileName);
				shareIntent.setType("*/*");
			}

			if (shareIntent != null)
				startActivity((item.getItemId() == R.id.action_mode_share_all_apps) ? Intent.createChooser(shareIntent, getString(R.string.text_fileShareAppChoose)) : shareIntent);
		} else if (id == R.id.action_mode_abs_editable_multi_select) {
			getSelectionList().clear();
			setSelection(getListView().getCheckedItemCount() != getAdapter().getCount());
			return false;
		} else
			return false;

		return true;
	}

	@Override
	public void onItemChecked(Context context, PowerfulActionMode actionMode, int position, boolean isSelected)
	{
		super.onItemChecked(context, actionMode, position, isSelected);

		T shareable = (T) getAdapter().getItem(position);

		if (isSelected)
			getSelectionList().add(shareable);
		else
			getSelectionList().remove(shareable);

		actionMode.setTitle(String.valueOf(getSelectionList().size()));
	}

	public ArrayList<T> getCachedList()
	{
		return mCachedList;
	}

	public boolean getSearchSupport()
	{
		return mSearchSupport;
	}

	public ArrayList<T> getSelectionList()
	{
		return mSelectionList;
	}

	public void openFile(Uri uri, String type, String chooserText)
	{
		Intent openIntent = new Intent(Intent.ACTION_VIEW);

		openIntent.setDataAndType(uri, type);

		startActivity(Intent.createChooser(openIntent, chooserText));
	}

	public boolean search(String word)
	{
		if (getPowerfulActionMode() != null)
			getPowerfulActionMode().finish(this);

		if ((word == null || word.length() == 0) && lockRefresh(false) && mCachedList.size() != 0) {
			mCachedList.clear();
			getAdapter().onUpdate(mCachedList);
			getAdapter().notifyDataSetChanged();
		} else {
			lockRefresh(true);

			if (mCachedList.size() == 0)
				mCachedList.addAll(getAdapter().getList());

			ArrayList<T> searchableList = new ArrayList<>();

			for (T shareable : mCachedList)
				if (shareable.searchMatches(word))
					searchableList.add(shareable);

			getAdapter().onUpdate(searchableList);
			getAdapter().notifyDataSetChanged();
		}


		return getAdapter().getCount() > 0;
	}

	public void setSearchSupport(boolean searchSupport)
	{
		mSearchSupport = searchSupport;
	}
}
