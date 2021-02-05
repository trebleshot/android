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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.App;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.TransferDetailActivity;
import com.genonbeta.TrebleShot.adapter.TransferListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.app.GroupEditableListFragment;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.DialogUtils;
import com.genonbeta.TrebleShot.dataobject.TransferIndex;
import com.genonbeta.TrebleShot.dataobject.Transfer;
import com.genonbeta.TrebleShot.task.FileTransferTask;
import com.genonbeta.TrebleShot.ui.callback.IconProvider;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ListUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.framework.object.Selectable;
import com.genonbeta.android.framework.ui.PerformerMenu;
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine;
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * created by: Veli
 * date: 10.11.2017 00:15
 */

public class TransferListFragment extends GroupEditableListFragment<TransferIndex,
        GroupEditableListAdapter.GroupViewHolder, TransferListAdapter> implements IconProvider
{
    private final IntentFilter mFilter = new IntentFilter();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (Kuick.ACTION_DATABASE_CHANGE.equals(intent.getAction())) {
                Kuick.BroadcastData data = Kuick.toData(intent);
                if (data != null && (Kuick.TABLE_TRANSFER.equals(data.tableName)
                        || Kuick.TABLE_TRANSFERITEM.equals(data.tableName)))
                    refreshList();
            } else if (App.ACTION_TASK_CHANGE.equals(intent.getAction()))
                updateTasks();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setFilteringSupported(true);
        setDefaultOrderingCriteria(TransferListAdapter.MODE_SORT_ORDER_DESCENDING);
        setDefaultSortingCriteria(TransferListAdapter.MODE_SORT_BY_DATE);
        setDefaultGroupingCriteria(TransferListAdapter.MODE_GROUP_BY_DATE);
        setItemOffsetDecorationEnabled(true);
        setItemOffsetForEdgesEnabled(true);
        setDefaultItemOffsetPadding(getResources().getDimension(R.dimen.padding_list_content_parent_layout));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setListAdapter(new TransferListAdapter(this));
        setEmptyListImage(R.drawable.ic_compare_arrows_white_24dp);
        setEmptyListText(getString(R.string.text_listEmptyTransfer));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        mFilter.addAction(Kuick.ACTION_DATABASE_CHANGE);
        mFilter.addAction(App.ACTION_TASK_CHANGE);
    }

    @Nullable
    @Override
    public PerformerMenu onCreatePerformerMenu(Context context)
    {
        return new PerformerMenu(getContext(), new SelectionCallback(getActivity(), this));
    }

    @Override
    public void onResume()
    {
        super.onResume();
        requireContext().registerReceiver(mReceiver, mFilter);
        updateTasks();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        requireContext().unregisterReceiver(mReceiver);
    }

    @Override
    public void onSortingOptions(Map<String, Integer> options)
    {
        options.put(getString(R.string.text_sortByDate), TransferListAdapter.MODE_SORT_BY_DATE);
        options.put(getString(R.string.text_sortBySize), TransferListAdapter.MODE_SORT_BY_SIZE);
    }

    @Override
    public void onGroupingOptions(Map<String, Integer> options)
    {
        options.put(getString(R.string.text_groupByNothing), TransferListAdapter.MODE_GROUP_BY_NOTHING);
        options.put(getString(R.string.text_groupByDate), TransferListAdapter.MODE_GROUP_BY_DATE);
    }

    @Override
    public CharSequence getDistinctiveTitle(Context context)
    {
        return context.getString(R.string.text_transfers);
    }

    @Override
    public int getIconRes()
    {
        return R.drawable.ic_swap_vert_white_24dp;
    }

    @Override
    public boolean performDefaultLayoutClick(GroupEditableListAdapter.GroupViewHolder holder,
                                             TransferIndex object)
    {
        TransferDetailActivity.startInstance(requireActivity(), object.transfer);
        return true;
    }

    public void updateTasks()
    {
        try {
            List<FileTransferTask> tasks = App.from(requireActivity()).getTaskListOf(FileTransferTask.class);
            List<Long> activeTaskList = new ArrayList<>();
            for (FileTransferTask task : tasks)
                if (task.transfer != null)
                    activeTaskList.add(task.transfer.id);

            getAdapter().updateActiveList(activeTaskList);
            refreshList();
        } catch (IllegalStateException ignored) {

        }
    }

    private static class SelectionCallback extends EditableListFragment.SelectionCallback
    {

        public SelectionCallback(Activity activity, PerformerEngineProvider provider)
        {
            super(activity, provider);
        }

        @Override
        public boolean onPerformerMenuList(PerformerMenu performerMenu, MenuInflater inflater, Menu targetMenu)
        {
            super.onPerformerMenuList(performerMenu, inflater, targetMenu);
            inflater.inflate(R.menu.action_mode_group, targetMenu);
            return true;
        }

        @Override
        public boolean onPerformerMenuSelected(PerformerMenu performerMenu, MenuItem item)
        {
            int id = item.getItemId();
            Kuick kuick = AppUtils.getKuick(getActivity());
            IPerformerEngine engine = getPerformerEngine();

            if (engine == null)
                return false;

            List<Selectable> genericList = new ArrayList<>(engine.getSelectionList());
            List<TransferIndex> indexList = ListUtils.typedListOf(genericList, TransferIndex.class);

            if (id == R.id.action_mode_group_delete) {
                List<Transfer> groupList = new ArrayList<>();
                for (TransferIndex index : indexList)
                    groupList.add(index.transfer);

                DialogUtils.showRemoveTransferGroupListDialog(getActivity(), groupList);
                return true;
            } else if (id == R.id.action_mode_group_serve_on_web || id == R.id.action_mode_group_hide_on_web) {
                boolean served = id == R.id.action_mode_group_serve_on_web;
                List<TransferIndex> changedList = new ArrayList<>();

                for (TransferIndex index : indexList) {
                    if (!index.hasOutgoing() || index.transfer.isServedOnWeb == served)
                        continue;

                    index.transfer.isServedOnWeb = served;
                    changedList.add(index);
                }

                kuick.update(changedList);
                kuick.broadcast();
            } else
                super.onPerformerMenuSelected(performerMenu, item);

            return false;
        }
    }
}