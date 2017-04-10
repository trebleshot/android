package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ReceivedFilesListAdapter;
import com.genonbeta.TrebleShot.fragment.dialog.FileDeleteDialogFragment;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.FileUtils;
import com.genonbeta.TrebleShot.helper.GAnimater;
import com.genonbeta.TrebleShot.helper.NotificationPublisher;
import com.genonbeta.TrebleShot.receiver.FileChangesReceiver;
import com.genonbeta.TrebleShot.support.FragmentTitle;

public class ReceivedFilesListFragment extends AbstractEditableListFragment<ReceivedFilesListAdapter> implements FragmentTitle
{
	public static final String TAG = "ReceivedFilesListFragment";

	private NotificationPublisher mPublisher;
	private IntentFilter mIntentFilter = new IntentFilter();
	private BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (FileChangesReceiver.ACTION_FILE_LIST_CHANGED.equals(intent.getAction()))
				ReceivedFilesListFragment.this.updateInBackground();
		}
	};

	@Override
	protected ReceivedFilesListAdapter onAdapter()
	{
		return new ReceivedFilesListAdapter(getActivity());
	}

	@Override
	protected AbstractEditableListFragment.ActionModeListener onActionModeListener()
	{
		return new ChoiceListener();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mPublisher = new NotificationPublisher(getActivity());
		mIntentFilter.addAction(FileChangesReceiver.ACTION_FILE_LIST_CHANGED);

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
		inflater.inflate(R.menu.received_files_options, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case (R.id.received_device_options_refresh):
				getContext().sendBroadcast(new Intent(FileChangesReceiver.ACTION_FILE_LIST_CHANGED));
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

	@Override
	public CharSequence getFragmentTitle(Context context)
	{
		return context.getString(R.string.received_files);
	}

	private class ChoiceListener extends ActionModeListener
	{
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
			if (!super.onActionItemClicked(mode, item))
				if (item.getItemId() == R.id.file_actions_delete)
				{
					FileDeleteDialogFragment df = new FileDeleteDialogFragment();

					df.setItems(getSharedItemList().toArray());

					df.setOnDeleteCompletedListener(
							new FileDeleteDialogFragment.OnDeleteCompletedListener()
							{
								@Override
								public void onFilesDeleted(FileDeleteDialogFragment fragment, int fileSize)
								{
									fragment.getContext().sendBroadcast(new Intent(FileChangesReceiver.ACTION_FILE_LIST_CHANGED));
								}
							}
					);

					df.show(getFragmentManager(), "delete");

					mode.finish();

					return true;
				}

			return false;
		}

		@Override
		public Uri onItemChecked(ActionMode mode, int position, long id, boolean isChecked)
		{
			ReceivedFilesListAdapter.FileInfo fileInfo = (ReceivedFilesListAdapter.FileInfo) getAdapter().getItem(position);

			return Uri.fromFile(fileInfo.file);
		}
	}
}
