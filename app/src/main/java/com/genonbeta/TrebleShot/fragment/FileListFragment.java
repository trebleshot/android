/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ChangeStoragePathActivity;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.adapter.FileListAdapter.FileHolder;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.app.GroupEditableListFragment;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.FileDeletionDialog;
import com.genonbeta.TrebleShot.dialog.FileRenameDialog;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.io.LocalDocumentFile;
import com.google.android.material.snackbar.Snackbar;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

abstract public class FileListFragment extends GroupEditableListFragment<FileHolder,
        GroupEditableListAdapter.GroupViewHolder, FileListAdapter>
{
    public static final String TAG = FileListFragment.class.getSimpleName();

    public final static int REQUEST_WRITE_ACCESS = 264;

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
                    Object parentUri = intent.getParcelableExtra(EXTRA_FILE_PARENT);

                    if (parentUri == null && getAdapter().getPath() == null) {
                        refreshList();
                    } else if (parentUri != null) {
                        final DocumentFile parentFile = FileUtils.fromUri(getContext(), (Uri) parentUri);

                        if (getAdapter().getPath() != null && parentFile.getUri().equals(getAdapter().getPath().getUri()))
                            refreshList();
                        else if (intent.hasExtra(EXTRA_FILE_NAME)) {
                            if (mUpdateSnackbar == null)
                                mUpdateSnackbar = createSnackbar(R.string.mesg_newFilesReceived);

                            mUpdateSnackbar
                                    .setText(getString(R.string.mesg_fileReceived, intent.getStringExtra(EXTRA_FILE_NAME)))
                                    .setAction(R.string.butn_show, v -> goPath(parentFile))
                                    .show();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (getAdapter().getPath() == null && Kuick.ACTION_DATABASE_CHANGE.equals(intent.getAction())) {
                Kuick.BroadcastData data = Kuick.toData(intent);
                if (Kuick.TABLE_FILEBOOKMARK.equals(data.tableName))
                    refreshList();
            }
        }
    };

    public static boolean handleEditingAction(int id, final FileListFragment fragment,
                                              List<FileHolder> selectedItemList)
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
                            .putExtra(EXTRA_FILE_PARENT, adapter.getPath() == null ? null
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
                            .putExtra(EXTRA_FILE_PARENT, adapter.getPath() == null ? null
                                    : adapter.getPath().getUri()));
                }
            }).show();
        } else if (id == R.id.action_mode_file_copy_here) {
            //todo: implement file copying
        } else
            return false;

        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setFilteringSupported(true);
        setDefaultOrderingCriteria(FileListAdapter.MODE_SORT_ORDER_ASCENDING);
        setDefaultSortingCriteria(FileListAdapter.MODE_SORT_BY_NAME);
        setDefaultGroupingCriteria(FileListAdapter.MODE_GROUP_BY_DEFAULT);
        // TODO: 22.02.2020 Implement default selection callback installation
        //setDefaultSelectionCallback(new SelectionCallback(this));
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
        mIntentFilter.addAction(Kuick.ACTION_DATABASE_CHANGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK)
            if (requestCode == REQUEST_WRITE_ACCESS) {
                Uri pathUri = data.getData();

                if (Build.VERSION.SDK_INT >= 21 && pathUri != null && getContext() != null) {
                    getContext().getContentResolver().takePersistableUriPermission(pathUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    try {
                        Kuick kuick = AppUtils.getKuick(getContext());
                        DocumentFile file = DocumentFile.fromUri(getContext(), pathUri, true);
                        kuick.publish(new FileHolder(getContext(), file));
                        kuick.broadcast();
                        goPath(null);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show();
                    }
                }
            }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.actions_file_list, menu);

        MenuItem mountDirectory = menu.findItem(R.id.actions_file_list_mount_directory);

        if (Build.VERSION.SDK_INT >= 21 && mountDirectory != null)
            mountDirectory.setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.actions_file_list_mount_directory) {
            requestMountStorage();
        } else if (id == R.id.actions_file_list_toggle_shortcut && getAdapter().getPath() != null) {
            shortcutItem(new FileHolder(getContext(), getAdapter().getPath()));
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

        MenuItem shortcutMenuItem = menu.findItem(R.id.actions_file_list_toggle_shortcut);

        if (shortcutMenuItem != null) {
            boolean hasPath = getAdapter().getPath() != null;
            shortcutMenuItem.setEnabled(hasPath);

            if (hasPath)
                try {
                    AppUtils.getKuick(getContext()).reconstruct(new FileHolder(getContext(), getAdapter().getPath()));
                    shortcutMenuItem.setTitle(R.string.butn_removeShortcut);
                } catch (Exception e) {
                    shortcutMenuItem.setTitle(R.string.butn_addShortcut);
                }
        }
    }

    @Override
    public FileListAdapter onAdapter()
    {
        final AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder> quickActions = clazz -> {
            if (!clazz.isRepresentative()) {
                registerLayoutViewClicks(clazz);

                clazz.itemView.findViewById(R.id.layout_image).setOnClickListener(v -> setItemSelected(clazz,
                        true));
                clazz.itemView.findViewById(R.id.menu).setOnClickListener(v -> {
                    final FileHolder fileHolder = getAdapter().getList().get(clazz.getAdapterPosition());
                    boolean isFile = FileHolder.Type.File.equals(fileHolder.getType());
                    boolean isMounted = FileHolder.Type.Mounted.equals(fileHolder.getType());
                    boolean isBookmarked = FileHolder.Type.Bookmarked.equals(fileHolder.getType());
                    boolean canWrite = fileHolder.file != null && fileHolder.file.canWrite();
                    boolean canRead = fileHolder.file != null && fileHolder.file.canRead();

                    PopupMenu popupMenu = new PopupMenu(getContext(), v);
                    Menu menuItself = popupMenu.getMenu();

                    popupMenu.getMenuInflater().inflate(R.menu.action_mode_file, menuItself);

                    menuItself.findItem(R.id.action_mode_file_open).setVisible(canRead && isFile);
                    menuItself.findItem(R.id.action_mode_file_rename).setEnabled(canWrite || isMounted || isBookmarked);
                    menuItself.findItem(R.id.action_mode_file_delete).setEnabled(canWrite && !isMounted);
                    menuItself.findItem(R.id.action_mode_file_show).setVisible(FileHolder.Type.Recent.equals(
                            fileHolder.getType()));
                    menuItself.findItem(R.id.action_mode_file_change_save_path).setVisible(
                            FileHolder.Type.SaveLocation.equals(fileHolder.getType())
                                    || (fileHolder.file != null && FileUtils.getApplicationDirectory(getContext())
                            .equals(fileHolder.file)));
                    menuItself.findItem(R.id.action_mode_file_eject_directory).setVisible(isMounted);
                    menuItself.findItem(R.id.action_mode_file_toggle_shortcut).setVisible(!isFile && !isMounted)
                            .setTitle(isBookmarked ? R.string.butn_removeShortcut : R.string.butn_addShortcut);

                    popupMenu.setOnMenuItemClickListener(item -> {
                        int id = item.getItemId();

                        ArrayList<FileHolder> generateSelectionList = new ArrayList<>();
                        generateSelectionList.add(fileHolder);

                        if (id == R.id.action_mode_file_open) {
                            performLayoutClickOpen(clazz);
                        } else if (id == R.id.action_mode_file_show && fileHolder.file.getParentFile() != null) {
                            goPath(fileHolder.file.getParentFile());
                        } else if (id == R.id.action_mode_file_eject_directory) {
                            AppUtils.getKuick(getContext()).remove(fileHolder);
                            AppUtils.getKuick(getContext()).broadcast();
                        } else if (id == R.id.action_mode_file_toggle_shortcut) {
                            shortcutItem(fileHolder);
                        } else if (id == R.id.action_mode_file_change_save_path) {
                            startActivity(new Intent(getContext(), ChangeStoragePathActivity.class));
                        } else
                            return !handleEditingAction(id, FileListFragment.this, generateSelectionList);

                        return true;
                    });

                    popupMenu.show();
                });
            }
        };

        return new FileListAdapter(getActivity())
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
        performLayoutClickOpen(holder);
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

    @Override
    protected void onListRefreshed()
    {
        super.onListRefreshed();

        // If the current path is different from the older one, move the scroll position
        // to the top.
        DocumentFile pathOnTrial = getAdapter().getPath();

        if (!(mLastKnownPath == null && getAdapter().getPath() == null)
                && (mLastKnownPath != null && !mLastKnownPath.equals(pathOnTrial)))
            getListView().scrollToPosition(0);

        mLastKnownPath = pathOnTrial;
    }

    protected void shortcutItem(FileHolder holder)
    {
        Kuick kuick = AppUtils.getKuick(getContext());

        try {
            kuick.reconstruct(holder);
            kuick.remove(holder);

            createSnackbar(R.string.mesg_removed).show();
        } catch (Exception e) {
            kuick.insert(holder);
            createSnackbar(R.string.mesg_added).show();
        } finally {
            kuick.broadcast();
        }
    }

    @Override
    public Snackbar createSnackbar(int resId, Object... objects)
    {
        return Snackbar.make(getListView(), getString(resId, objects), Snackbar.LENGTH_SHORT);
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

    public void requestMountStorage()
    {
        if (Build.VERSION.SDK_INT < 21)
            return;

        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_WRITE_ACCESS);
        Toast.makeText(getActivity(), R.string.mesg_mountDirectoryHelp, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean performLayoutClick(GroupEditableListAdapter.GroupViewHolder holder)
    {
        try {
            FileHolder fileInfo = getAdapter().getItem(holder);

            if (fileInfo.getViewType() == GroupEditableListAdapter.VIEW_TYPE_ACTION_BUTTON
                    && fileInfo.getRequestCode() == FileListAdapter.REQUEST_CODE_MOUNT_FOLDER)
                requestMountStorage();
            else if (fileInfo.file.isDirectory()) {
                FileListFragment.this.goPath(fileInfo.file);

                if (getEngineConnection().getSelectedItemList().size() > 0
                        && !AppUtils.getDefaultPreferences(getContext()).getBoolean("helpFolderSelection",
                        false))
                    createSnackbar(R.string.mesg_helpFolderSelection)
                            .setAction(R.string.butn_gotIt, v -> AppUtils.getDefaultPreferences(getContext())
                                    .edit()
                                    .putBoolean("helpFolderSelection", true)
                                    .apply())
                            .show();
            } else
                return super.performLayoutClick(holder);

            return true;
        } catch (NotReadyException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean performLayoutClickOpen(GroupEditableListAdapter.GroupViewHolder holder)
    {
        try {
            return FileUtils.openUriForeground(getActivity(), getAdapter().getItem(holder).file);
        } catch (NotReadyException e) {
            // do nothing
        }

        return super.performLayoutClickOpen(holder);
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

    public interface OnPathChangedListener
    {
        void onPathChanged(DocumentFile file);
    }

    /*

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

            if (getFragment().getEngineConnection().getSelectedItemList().size() == 0)
                return super.onActionMenuItemSelected(context, actionMode, item);

            if (!handleEditingAction(id, mFragment, getFragment().getEngineConnection().getSelectedItemList()))
                return super.onActionMenuItemSelected(context, actionMode, item);

            return true;
        }
    }*/
}