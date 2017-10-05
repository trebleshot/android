package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.dialog.FileDeleteDialog;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.FileUtils;
import com.genonbeta.TrebleShot.helper.GAnimater;

import java.io.File;

public class FileListFragment extends AbstractEditableListFragment<FileListAdapter.FileInfo, FileListAdapter>
{
	public static final String TAG = FileListFragment.class.getSimpleName();

	public final static String ACTION_FILE_LIST_CHANGED = "com.genonbeta.TrebleShot.action.FILE_LIST_CHANGED";
	public final static String EXTRA_KEEP_CURRENT = "keepCurrent";

	private IntentFilter mIntentFilter = new IntentFilter();
	private MediaScannerConnection mMediaScanner;
	private OnFileClickedListener mFileClickedListener;
	private OnPathChangedListener mPathChangedListener;
	private BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (ACTION_FILE_LIST_CHANGED.equals(intent.getAction())) {
				if (!intent.hasExtra(EXTRA_KEEP_CURRENT))
					getAdapter().goDefault();

				refreshList();
			}
		}
	};

	@Override
	protected ActionModeListener onActionModeListener()
	{
		return new ChoiceListener();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mMediaScanner = new MediaScannerConnection(getActivity(), null);

		mMediaScanner.connect();
		mIntentFilter.addAction(ACTION_FILE_LIST_CHANGED);

		GAnimater.applyLayoutAnimation(getListView(), GAnimater.APPEAR);
	}

	@Override
	public FileListAdapter onAdapter()
	{
		return new FileListAdapter(getActivity());
	}

	@Override
	public void onResume()
	{
		super.onResume();
		getActivity().registerReceiver(mReceiver, mIntentFilter);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getActivity().unregisterReceiver(mReceiver);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		mMediaScanner.disconnect();
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
		switch (item.getItemId()) {
			case (R.id.received_device_options_open_in_file_manager):
				openFile(Uri.fromFile(ApplicationHelper.getApplicationDirectory(getActivity())), "*/*", getString(R.string.text_chooseFileManager));
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		FileListAdapter.FileInfo fileInfo = (FileListAdapter.FileInfo) getAdapter().getItem(position);

		if (mFileClickedListener == null || !mFileClickedListener.onFileClicked(fileInfo))
			if (fileInfo.file.isFile())
				openFile(Uri.fromFile(fileInfo.file), FileUtils.getFileContentType(fileInfo.file.getAbsolutePath()), getString(R.string.text_fileOpenAppChoose));
			else
				goPath(fileInfo.file);
	}

	public void goPath(File file)
	{
		if (mPathChangedListener != null)
			mPathChangedListener.onPathChanged(file);

		getAdapter().goPath(file);
		refreshList();
	}

	public void setOnPathChangedListener(OnPathChangedListener pathChangedListener)
	{
		mPathChangedListener = pathChangedListener;
	}

	public void setOnFileClickedListener(OnFileClickedListener fileClickedListener)
	{
		mFileClickedListener = fileClickedListener;
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
		public boolean onActionItemClicked(final ActionMode mode, MenuItem item)
		{
			if (!super.onActionItemClicked(mode, item))
				if (item.getItemId() == R.id.file_actions_delete) {
					new FileDeleteDialog(getActivity(), getSharedItemList().toArray(), new FileDeleteDialog.Listener()
					{
						@Override
						public void onFileDeletion(Context context, File file)
						{
							if (mMediaScanner.isConnected())
								mMediaScanner.scanFile(file.getAbsolutePath(), "*/*");
						}

						@Override
						public void onCompleted(Context context, int fileSize)
						{
							context.sendBroadcast(new Intent(ACTION_FILE_LIST_CHANGED)
									.putExtra(EXTRA_KEEP_CURRENT, true));
						}
					}).show();

					mode.finish();

					return true;
				}

			return false;
		}

		@Override
		public Uri onItemChecked(ActionMode mode, int position, long id, boolean isChecked)
		{
			FileListAdapter.FileInfo fileInfo = (FileListAdapter.FileInfo) getAdapter().getItem(position);
			return Uri.fromFile(fileInfo.file);
		}

		@Override
		protected boolean onItemCheckable(int position)
		{
			FileListAdapter.FileInfo fileInfo = (FileListAdapter.FileInfo) getAdapter().getItem(position);
			return getAdapter().getPath().equals(fileInfo.file.getParentFile());
		}
	}

	public interface OnFileClickedListener
	{
		boolean onFileClicked(FileListAdapter.FileInfo fileInfo);
	}

	public interface OnPathChangedListener
	{
		void onPathChanged(File file);
	}
}
