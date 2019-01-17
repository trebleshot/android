package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.activity.TextEditorActivity;
import com.genonbeta.TrebleShot.adapter.TextStreamListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.app.EditableListFragmentImpl;
import com.genonbeta.TrebleShot.app.GroupEditableListFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.ui.callback.IconSupport;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.framework.widget.PowerfulActionMode;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Map;

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */

public class TextStreamListFragment
        extends GroupEditableListFragment<TextStreamObject, GroupEditableListAdapter.GroupViewHolder, TextStreamListAdapter>
        implements IconSupport, TitleSupport
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
        setDefaultSelectionCallback(new SelectionCallback(this));
    }

    @Override
    protected RecyclerView onListView(View mainContainer, ViewGroup listViewContainer)
    {
        FrameLayout view = (FrameLayout) getLayoutInflater().inflate(R.layout.layout_text_stream, null, false);
        FloatingActionButton actionButton = view.findViewById(R.id.layout_text_stream_fab);

        listViewContainer.addView(view);

        actionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(getActivity(), TextEditorActivity.class)
                        .setAction(TextEditorActivity.ACTION_EDIT_TEXT));
            }
        });

        return super.onListView(mainContainer, (FrameLayout) view.findViewById(R.id.layout_text_stream_content));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setEmptyImage(R.drawable.ic_forum_white_24dp);
        setEmptyText(getString(R.string.text_listEmptyTextStream));
        getListView().setClipToPadding(false);
        getListView().setPadding(0, 0, 0, (int) (getResources().getDimension(R.dimen.fab_margin) * 6));
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
        return viewType == TextStreamListAdapter.VIEW_TYPE_REPRESENTATIVE
                ? currentSpanSize
                : super.onGridSpanSize(viewType, currentSpanSize);
    }

    @Override
    public TextStreamListAdapter onAdapter()
    {
        final AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder> quickActions = new AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder>()
        {
            @Override
            public void onQuickActions(final GroupEditableListAdapter.GroupViewHolder clazz)
            {
                if (!clazz.isRepresentative())
                    registerLayoutViewClicks(clazz);
            }
        };

        return new TextStreamListAdapter(getActivity(), AppUtils.getDatabase(getContext()))
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
        } catch (Exception e) {
        }

        return false;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        getActivity().registerReceiver(mStatusReceiver, new IntentFilter(AccessDatabase.ACTION_DATABASE_CHANGE));
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
    public CharSequence getTitle(Context context)
    {
        return context.getString(R.string.text_textStream);
    }

    private static class SelectionCallback extends EditableListFragment.SelectionCallback<TextStreamObject>
    {
        public SelectionCallback(EditableListFragmentImpl<TextStreamObject> fragment)
        {
            super(fragment);
        }

        @Override
        public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
        {
            super.onCreateActionMenu(context, actionMode, menu);
            actionMode.getMenuInflater().inflate(R.menu.action_mode_text_stream, menu);
            return true;
        }

        @Override
        public boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item)
        {
            int id = item.getItemId();

            List<TextStreamObject> selectionList = getFragment().getSelectionConnection().getSelectedItemList();

            if (id == R.id.action_mode_text_stream_delete) {
                AppUtils.getDatabase(getFragment().getContext()).remove(selectionList);
            } else if (id == R.id.action_mode_share_all_apps || id == R.id.action_mode_share_trebleshot) {
                if (selectionList.size() == 1) {
                    TextStreamObject streamObject = selectionList.get(0);

                    Intent shareIntent = new Intent(item.getItemId() == R.id.action_mode_share_all_apps
                            ? Intent.ACTION_SEND : ShareActivity.ACTION_SEND)
                            .putExtra(Intent.EXTRA_TEXT, streamObject.text)
                            .setType("text/*");

                    getAdapter().getContext().startActivity((item.getItemId() == R.id.action_mode_share_all_apps) ? Intent.createChooser(shareIntent, getFragment().getContext().getString(R.string.text_fileShareAppChoose)) : shareIntent);
                } else {
                    Toast.makeText(context, R.string.mesg_textShareLimit, Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else
                return super.onActionMenuItemSelected(context, actionMode, item);

            return true;
        }
    }

    private class StatusReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())
                    && intent.hasExtra(AccessDatabase.EXTRA_TABLE_NAME)
                    && intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME).equals(AccessDatabase.TABLE_CLIPBOARD))
                refreshList();
        }
    }
}