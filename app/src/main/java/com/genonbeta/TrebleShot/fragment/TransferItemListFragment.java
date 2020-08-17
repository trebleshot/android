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

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.TrebleShot.App;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.FilePickerActivity;
import com.genonbeta.TrebleShot.adapter.TransferItemListAdapter;
import com.genonbeta.TrebleShot.adapter.TransferItemListAdapter.StorageStatusItem;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.app.GroupEditableListFragment;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.ChooseMemberDialog;
import com.genonbeta.TrebleShot.dialog.DialogUtils;
import com.genonbeta.TrebleShot.dialog.TransferInfoDialog;
import com.genonbeta.TrebleShot.object.IndexOfTransferGroup;
import com.genonbeta.TrebleShot.object.LoadedMember;
import com.genonbeta.TrebleShot.object.Transfer;
import com.genonbeta.TrebleShot.object.TransferItem;
import com.genonbeta.TrebleShot.task.ChangeSaveDirectoryTask;
import com.genonbeta.TrebleShot.ui.callback.TitleProvider;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.Transfers;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.framework.object.Selectable;
import com.genonbeta.android.framework.ui.PerformerMenu;
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine;
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TransferItemListFragment extends GroupEditableListFragment<TransferItemListAdapter.GenericItem,
        GroupEditableListAdapter.GroupViewHolder, TransferItemListAdapter> implements TitleProvider,
        Activity.OnBackPressedListener
{
    public static final String TAG = "TransferListFragment";

    public static final String ARG_DEVICE_ID = "argDeviceId";
    public static final String ARG_TRANSFER_ID = "argGroupId";
    public static final String ARG_TYPE = "argType";
    public static final String ARG_PATH = "argPath";

    public static final int REQUEST_CHOOSE_FOLDER = 1;

    private Transfer mTransfer;
    private IndexOfTransferGroup mIndex;
    private String mLastKnownPath;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (Kuick.ACTION_DATABASE_CHANGE.equals(intent.getAction())) {
                Kuick.BroadcastData data = Kuick.toData(intent);
                if (Kuick.TABLE_TRANSFERITEM.equals(data.tableName) || Kuick.TABLE_TRANSFER.equals(data.tableName))
                    refreshList();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setFilteringSupported(true);
        setDefaultOrderingCriteria(TransferItemListAdapter.MODE_SORT_ORDER_ASCENDING);
        setDefaultSortingCriteria(TransferItemListAdapter.MODE_SORT_BY_NAME);
        setDefaultGroupingCriteria(TransferItemListAdapter.MODE_GROUP_BY_DEFAULT);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setListAdapter(new TransferItemListAdapter(this));
        setEmptyListImage(R.drawable.ic_compare_arrows_white_24dp);

        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_TRANSFER_ID)) {
            goPath(args.getString(ARG_PATH), args.getLong(ARG_TRANSFER_ID), args.getString(ARG_DEVICE_ID),
                    args.getString(ARG_TYPE));
        }
    }

    @Nullable
    @Override
    public PerformerMenu onCreatePerformerMenu(Context context)
    {
        return new PerformerMenu(context, new SelectionCallback(getActivity(), this));
    }

    @Override
    public void onResume()
    {
        super.onResume();
        requireContext().registerReceiver(mReceiver, new IntentFilter(Kuick.ACTION_DATABASE_CHANGE));
    }

    @Override
    public void onPause()
    {
        super.onPause();
        requireContext().unregisterReceiver(mReceiver);
    }

    @Override
    public int onGridSpanSize(int viewType, int currentSpanSize)
    {
        return viewType == TransferItemListAdapter.VIEW_TYPE_REPRESENTATIVE ? currentSpanSize
                : super.onGridSpanSize(viewType, currentSpanSize);
    }

    @Override
    protected void onListRefreshed()
    {
        super.onListRefreshed();

        String pathOnTrial = getAdapter().getPath();

        if (!(mLastKnownPath == null && getAdapter().getPath() == null)
                && (mLastKnownPath != null && !mLastKnownPath.equals(pathOnTrial)))
            getListView().scrollToPosition(0);

        mLastKnownPath = pathOnTrial;
    }

    @Override
    public boolean onBackPressed()
    {
        String path = getAdapter().getPath();

        if (path == null)
            return false;

        int slashPos = path.lastIndexOf(File.separator);

        goPath(slashPos == -1 && path.length() > 0 ? null : path.substring(0, slashPos));

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null && resultCode == Activity.RESULT_OK && requestCode == REQUEST_CHOOSE_FOLDER
                && data.hasExtra(FilePickerActivity.EXTRA_CHOSEN_PATH)) {
            final Uri selectedPath = data.getParcelableExtra(FilePickerActivity.EXTRA_CHOSEN_PATH);

            if (selectedPath == null) {
                createSnackbar(R.string.mesg_somethingWentWrong).show();
            } else if (selectedPath.toString().equals(getTransfer().savePath)) {
                createSnackbar(R.string.mesg_pathSameError).show();
            } else {
                ChangeSaveDirectoryTask task = new ChangeSaveDirectoryTask(mTransfer, selectedPath);
                new AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.ques_checkOldFiles)
                        .setMessage(R.string.text_checkOldFiles)
                        .setNeutralButton(R.string.butn_cancel, null)
                        .setNegativeButton(R.string.butn_skip, (dialogInterface, i) -> App.run(requireActivity(),
                                task.setSkipMoving(true)))
                        .setPositiveButton(R.string.butn_proceed, (dialogInterface, i) -> App.run(requireActivity(), task))
                        .show();
            }
        }
    }

    public void changeSavePath(String initialPath)
    {
        startActivityForResult(new Intent(getActivity(), FilePickerActivity.class)
                .setAction(FilePickerActivity.ACTION_CHOOSE_DIRECTORY)
                .putExtra(FilePickerActivity.EXTRA_START_PATH, initialPath)
                .putExtra(FilePickerActivity.EXTRA_ACTIVITY_TITLE, getString(R.string.butn_saveTo)), REQUEST_CHOOSE_FOLDER);
    }

    @Override
    public CharSequence getDistinctiveTitle(Context context)
    {
        return context.getString(R.string.text_transfers);
    }

    public Transfer getTransfer()
    {
        if (mTransfer == null) {
            Bundle arguments = getArguments();
            if (arguments != null) {
                mTransfer = new Transfer(arguments.getLong(ARG_TRANSFER_ID, -1));
                try {
                    AppUtils.getKuick(getContext()).reconstruct(mTransfer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return mTransfer;
    }

    public IndexOfTransferGroup getIndex()
    {
        if (mIndex == null)
            mIndex = new IndexOfTransferGroup(getTransfer());
        return mIndex;
    }

    public void goPath(String path, long transferId, String deviceId, String type)
    {
        if (deviceId != null && type != null)
            try {
                LoadedMember member = new LoadedMember(transferId, deviceId, TransferItem.Type.valueOf(type));

                AppUtils.getKuick(getContext()).reconstruct(member);
                Transfers.loadMemberInfo(getContext(), member);

                getAdapter().setMember(member);
            } catch (Exception ignored) {
            }

        goPath(path, transferId);
    }

    public void goPath(String path, long transferId)
    {
        getAdapter().setTransferId(transferId);
        goPath(path);
    }

    public void goPath(String path)
    {
        getAdapter().setPath(path);
        refreshList();
    }

    @Override
    public boolean performDefaultLayoutClick(GroupEditableListAdapter.GroupViewHolder holder,
                                             TransferItemListAdapter.GenericItem object)
    {
        if (object instanceof TransferItemListAdapter.DetailsTransferFolder) {
            final List<LoadedMember> list = Transfers.loadMemberList(getContext(), getTransfer().id, null);

            if (list.size() > 0) {
                DialogInterface.OnClickListener listClickListener = (dialog, which) -> {
                    getAdapter().setMember(list.get(which));
                    getAdapter().setPath(getAdapter().getPath());
                    refreshList();
                };

                DialogInterface.OnClickListener noLimitListener = (dialog, which) -> {
                    getAdapter().setMember(null);
                    getAdapter().setPath(getAdapter().getPath());
                    refreshList();
                };

                ChooseMemberDialog dialog = new ChooseMemberDialog(requireActivity(), list, listClickListener);

                dialog.setTitle(R.string.text_limitTo)
                        .setNeutralButton(R.string.butn_showAll, noLimitListener)
                        .show();
            } else
                createSnackbar(R.string.text_noDeviceForTransfer).show();
        } else if (object instanceof StorageStatusItem) {
            final StorageStatusItem statusItem = (StorageStatusItem) object;

            if (statusItem.hasIssues(getAdapter()))
                new AlertDialog.Builder(requireActivity())
                        .setMessage(getString(R.string.mesg_notEnoughSpace))
                        .setNegativeButton(R.string.butn_close, null)
                        .setPositiveButton(R.string.butn_saveTo, (dialog, which) -> changeSavePath(statusItem.directory))
                        .show();
            else
                changeSavePath(statusItem.directory);
        } else if (object instanceof TransferItemListAdapter.TransferFolder) {
            getAdapter().setPath(object.directory);
            refreshList();
            AppUtils.showFolderSelectionHelp(this);
        } else
            new TransferInfoDialog(requireActivity(), getIndex(), object, getAdapter().getDeviceId()).show();

        return true;
    }

    @Override
    public boolean setItemSelected(GroupEditableListAdapter.GroupViewHolder holder)
    {
        if (getAdapterImpl().getItem(holder.getAdapterPosition()) instanceof TransferItemListAdapter.TransferFolder)
            return false;
        return super.setItemSelected(holder);
    }

    public void updateSavePath(String selectedPath)
    {
        requireActivity().runOnUiThread(() -> createSnackbar(R.string.mesg_pathSaved).show());
    }

    private static class SelectionCallback extends EditableListFragment.SelectionCallback
    {
        public SelectionCallback(android.app.Activity activity, PerformerEngineProvider provider)
        {
            super(activity, provider);
        }

        @Override
        public boolean onPerformerMenuList(PerformerMenu performerMenu, MenuInflater inflater, Menu targetMenu)
        {
            super.onPerformerMenuList(performerMenu, inflater, targetMenu);
            inflater.inflate(R.menu.action_mode_transfer, targetMenu);
            return true;
        }

        @Override
        public boolean onPerformerMenuSelected(PerformerMenu performerMenu, MenuItem item)
        {
            int id = item.getItemId();
            IPerformerEngine engine = getPerformerEngine();

            if (engine == null)
                return false;

            List<Selectable> genericList = new ArrayList<>(engine.getSelectionList());
            List<TransferItemListAdapter.GenericItem> selectionList = new ArrayList<>();

            for (Selectable selectable : genericList)
                if (selectable instanceof TransferItemListAdapter.GenericItem)
                    selectionList.add((TransferItemListAdapter.GenericItem) selectable);

            if (id == R.id.action_mode_transfer_delete) {
                DialogUtils.showRemoveTransferObjectListDialog(getActivity(), selectionList);
                return true;
            } else
                return super.onPerformerMenuSelected(performerMenu, item);
        }
    }
}
