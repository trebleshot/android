package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ChangeStoragePathActivity;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.app.GroupEditableListFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.FileDeletionDialog;
import com.genonbeta.TrebleShot.dialog.FileRenameDialog;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.object.WritablePathObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.ui.callback.SharingActionModeCallback;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.io.LocalDocumentFile;
import com.genonbeta.android.framework.widget.PowerfulActionMode;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FileListFragment
        extends GroupEditableListFragment<FileListAdapter.GenericFileHolder, GroupEditableListAdapter.GroupViewHolder, FileListAdapter>
{
    public static final String TAG = FileListFragment.class.getSimpleName();

    public final static int REQUEST_WRITE_ACCESS = 264;
    public static final int JOB_COPY_FILES = 0;

    public final static String ACTION_FILE_LIST_CHANGED = "com.genonbeta.TrebleShot.action.FILE_LIST_CHANGED";
    public final static String EXTRA_FILE_PARENT = "extraPath";
    public final static String EXTRA_FILE_NAME = "extraFile";
    public final static String EXTRA_FILE_LOCATION = "extraFileLocation";

    private DocumentFile mLastKnownPath;
    private IntentFilter mIntentFilter = new IntentFilter();
    private MediaScannerConnection mMediaScanner;
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

        setDefaultOrderingCriteria(FileListAdapter.MODE_SORT_ORDER_ASCENDING);
        setDefaultSortingCriteria(FileListAdapter.MODE_SORT_BY_NAME);
        setDefaultGroupingCriteria(FileListAdapter.MODE_GROUP_BY_DEFAULT);
        setDefaultSelectionCallback(new SelectionCallback(this));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
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

        mIntentFilter.addAction(ACTION_FILE_LIST_CHANGED);
        mIntentFilter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK)
            switch (requestCode) {
                case REQUEST_WRITE_ACCESS:
                    Uri pathUri = data.getData();

                    if (Build.VERSION.SDK_INT >= 23 && pathUri != null) {
                        String pathString = pathUri.toString();
                        String title = null;

                        try {
                            title = FileUtils.fromUri(getContext(), pathUri).getName();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                        if (title == null)
                            title = pathString.substring(pathString.lastIndexOf(File.separator) + 1);

                        AppUtils.getDatabase(getContext())
                                .publish(new WritablePathObject(title, pathUri));

                        if (getContext() != null)
                            getContext().getContentResolver().takePersistableUriPermission(pathUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                        goPath(null);
                    }
                    break;
            }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.actions_file_list, menu);

        MenuItem mountDirectory = menu.findItem(R.id.actions_file_list_mount_directory);

        if (Build.VERSION.SDK_INT >= 23
                && mountDirectory != null)
            mountDirectory.setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.actions_file_list_mount_directory) {
            requestMountStorage();
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public FileListAdapter onAdapter()
    {
        final AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder> quickActions = new AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder>()
        {
            @Override
            public void onQuickActions(final GroupEditableListAdapter.GroupViewHolder clazz)
            {
                if (!clazz.isRepresentative()) {
                    registerLayoutViewClicks(clazz);

                    if (getSelectionConnection() != null)
                        clazz.getView().findViewById(R.id.layout_image).setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                getSelectionConnection().setSelected(clazz.getAdapterPosition());
                            }
                        });

                    clazz.getView().findViewById(R.id.menu).setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            final FileListAdapter.GenericFileHolder fileHolder = getAdapter().getList().get(clazz.getAdapterPosition());
                            boolean canWrite = fileHolder.file.canWrite();
                            boolean canRead = fileHolder.file.canRead() && fileHolder.file.isFile();
                            boolean canAlter = canWrite && !(fileHolder instanceof FileListAdapter.StorageHolder);

                            PopupMenu popupMenu = new PopupMenu(getContext(), v);
                            Menu menuItself = popupMenu.getMenu(); // Like the song Life Itself from Glass Animals, you got it?

                            popupMenu.getMenuInflater().inflate(R.menu.action_mode_file, menuItself);

                            menuItself.findItem(R.id.action_mode_file_open).setVisible(canRead);
                            menuItself.findItem(R.id.action_mode_file_rename).setEnabled(canAlter);
                            menuItself.findItem(R.id.action_mode_file_delete).setEnabled(canAlter);
                            menuItself.findItem(R.id.action_mode_file_change_save_path)
                                    .setVisible(FileUtils.getApplicationDirectory(getContext(),
                                            AppUtils.getDefaultPreferences(getContext())).getUri().equals(fileHolder.file.getUri()));

                            menuItself.findItem(R.id.action_mode_file_eject_directory)
                                    .setVisible(fileHolder instanceof FileListAdapter.WritablePathHolder);

                            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                            {
                                @Override
                                public boolean onMenuItemClick(MenuItem item)
                                {
                                    int id = item.getItemId();

                                    ArrayList<FileListAdapter.GenericFileHolder> generateSelectionList = new ArrayList<>();
                                    generateSelectionList.add(fileHolder);

                                    if (id == R.id.action_mode_file_open) {
                                        performLayoutClickOpenUri(clazz);
                                    } else if (id == R.id.action_mode_file_eject_directory
                                            && fileHolder instanceof FileListAdapter.WritablePathHolder) {
                                        AppUtils.getDatabase(getContext()).remove(((FileListAdapter.WritablePathHolder) fileHolder).pathObject);
                                    } else if (id == R.id.action_mode_file_change_save_path) {
                                        startActivity(new Intent(getContext(), ChangeStoragePathActivity.class));
                                    } else
                                        return !handleEditingAction(id, FileListFragment.this, generateSelectionList);

                                    return true;
                                }
                            });

                            popupMenu.show();
                        }
                    });
                }
            }
        };

        return new FileListAdapter(getActivity(), AppUtils.getDatabase(getContext()), AppUtils.getDefaultPreferences(getContext()))
        {
            @NonNull
            @Override
            public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                GroupViewHolder holder = super.onCreateViewHolder(parent, viewType);

                if (viewType == GroupEditableListAdapter.VIEW_TYPE_ACTION_BUTTON) {
                    registerLayoutViewClicks(holder);
                    return holder;
                }

                return AppUtils.quickAction(holder, quickActions);
            }
        };
    }

    @Override
    public boolean onDefaultClickAction(GroupEditableListAdapter.GroupViewHolder holder)
    {
        performLayoutClickOpenUri(holder);
        return true;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        getActivity().registerReceiver(mReceiver, mIntentFilter);
        mMediaScanner.connect();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
        mMediaScanner.disconnect();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (getAdapter().getPath() != null)
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

    @Override
    protected void onListRefreshed()
    {
        super.onListRefreshed();

        // Try to bring scope to the top if the user is viewing another folder
        DocumentFile pathOnTrial = getAdapter().getPath();

        if (!(mLastKnownPath == null && getAdapter().getPath() == null)
                && (mLastKnownPath != null && !mLastKnownPath.equals(pathOnTrial)))
            getListView().scrollToPosition(0);

        mLastKnownPath = pathOnTrial;
    }

    public void requestMountStorage()
    {
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_WRITE_ACCESS);
        Toast.makeText(getActivity(), R.string.mesg_mountDirectoryHelp, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean performLayoutClick(GroupEditableListAdapter.GroupViewHolder holder)
    {
        try {
            FileListAdapter.GenericFileHolder fileInfo = getAdapter().getItem(holder);

            if (fileInfo.getViewType() == GroupEditableListAdapter.VIEW_TYPE_ACTION_BUTTON
                    && fileInfo.getRequestCode() == FileListAdapter.REQUEST_CODE_MOUNT_FOLDER)
                requestMountStorage();
            else if (fileInfo instanceof FileListAdapter.FileHolder)
                return super.performLayoutClick(holder);
            else if (fileInfo instanceof FileListAdapter.DirectoryHolder
                    || fileInfo instanceof FileListAdapter.WritablePathHolder) {
                FileListFragment.this.goPath(fileInfo.file);

                if (getSelectionCallback() != null && getSelectionCallback().isSelectionActivated() && !AppUtils.getDefaultPreferences(getContext()).getBoolean("helpFolderSelection", false))
                    createSnackbar(R.string.mesg_helpFolderSelection)
                            .setAction(R.string.butn_gotIt, new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View v)
                                {
                                    AppUtils.getDefaultPreferences(getContext())
                                            .edit()
                                            .putBoolean("helpFolderSelection", true)
                                            .apply();
                                }
                            })
                            .show();
            }

            return true;
        } catch (NotReadyException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean performLayoutLongClick(GroupEditableListAdapter.GroupViewHolder holder)
    {
        try {
            FileListAdapter.GenericFileHolder fileHolder = getAdapter().getItem(holder.getAdapterPosition());

            if ((fileHolder instanceof FileListAdapter.DirectoryHolder
                    || fileHolder instanceof FileListAdapter.WritablePathHolder
            )
                    && getSelectionConnection() != null
                    && getSelectionConnection().setSelected(holder))
                return true;
        } catch (NotReadyException e) {
            e.printStackTrace();
        }

        return super.performLayoutLongClick(holder);
    }

    public boolean scanFile(DocumentFile file)
    {
        // FIXME: 9/11/18 There should be insert, remove, update
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

    public static boolean handleEditingAction(int id, final FileListFragment fragment, ArrayList<FileListAdapter.GenericFileHolder> selectedItemList)
    {
        final FileListAdapter adapter = fragment.getAdapter();

        if (id == R.id.action_mode_file_delete) {
            new FileDeletionDialog(fragment.getContext(), selectedItemList, new FileDeletionDialog.Listener()
            {
                @Override
                public void onFileDeletion(WorkerService.RunningTask runningTask, Context context, DocumentFile file)
                {
                    fragment.scanFile(file);
                }

                @Override
                public void onCompleted(WorkerService.RunningTask runningTask, Context context, int fileSize)
                {
                    context.sendBroadcast(new Intent(ACTION_FILE_LIST_CHANGED)
                            .putExtra(EXTRA_FILE_PARENT, adapter.getPath() == null
                                    ? null
                                    : adapter.getPath().getUri()));
                }
            }).show();
        } else if (id == R.id.action_mode_file_rename) {
            new FileRenameDialog<>(fragment.getContext(), selectedItemList, new FileRenameDialog.OnFileRenameListener()
            {
                @Override
                public void onFileRename(DocumentFile file, String displayName)
                {
                    fragment.scanFile(file);
                }

                @Override
                public void onFileRenameCompleted(Context context)
                {
                    context.sendBroadcast(new Intent(ACTION_FILE_LIST_CHANGED)
                            .putExtra(EXTRA_FILE_PARENT, adapter.getPath() == null
                                    ? null
                                    : adapter.getPath().getUri()));
                }
            }).show();
        } else if (id == R.id.action_mode_file_copy_here) {
            WorkerService.run(fragment.getContext(), new WorkerService.RunningTask(TAG, JOB_COPY_FILES)
            {
                @Override
                protected void onRun()
                {

                }
            });
        } else
            return false;

        return true;
    }

    public interface OnPathChangedListener
    {
        void onPathChanged(DocumentFile file);
    }

    private static class SelectionCallback extends SharingActionModeCallback<FileListAdapter.GenericFileHolder>
    {
        private FileListFragment mFragment;

        public SelectionCallback(FileListFragment fragment)
        {
            super(fragment);
            mFragment = fragment;
        }

        @Override
        public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
        {
            super.onCreateActionMenu(context, actionMode, menu);
            actionMode.getMenuInflater().inflate(R.menu.action_mode_file, menu);
            return true;
        }

        @Override
        public boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item)
        {
            int id = item.getItemId();

            if (getFragment().getSelectionConnection().getSelectedItemList().size() == 0)
                return super.onActionMenuItemSelected(context, actionMode, item);

            if (!handleEditingAction(id, mFragment, getFragment().getSelectionConnection().getSelectedItemList()))
                return super.onActionMenuItemSelected(context, actionMode, item);

            return true;
        }
    }
}