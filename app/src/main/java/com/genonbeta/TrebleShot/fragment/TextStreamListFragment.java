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
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.activity.TextEditorActivity;
import com.genonbeta.TrebleShot.adapter.TextStreamListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.app.GroupEditableListFragment;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.ui.callback.IconProvider;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.SelectionUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.framework.object.Selectable;
import com.genonbeta.android.framework.ui.PerformerMenu;
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection;
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine;
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */

public class TextStreamListFragment extends GroupEditableListFragment<TextStreamObject,
        GroupEditableListAdapter.GroupViewHolder, TextStreamListAdapter> implements IconProvider
{
    private StatusReceiver mStatusReceiver = new StatusReceiver();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setFilteringSupported(true);
        setDefaultOrderingCriteria(TextStreamListAdapter.MODE_SORT_ORDER_DESCENDING);
        setDefaultSortingCriteria(TextStreamListAdapter.MODE_SORT_BY_DATE);
        setDefaultGroupingCriteria(TextStreamListAdapter.MODE_GROUP_BY_DATE);
    }

    @Override
    protected RecyclerView onListView(View mainContainer, ViewGroup listViewContainer)
    {
        FrameLayout view = (FrameLayout) getLayoutInflater().inflate(R.layout.layout_text_stream, null,
                false);
        ExtendedFloatingActionButton actionButton = view.findViewById(R.id.layout_text_stream_fab);

        listViewContainer.addView(view);

        actionButton.setOnClickListener(v -> startActivity(new Intent(getActivity(),
                TextEditorActivity.class).setAction(TextEditorActivity.ACTION_EDIT_TEXT)));

        return super.onListView(mainContainer, view.findViewById(R.id.layout_text_stream_content));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setEmptyImage(R.drawable.ic_forum_white_24dp);
        setEmptyText(getString(R.string.text_listEmptyTextStream));
        getListView().setClipToPadding(false);
        getListView().setPadding(0, 0, 0, (int) (getResources().getDimension(R.dimen.fab_margin) * 4));
    }

    @Nullable
    @Override
    public PerformerMenu onCreatePerformerMenu(Context context)
    {
        return new PerformerMenu(context, new SelectionCallback(getActivity(), this));
    }

    @Override
    public void onSortingOptions(Map<String, Integer> options)
    {
        options.put(getString(R.string.text_sortByName), TextStreamListAdapter.MODE_SORT_BY_NAME);
        options.put(getString(R.string.text_sortByDate), TextStreamListAdapter.MODE_SORT_BY_DATE);
    }

    @Override
    public void onGroupingOptions(Map<String, Integer> options)
    {
        options.put(getString(R.string.text_groupByNothing), TextStreamListAdapter.MODE_GROUP_BY_NOTHING);
        options.put(getString(R.string.text_groupByDate), TextStreamListAdapter.MODE_GROUP_BY_DATE);
    }

    @Override
    public int onGridSpanSize(int viewType, int currentSpanSize)
    {
        return viewType == TextStreamListAdapter.VIEW_TYPE_REPRESENTATIVE ? currentSpanSize
                : super.onGridSpanSize(viewType, currentSpanSize);
    }

    @Override
    public TextStreamListAdapter onAdapter()
    {
        final AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder> quickActions = clazz -> {
            if (!clazz.isRepresentative())
                registerLayoutViewClicks(clazz);
        };

        return new TextStreamListAdapter(getActivity(), AppUtils.getKuick(getContext()))
        {
            @NonNull
            @Override
            public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), quickActions);
            }
        };
    }

    @Override
    public boolean onDefaultClickAction(GroupEditableListAdapter.GroupViewHolder holder)
    {
        try {
            TextStreamObject object = getAdapter().getItem(holder.getAdapterPosition());

            startActivity(new Intent(getContext(), TextEditorActivity.class)
                    .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                    .putExtra(TextEditorActivity.EXTRA_CLIPBOARD_ID, object.id)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

            return true;
        } catch (Exception ignored) {
        }

        return false;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        getActivity().registerReceiver(mStatusReceiver, new IntentFilter(Kuick.ACTION_DATABASE_CHANGE));
        refreshList();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        getActivity().unregisterReceiver(mStatusReceiver);
    }

    @Override
    public int getIconRes()
    {
        return R.drawable.ic_short_text_white_24dp;
    }

    @Override
    public CharSequence getDistinctiveTitle(Context context)
    {
        return context.getString(R.string.text_textStream);
    }

    private static class SelectionCallback extends EditableListFragment.SelectionCallback
    {
        private MenuItem mShareWithTrebleShot;
        private MenuItem mShareWithOthers;

        public SelectionCallback(Activity activity, PerformerEngineProvider provider)
        {
            super(activity, provider);
        }

        @Override
        public boolean onPerformerMenuList(PerformerMenu performerMenu, MenuInflater inflater, Menu targetMenu)
        {
            super.onPerformerMenuList(performerMenu, inflater, targetMenu);

            // Sharing text with this menu is unnecessary since only one item can be sent at a time. So, this will be
            // disabled until it is possible to send multiple items.
            //inflater.inflate(R.menu.action_mode_share, targetMenu);
            inflater.inflate(R.menu.action_mode_text_stream, targetMenu);

            mShareWithTrebleShot = targetMenu.findItem(R.id.action_mode_share_trebleshot);
            mShareWithOthers = targetMenu.findItem(R.id.action_mode_share_all_apps);
            updateShareMethods(getPerformerEngine());

            return true;
        }

        @Override
        public boolean onPerformerMenuSelected(PerformerMenu performerMenu, MenuItem item)
        {
            int id = item.getItemId();

            IPerformerEngine engine = getPerformerEngine();
            if (engine == null)
                return false;

            List<Selectable> genericSelectionList = new ArrayList<>(engine.getSelectionList());
            List<TextStreamObject> selectionList = new ArrayList<>();
            Kuick kuick = AppUtils.getKuick(getActivity());
            Context context = getActivity();

            for (Selectable selectable : genericSelectionList)
                if (selectable instanceof TextStreamObject)
                    selectionList.add((TextStreamObject) selectable);

            if (id == R.id.action_mode_text_stream_delete) {
                kuick.remove(selectionList);
                kuick.broadcast();
                return true;
            } else if (id == R.id.action_mode_share_all_apps || id == R.id.action_mode_share_trebleshot) {
                if (selectionList.size() == 1) {
                    TextStreamObject streamObject = selectionList.get(0);
                    boolean shareLocally = id == R.id.action_mode_share_trebleshot;
                    Intent intent = (shareLocally ? new Intent(context, ShareActivity.class) : new Intent())
                            .setAction(Intent.ACTION_SEND)
                            .putExtra(Intent.EXTRA_TEXT, streamObject.text)
                            .setType("text/*");

                    getActivity().startActivity(shareLocally ? intent : Intent.createChooser(intent, context.getString(
                            R.string.text_fileShareAppChoose)));
                } else
                    Toast.makeText(context, R.string.mesg_textShareLimit, Toast.LENGTH_SHORT).show();
            } else
                return super.onPerformerMenuSelected(performerMenu, item);

            return false;
        }

        @Override
        public void onPerformerMenuItemSelected(PerformerMenu performerMenu, IPerformerEngine engine,
                                                IBaseEngineConnection owner, Selectable selectable, boolean isSelected,
                                                int position)
        {
            super.onPerformerMenuItemSelected(performerMenu, engine, owner, selectable, isSelected, position);
            updateShareMethods(engine);
        }

        @Override
        public void onPerformerMenuItemSelected(PerformerMenu performerMenu, IPerformerEngine engine,
                                                IBaseEngineConnection owner, List<? extends Selectable> selectableList,
                                                boolean isSelected, int[] positions)
        {
            super.onPerformerMenuItemSelected(performerMenu, engine, owner, selectableList, isSelected, positions);
            updateShareMethods(engine);
        }

        private void updateShareMethods(IPerformerEngine engine)
        {
            int totalSelections = SelectionUtils.getTotalSize(engine);

            if (mShareWithOthers != null)
                mShareWithOthers.setEnabled(totalSelections == 1);

            if (mShareWithTrebleShot != null)
                mShareWithTrebleShot.setEnabled(totalSelections == 1);
        }
    }

    private class StatusReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (Kuick.ACTION_DATABASE_CHANGE.equals(intent.getAction())) {
                Kuick.BroadcastData data = Kuick.toData(intent);
                if (Kuick.TABLE_CLIPBOARD.equals(data.tableName))
                    refreshList();
            }
        }
    }
}