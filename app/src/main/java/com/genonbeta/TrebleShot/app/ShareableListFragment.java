package com.genonbeta.TrebleShot.app;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.widget.SearchView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.io.StreamInfo;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;

import java.io.File;
import java.util.ArrayList;

public abstract class ShareableListFragment<T extends Shareable, V extends RecyclerViewAdapter.ViewHolder, E extends ShareableListAdapter<T, V>>
		extends EditableListFragment<T, V, E>
{
	private ArrayList<T> mCachedList = new ArrayList<>();
	private boolean mSearchSupport = true;
	private boolean mSearchActive = false;
	private Handler mHandler = new Handler(Looper.myLooper());
	private String mDefaultEmptyText = null;
	private Toast mToastNoResult = null;

	private ContentObserver mObserver = new ContentObserver(mHandler)
	{
		@Override
		public boolean deliverSelfNotifications()
		{
			return true;
		}

		@Override
		public void onChange(boolean selfChange)
		{
			refreshList();
		}
	};

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

			SearchView searchView = ((SearchView) menu.findItem(R.id.search).getActionView());

			searchView.setOnQueryTextListener(mSearchComposer);
			searchView.setMaxWidth(500);
		}
	}

	@Override
	public boolean onPrepareActionMenu(Context context, PowerfulActionMode actionMode)
	{
		super.onPrepareActionMenu(context, actionMode);
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

		ArrayList<T> selectedItemList = new ArrayList<>(getSelectionConnection().getSelectedItemList());

		if (selectedItemList.size() > 0
				&& (id == R.id.action_mode_share_trebleshot || id == R.id.action_mode_share_all_apps)) {
			Intent shareIntent = new Intent()
					.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
					.setAction((item.getItemId() == R.id.action_mode_share_all_apps)
							? (selectedItemList.size() > 1 ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND)
							: (selectedItemList.size() > 1 ? ShareActivity.ACTION_SEND_MULTIPLE : ShareActivity.ACTION_SEND));

			if (selectedItemList.size() > 1) {
				MIMEGrouper mimeGrouper = new MIMEGrouper();
				ArrayList<Uri> uriList = new ArrayList<>();
				ArrayList<CharSequence> nameList = new ArrayList<>();

				for (T sharedItem : selectedItemList) {
					uriList.add(sharedItem.uri);
					nameList.add(sharedItem.fileName);

					if (!mimeGrouper.isLocked())
						mimeGrouper.process(sharedItem.mimeType);
				}

				shareIntent.setType(mimeGrouper.toString())
						.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
						.putCharSequenceArrayListExtra(ShareActivity.EXTRA_FILENAME_LIST, nameList);
			} else if (selectedItemList.size() == 1) {
				T sharedItem = selectedItemList.get(0);

				shareIntent.setType(sharedItem.mimeType)
						.putExtra(Intent.EXTRA_STREAM, sharedItem.uri)
						.putExtra(ShareActivity.EXTRA_FILENAME_LIST, sharedItem.fileName);
			}

			try {
				startActivity(item.getItemId() == R.id.action_mode_share_all_apps
						? Intent.createChooser(shareIntent, getString(R.string.text_fileShareAppChoose))
						: shareIntent);
			} catch (Throwable e) {
				e.printStackTrace();
				Toast.makeText(getActivity(), R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show();

				return false;
			}
		} else
			return super.onActionMenuItemSelected(context, actionMode, item);

		return true;
	}

	public ArrayList<T> getCachedList()
	{
		return mCachedList;
	}

	public ContentObserver getDefaultContentObserver()
	{
		return mObserver;
	}

	public boolean getSearchSupport()
	{
		return mSearchSupport;
	}

	@Override
	public boolean isRefreshLocked()
	{
		return super.isRefreshLocked() || mSearchActive;
	}

	public void openFile(Uri uri, String chooserText)
	{
		try {
			StreamInfo streamInfo = StreamInfo.getStreamInfo(getActivity(), uri);
			Intent openIntent = FileUtils.applySecureOpenIntent(getActivity(), streamInfo, new Intent(Intent.ACTION_VIEW));

			startActivity(Intent.createChooser(openIntent, chooserText));
		} catch (Throwable e) {
			e.printStackTrace();
			Toast.makeText(getActivity(), R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show();
		}
	}

	public void performLayoutClick(View view, V viewHolder)
	{
		T object = getAdapter().getItem(viewHolder.getAdapterPosition());

		if (!setItemSelected(viewHolder.getAdapterPosition()))
			openFile(object.uri, getString(R.string.text_fileOpenAppChoose));
	}

	public boolean search(String word)
	{
		mSearchActive = word != null && word.length() > 0;

		if (mSearchActive) {
			if (mCachedList.size() == 0)
				mCachedList.addAll(getAdapter().getList());

			ArrayList<T> searchableList = new ArrayList<>();

			for (T shareable : mCachedList)
				if (shareable.searchMatches(word))
					searchableList.add(shareable);

			if (searchableList.size() > 0) {
				getAdapter().onUpdate(searchableList);
				getAdapter().notifyDataSetChanged();

				if (mToastNoResult != null)
					mToastNoResult.cancel();
			} else {
				String text = getString(R.string.text_emptySearchResult, word);

				if (mToastNoResult == null) {
					mToastNoResult = Toast.makeText(getContext(), text, Toast.LENGTH_SHORT);
					mToastNoResult.setGravity(Gravity.TOP, mToastNoResult.getXOffset(), mToastNoResult.getYOffset());
				} else
					mToastNoResult.setText(text);

				mToastNoResult.show();
			}
		} else if (!loadIfRequested() && mCachedList.size() != 0) {
			getAdapter().onUpdate(mCachedList);
			getAdapter().notifyDataSetChanged();

			mCachedList.clear();
		}

		if (mDefaultEmptyText == null)
			mDefaultEmptyText = String.valueOf(getEmptyText().getText());

		setEmptyText(mSearchActive
				? getString(R.string.text_emptySearchResult, word)
				: mDefaultEmptyText);

		return getAdapter().getCount() > 0;
	}

	public void setSearchSupport(boolean searchSupport)
	{
		mSearchSupport = searchSupport;
	}

	public static class MIMEGrouper
	{
		public static final String TYPE_GENERIC = "*";

		private String mMajor;
		private String mMinor;
		private boolean mLocked;

		public MIMEGrouper()
		{

		}

		public boolean isLocked()
		{
			return mLocked;
		}

		public String getMajor()
		{
			return mMajor == null ? TYPE_GENERIC : mMajor;
		}

		public String getMinor()
		{
			return mMinor == null ? TYPE_GENERIC : mMinor;
		}

		public void process(String mimeType)
		{
			if (mimeType == null || mimeType.length() < 3 || !mimeType.contains(File.separator))
				return;

			String[] splitMIME = mimeType.split(File.separator);

			process(splitMIME[0], splitMIME[1]);
		}

		public void process(String major, String minor)
		{
			if (mMajor == null || mMinor == null) {
				mMajor = major;
				mMinor = minor;
			} else if (getMajor().equals(TYPE_GENERIC))
				mLocked = true;
			else if (!getMajor().equals(major)) {
				mMajor = TYPE_GENERIC;
				mMinor = TYPE_GENERIC;

				mLocked = true;
			} else if (!getMinor().equals(minor)) {
				mMinor = TYPE_GENERIC;
			}
		}

		@Override
		public String toString()
		{
			return getMajor() + File.separator + getMinor();
		}
	}
}
