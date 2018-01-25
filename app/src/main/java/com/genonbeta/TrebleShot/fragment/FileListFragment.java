package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.app.ShareableListFragment;
import com.genonbeta.TrebleShot.dialog.FileDeleteDialog;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;

import java.io.File;

public class FileListFragment extends ShareableListFragment<FileListAdapter.FileHolder, FileListAdapter>
{
	public static final String TAG = FileListFragment.class.getSimpleName();

	public final static String ACTION_FILE_LIST_CHANGED = "com.genonbeta.TrebleShot.action.FILE_LIST_CHANGED";
	public final static String EXTRA_PATH = "path";

	private IntentFilter mIntentFilter = new IntentFilter();
	private MediaScannerConnection mMediaScanner;
	private OnFileClickedListener mFileClickedListener;
	private OnPathChangedListener mPathChangedListener;
	private BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (ACTION_FILE_LIST_CHANGED.equals(intent.getAction()) && intent.hasExtra(EXTRA_PATH)) {
				final String extraPath = intent.getStringExtra(EXTRA_PATH);

				if (extraPath.equals(getAdapter().getPath().getAbsolutePath()))
					refreshList();
				else
					createSnackbar(R.string.mesg_newFilesReceived)
							.setAction(R.string.butn_show, new View.OnClickListener()
							{
								@Override
								public void onClick(View v)
								{
									goPath(new File(extraPath));
								}
							})
							.show();
			}
		}
	};

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mMediaScanner = new MediaScannerConnection(getActivity(), null);

		mMediaScanner.connect();
		mIntentFilter.addAction(ACTION_FILE_LIST_CHANGED);

		if (getAdapter().getPath() == null)
			goPath(FileUtils.getApplicationDirectory(getActivity()));
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
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		FileListAdapter.FileHolder fileInfo = (FileListAdapter.FileHolder) getAdapter().getItem(position);

		if (mFileClickedListener == null || !mFileClickedListener.onFileClicked(fileInfo)) {
			if (!fileInfo.isFolder)
				super.onListItemClick(l, v, position, id);
			else {
				goPath(fileInfo.file);

				if (isSelectionActivated() && !PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("helpFolderSelection", false))
					createSnackbar(R.string.mesg_helpFolderSelection)
							.setAction(R.string.butn_gotIt, new View.OnClickListener()
							{
								@Override
								public void onClick(View v)
								{
									PreferenceManager.getDefaultSharedPreferences(getActivity())
											.edit()
											.putBoolean("helpFolderSelection", true)
											.apply();
								}
							})
							.show();
			}
 		}
	}

	@Override
	public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
	{
		super.onCreateActionMenu(context, actionMode, menu);
		actionMode.getMenuInflater().inflate(R.menu.action_mode_file, menu);

		MenuItem shareOthers = menu.findItem(R.id.action_mode_share_all_apps);

		if (shareOthers != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			shareOthers.setVisible(false);

		return true;
	}

	@Override
	public boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item)
	{
		if (item.getItemId() == R.id.action_mode_file_delete && getAdapter().getPath() != null) {
			new FileDeleteDialog<>(getActivity(), getSelectionConnection().getSelectedItemList(), new FileDeleteDialog.Listener()
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
							.putExtra(EXTRA_PATH, getAdapter().getPath().getAbsolutePath()));
				}
			}).show();

			return true;
		}

		return super.onActionMenuItemSelected(context, actionMode, item);
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

	public interface OnFileClickedListener
	{
		boolean onFileClicked(FileListAdapter.FileHolder fileInfo);
	}

	public interface OnPathChangedListener
	{
		void onPathChanged(File file);
	}
}