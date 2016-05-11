package com.genonbeta.TrebleShot.fragment;

import android.content.*;
import android.net.*;
import android.os.*;
import android.support.v7.widget.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.genonbeta.TrebleShot.*;
import com.genonbeta.TrebleShot.adapter.*;
import com.genonbeta.TrebleShot.fragment.dialog.*;
import com.genonbeta.TrebleShot.helper.*;
import com.genonbeta.TrebleShot.service.*;

import android.support.v7.widget.SearchView;

public class ReceivedFilesListFragment extends AbstractMediaListFragment<ReceivedFilesListAdapter>
{
	public static final String TAG = "ReceivedFilesListFragment";

	private NotificationPublisher mPublisher;
	private SearchView mSearchView;
	private SearchComposer mSearchComposer = new SearchComposer();
	private Receiver mReceiver = new Receiver();
	private IntentFilter mIntentFilter = new IntentFilter();

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setHasOptionsMenu(true);
	}

	@Override
	protected ReceivedFilesListAdapter onAdapter()
	{
		return new ReceivedFilesListAdapter(getActivity());
	}

	@Override
	protected AbstractMediaListFragment.MediaChoiceListener onChoiceListener()
	{
		return new ChoiceListener();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mPublisher = new NotificationPublisher(getActivity());

		getListView().setPadding(20, 0, 20, 0);

		mIntentFilter.addAction(ServerService.ACTION_FILE_LIST_CHANGED);

		GAnimater.applyLayoutAnimation(getListView(), GAnimater.APPEAR);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		this.getActivity().registerReceiver(mReceiver, mIntentFilter);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		this.getActivity().unregisterReceiver(mReceiver);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.search_menu, menu);
		inflater.inflate(R.menu.received_files_options, menu);

		mSearchView = (SearchView)menu.findItem(R.id.search).getActionView();

		setupSearchView();
	}

	public void setupSearchView()
	{
		mSearchView.setOnQueryTextListener(mSearchComposer);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case (R.id.received_device_options_refresh):
				this.updateList();
				return true;
			case (R.id.received_device_options_open_in_file_manager):
				this.openFile(Uri.fromFile(ApplicationHelper.getApplicationDirectory(getActivity())), "*/*", getString(R.string.pick_file_manager));
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		ReceivedFilesListAdapter.FileInfo fileInfo = (ReceivedFilesListAdapter.FileInfo) getAdapter().getItem(position);

		this.openFile(Uri.fromFile(fileInfo.file), FileUtils.getFileContentType(fileInfo.file.getAbsolutePath()), getString(R.string.file_open_app_chooser_msg));
	}

	private class ChoiceListener extends MediaChoiceListener
	{
		protected ActionMode mActionMode;
		protected MenuItem mSelectAll;

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu)
		{
			if (!super.onCreateActionMode(mode, menu))
				return false;

			mode.getMenuInflater().inflate(R.menu.file_actions, menu);

			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item)
		{
			if (item.getItemId() == R.id.file_actions_delete)
			{
				FileDeleteDialogFragment df = new FileDeleteDialogFragment();

				df.setItems(mCheckedList.toArray());

				df.setOnDeleteCompletedListener(
					new FileDeleteDialogFragment.OnDeleteCompletedListener()
					{
						@Override
						public void onFilesDeleted(FileDeleteDialogFragment fragment, int fileSize)
						{
							fragment.getContext().sendBroadcast(new Intent(ServerService.ACTION_FILE_LIST_CHANGED));
						}
					}
				);

				df.show(getFragmentManager(), "delete");

				mode.finish();

				return true;
			}

			return super.onActionItemClicked(mode, item);
		}

		@Override
		public void onItemChecked(ActionMode mode, int position, long id, boolean isChecked)
		{
			ReceivedFilesListAdapter.FileInfo fileInfo = (ReceivedFilesListAdapter.FileInfo) getAdapter().getItem(position);

			if (isChecked)
				mCheckedList.add(Uri.fromFile(fileInfo.file));
			else
				mCheckedList.remove(Uri.fromFile(fileInfo.file));
		}
	}

	private class SearchComposer implements SearchView.OnQueryTextListener
	{
		@Override
		public boolean onQueryTextSubmit(String word)
		{
			search(word);
			return false;
		}

		@Override
		public boolean onQueryTextChange(String word)
		{
			search(word);
			return false;
		}
	}

	private class Receiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction() != null)
				Log.d(TAG, intent.getAction());

			if (ServerService.ACTION_FILE_LIST_CHANGED.equals(intent.getAction()))
				ReceivedFilesListFragment.this.updateInBackground();
		}
	}
}
