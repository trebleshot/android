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
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.AddDeviceActivity;
import com.genonbeta.TrebleShot.activity.ContentSharingActivity;
import com.genonbeta.TrebleShot.activity.ViewTransferActivity;
import com.genonbeta.TrebleShot.adapter.TransferGroupListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.app.GroupEditableListFragment;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.DialogUtils;
import com.genonbeta.TrebleShot.object.PreloadedGroup;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.ui.callback.IconProvider;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.SQLQuery;
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

public class TransferGroupListFragment extends GroupEditableListFragment<PreloadedGroup,
        GroupEditableListAdapter.GroupViewHolder, TransferGroupListAdapter> implements IconProvider
{
    private IntentFilter mFilter = new IntentFilter();
    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (Kuick.ACTION_DATABASE_CHANGE.equals(intent.getAction())) {
                Kuick.BroadcastData data = Kuick.toData(intent);
                if (data != null && (Kuick.TABLE_TRANSFERGROUP.equals(data.tableName)
                        || Kuick.TABLE_TRANSFER.equals(data.tableName)))
                    refreshList();
            } else if (CommunicationService.ACTION_TASK_LIST.equals(intent.getAction())
                    && intent.hasExtra(CommunicationService.EXTRA_TASK_LIST)) {
                getAdapter().updateActiveList(intent.getLongArrayExtra(CommunicationService.EXTRA_TASK_LIST));
                refreshList();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setLayoutResId(R.layout.layout_transfer_group);
        setFilteringSupported(true);
        setDefaultOrderingCriteria(TransferGroupListAdapter.MODE_SORT_ORDER_DESCENDING);
        setDefaultSortingCriteria(TransferGroupListAdapter.MODE_SORT_BY_DATE);
        setDefaultGroupingCriteria(TransferGroupListAdapter.MODE_GROUP_BY_DATE);
        setUseDefaultPaddingDecoration(true);
        setUseDefaultPaddingDecorationSpaceForEdges(true);
        setDefaultPaddingDecorationSize(getResources().getDimension(R.dimen.padding_list_content_parent_layout));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setListAdapter(new TransferGroupListAdapter(this, this));
        setEmptyListImage(R.drawable.ic_compare_arrows_white_24dp);
        setEmptyListText(getString(R.string.text_listEmptyTransfer));

        view.findViewById(R.id.sendLayoutButton).setOnClickListener(v -> startActivity(
                new Intent(getContext(), ContentSharingActivity.class)));
        view.findViewById(R.id.receiveLayoutButton).setOnClickListener(v -> startActivity(
                new Intent(getContext(), AddDeviceActivity.class)
                        .putExtra(AddDeviceActivity.EXTRA_REQUEST_TYPE,
                                AddDeviceActivity.RequestType.MAKE_ACQUAINTANCE)));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        mFilter.addAction(Kuick.ACTION_DATABASE_CHANGE);
        mFilter.addAction(CommunicationService.ACTION_TASK_LIST);
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
        getActivity().registerReceiver(mReceiver, mFilter);

        AppUtils.startForegroundService(getActivity(), new Intent(getActivity(), CommunicationService.class)
                .setAction(CommunicationService.ACTION_REQUEST_TASK_LIST));
    }

    @Override
    public void onPause()
    {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onSortingOptions(Map<String, Integer> options)
    {
        options.put(getString(R.string.text_sortByDate), TransferGroupListAdapter.MODE_SORT_BY_DATE);
        options.put(getString(R.string.text_sortBySize), TransferGroupListAdapter.MODE_SORT_BY_SIZE);
    }

    @Override
    public void onGroupingOptions(Map<String, Integer> options)
    {
        options.put(getString(R.string.text_groupByNothing), TransferGroupListAdapter.MODE_GROUP_BY_NOTHING);
        options.put(getString(R.string.text_groupByDate), TransferGroupListAdapter.MODE_GROUP_BY_DATE);
    }

    @Override
    public boolean onDefaultClickAction(GroupEditableListAdapter.GroupViewHolder holder)
    {
        try {
            ViewTransferActivity.startInstance(getActivity(), getAdapter().getItem(holder).id);
            return true;
        } catch (Exception ignored) {
        }

        return false;
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
            List<PreloadedGroup> selectionList = new ArrayList<>();

            for (Selectable selectable : genericList)
                if (selectable instanceof PreloadedGroup)
                    selectionList.add((PreloadedGroup) selectable);

            if (id == R.id.action_mode_group_delete) {
                DialogUtils.showRemoveTransferGroupListDialog(getActivity(), selectionList);
                return true;
            } else if (id == R.id.action_mode_group_serve_on_web || id == R.id.action_mode_group_hide_on_web) {
                boolean served = id == R.id.action_mode_group_serve_on_web;
                List<PreloadedGroup> changedList = new ArrayList<>();

                for (PreloadedGroup group : selectionList) {
                    if (!group.hasOutgoing() || group.isServedOnWeb == served)
                        continue;

                    group.isServedOnWeb = served;
                    changedList.add(group);
                }

                kuick.update(changedList);
                kuick.broadcast();
            } else
                super.onPerformerMenuSelected(performerMenu, item);

            return false;
        }
    }
}