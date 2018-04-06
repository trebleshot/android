package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.TransactionActivity;
import com.genonbeta.TrebleShot.adapter.TransactionGroupListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.util.TitleSupport;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;
import java.util.Map;

/**
 * created by: Veli
 * date: 10.11.2017 00:15
 */

public class TransactionGroupListFragment
		extends EditableListFragment<TransactionGroupListAdapter.PreloadedGroup, EditableListAdapter.EditableViewHolder, TransactionGroupListAdapter>
		implements TitleSupport
{
	public SQLQuery.Select mSelect;
	public IntentFilter mFilter = new IntentFilter();
	public BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())
					&& intent.hasExtra(AccessDatabase.EXTRA_TABLE_NAME)
					&& (intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME).equals(AccessDatabase.TABLE_TRANSFERGROUP)
					|| intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME).equals(AccessDatabase.TABLE_TRANSFER)
			))
				refreshList();
		}
	};

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setDefaultOrderingCriteria(TransactionGroupListAdapter.MODE_SORT_ORDER_DESCENDING);
		setDefaultSortingCriteria(TransactionGroupListAdapter.MODE_SORT_BY_DATE);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		setEmptyImage(R.drawable.ic_compare_arrows_white_24dp);
		setEmptyText(getString(R.string.text_listEmptyPendingTransfer));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mFilter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);

		if (getSelect() == null)
			setSelect(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP));
	}

	@Override
	public void onResume()
	{
		super.onResume();
		getActivity().registerReceiver(mReceiver, mFilter);
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
		options.put(getString(R.string.text_sortByDate), TransactionGroupListAdapter.MODE_SORT_BY_DATE);
		options.put(getString(R.string.text_sortBySize), TransactionGroupListAdapter.MODE_SORT_BY_SIZE);
	}

	@Override
	public TransactionGroupListAdapter onAdapter()
	{
		return new TransactionGroupListAdapter(getActivity(), getDatabase())
		{
			@NonNull
			@Override
			public EditableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
			{
				return super.onCreateViewHolder(parent, viewType);
			}

			@Override
			public void onBindViewHolder(@NonNull final EditableListAdapter.EditableViewHolder holder, int position)
			{
				super.onBindViewHolder(holder, position);

				holder.getView().setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						if (!setItemSelected(holder))
							TransactionActivity.startInstance(getActivity(), getAdapter().getItem(holder).groupId);
					}
				});
			}
		}.setSelect(getSelect());
	}

	@Override
	public boolean onPrepareActionMenu(Context context, PowerfulActionMode actionMode)
	{
		super.onPrepareActionMenu(context, actionMode);
		return true;
	}

	@Override
	public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
	{
		super.onCreateActionMenu(context, actionMode, menu);
		actionMode.getMenuInflater().inflate(R.menu.action_mode_group, menu);
		return true;
	}

	@Override
	public boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item)
	{
		int id = item.getItemId();

		ArrayList<TransactionGroupListAdapter.PreloadedGroup> selectionList = getSelectionConnection().getSelectedItemList();

		if (id == R.id.action_mode_group_delete) {
			for (TransactionGroupListAdapter.PreloadedGroup preloadedGroup : selectionList)
				getDatabase().remove(preloadedGroup);
		} else
			return super.onActionMenuItemSelected(context, actionMode, item);

		return true;
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_pendingTransfers);
	}

	public SQLQuery.Select getSelect()
	{
		return mSelect;
	}

	public TransactionGroupListFragment setSelect(SQLQuery.Select select)
	{
		mSelect = select;
		return this;
	}
}