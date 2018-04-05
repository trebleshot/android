package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.app.ShareableListFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.FileDeletionDialog;
import com.genonbeta.TrebleShot.dialog.FileRenameDialog;
import com.genonbeta.TrebleShot.io.DocumentFile;
import com.genonbeta.TrebleShot.io.LocalDocumentFile;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.DynamicNotification;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class FileListFragment
		extends ShareableListFragment<FileListAdapter.GenericFileHolder, EditableListAdapter.EditableViewHolder, FileListAdapter>
{
	public static final String TAG = FileListFragment.class.getSimpleName();

	public static final int JOB_COPY_FILES = 0;

	public final static String ACTION_FILE_LIST_CHANGED = "com.genonbeta.TrebleShot.action.FILE_LIST_CHANGED";
	public final static String EXTRA_FILE_PARENT = "extraPath";
	public final static String EXTRA_FILE_NAME = "extraFile";
	public final static String EXTRA_FILE_LOCATION = "extraFileLocation";

	private IntentFilter mIntentFilter = new IntentFilter();
	private MediaScannerConnection mMediaScanner;
	private OnFileClickedListener mFileClickedListener;
	private OnPathChangedListener mPathChangedListener;
	private BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		private Snackbar mUpdateSnackbar;

		@Override
		public void onReceive(Context context, Intent intent)
		{
			if ((ACTION_FILE_LIST_CHANGED.equals(intent.getAction()) && intent.hasExtra(EXTRA_FILE_PARENT))) {
				try {
					final DocumentFile parentFile = FileUtils.fromUri(getContext(), (Uri) intent.getParcelableExtra(EXTRA_FILE_PARENT));

					if (getAdapter().getPath() != null && parentFile.getUri().equals(getAdapter().getPath().getUri()))
						refreshList();
					else if (intent.hasExtra(EXTRA_FILE_NAME)) {
						if (mUpdateSnackbar == null)
							mUpdateSnackbar = createSnackbar(R.string.mesg_newFilesReceived);

						mUpdateSnackbar
								.setText(getString(R.string.mesg_fileReceived, intent.getStringExtra(EXTRA_FILE_NAME)))
								.setAction(R.string.butn_show, new View.OnClickListener()
								{
									@Override
									public void onClick(View v)
									{
										goPath(parentFile);
									}
								})
								.show();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (getAdapter().getPath() == null
					&& AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())
					&& AccessDatabase.TABLE_WRITABLEPATH.equals(intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME)))
				refreshList();
		}
	};

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setDefaultOrderingCriteria(FileListAdapter.MODE_SORT_ORDER_DESCENDING);
		setDefaultSortingCriteria(FileListAdapter.MODE_SORT_BY_DATE);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		setEmptyImage(R.drawable.ic_folder_white_24dp);
		setEmptyText(getString(R.string.text_listEmptyFiles));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mMediaScanner = new MediaScannerConnection(getActivity(), null);

		mMediaScanner.connect();
		mIntentFilter.addAction(ACTION_FILE_LIST_CHANGED);
		mIntentFilter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);
	}

	@Override
	public FileListAdapter onAdapter()
	{
		final AppUtils.QuickActions<EditableListAdapter.EditableViewHolder> quickActions = new AppUtils.QuickActions<EditableListAdapter.EditableViewHolder>()
		{
			@Override
			public void onQuickActions(final EditableListAdapter.EditableViewHolder clazz)
			{
				clazz.getView().setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						FileListAdapter.GenericFileHolder fileInfo = getAdapter().getItem(clazz);

						if (mFileClickedListener == null || !mFileClickedListener.onFileClicked(fileInfo)) {
							if (fileInfo instanceof FileListAdapter.FileHolder)
								performLayoutClick(v, clazz);
							else if (fileInfo instanceof FileListAdapter.DirectoryHolder
									|| fileInfo instanceof FileListAdapter.WritablePathHolder) {
								FileListFragment.this.goPath(fileInfo.file);

								if (isSelectionActivated() && !getDefaultPreferences().getBoolean("helpFolderSelection", false))
									createSnackbar(R.string.mesg_helpFolderSelection)
											.setAction(R.string.butn_gotIt, new View.OnClickListener()
											{
												@Override
												public void onClick(View v)
												{
													getDefaultPreferences()
															.edit()
															.putBoolean("helpFolderSelection", true)
															.apply();
												}
											})
											.show();
							}
						}
					}
				});
			}
		};

		return new FileListAdapter(getActivity(), getDatabase(), getDefaultPreferences())
		{
			@NonNull
			@Override
			public EditableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
			{
				return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), quickActions);
			}
		};
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
	public void onSaveInstanceState(@NonNull Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putString(EXTRA_FILE_LOCATION, getAdapter().getPath().getUri().toString());
	}

	@Override
	public void onViewStateRestored(@Nullable Bundle savedInstanceState)
	{
		super.onViewStateRestored(savedInstanceState);

		if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_FILE_LOCATION)) {
			try {
				goPath(FileUtils.fromUri(getContext(), Uri.parse(savedInstanceState.getString(EXTRA_FILE_LOCATION))));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}


	@Override
	public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
	{
		super.onCreateActionMenu(context, actionMode, menu);
		actionMode.getMenuInflater().inflate(R.menu.action_mode_file, menu);

		MenuItem shareOthers = menu.findItem(R.id.action_mode_share_all_apps);

		//if (shareOthers != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		//shareOthers.setVisible(false);

		MenuItem ejectDirectory = menu.findItem(R.id.action_mode_file_eject_directory);

		if (ejectDirectory != null && Build.VERSION.SDK_INT >= 21)
			ejectDirectory.setVisible(true);

		return true;
	}

	@Override
	public boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item)
	{
		int id = item.getItemId();

		if (getSelectionConnection().getSelectedItemList().size() == 0)
			return super.onActionMenuItemSelected(context, actionMode, item);

		if (id == R.id.action_mode_file_delete && getAdapter().getPath() != null) {
			new FileDeletionDialog<>(getActivity(), getSelectionConnection().getSelectedItemList(), new FileDeletionDialog.Listener()
			{
				@Override
				public void onFileDeletion(WorkerService.RunningTask runningTask, Context context, DocumentFile file)
				{
					scanFile(file);
				}

				@Override
				public void onCompleted(WorkerService.RunningTask runningTask, Context context, int fileSize)
				{
					context.sendBroadcast(new Intent(ACTION_FILE_LIST_CHANGED)
							.putExtra(EXTRA_FILE_PARENT, getAdapter().getPath().getUri()));
				}
			}).show();
		} else if (id == R.id.action_mode_file_eject_directory) {
			ArrayList<FileListAdapter.GenericFileHolder> selectionList = new ArrayList<>(getSelectionConnection().getSelectedItemList());

			for (FileListAdapter.GenericFileHolder holder : selectionList)
				if (holder instanceof FileListAdapter.WritablePathHolder)
					getAdapter().getDatabase().remove(((FileListAdapter.WritablePathHolder) holder).pathObject);
		} else if (id == R.id.action_mode_file_rename) {
			new FileRenameDialog<>(getActivity(), getSelectionConnection().getSelectedItemList(), new FileRenameDialog.OnFileRenameListener()
			{
				@Override
				public void onFileRename(DocumentFile file, String displayName)
				{
					scanFile(file);
				}

				@Override
				public void onFileRenameCompleted()
				{
					refreshList();
				}
			}).show();
		} else if (id == R.id.action_mode_file_copy_here) {
			WorkerService.run(getContext(), new WorkerService.NotifiableRunningTask(TAG, JOB_COPY_FILES)
			{
				@Override
				protected void onRun()
				{

				}

				@Override
				public void onUpdateNotification(DynamicNotification dynamicNotification, UpdateType updateType)
				{

				}
			});
		} else
			return super.onActionMenuItemSelected(context, actionMode, item);

		return true;
	}

	public void goPath(DocumentFile file)
	{
		if (file != null && !file.canRead()) {
			createSnackbar(R.string.mesg_errorReadFolder, file.getName())
					.show();

			return;
		}

		if (mPathChangedListener != null)
			mPathChangedListener.onPathChanged(file);

		getAdapter().goPath(file);
		refreshList();
	}

	public boolean scanFile(DocumentFile file)
	{
		if (!(file instanceof LocalDocumentFile) || !mMediaScanner.isConnected())
			return false;

		String filePath = ((LocalDocumentFile) file).getFile().getAbsolutePath();

		mMediaScanner.scanFile(filePath, file.isDirectory() ? file.getType() : null);

		return true;
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
		boolean onFileClicked(FileListAdapter.GenericFileHolder fileInfo);
	}

	public interface OnPathChangedListener
	{
		void onPathChanged(DocumentFile file);
	}
}