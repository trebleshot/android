package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.TransactionActivity;
import com.genonbeta.TrebleShot.adapter.TransactionGroupListAdapter;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.support.FragmentTitle;
import com.genonbeta.TrebleShot.util.TransactionObject;
import com.genonbeta.android.database.SQLQuery;

/**
 * created by: Veli
 * date: 10.11.2017 00:15
 */

public class TransactionGroupListFragment extends AbstractEditableListFragment<TransactionGroupListAdapter.PreloadedGroup, TransactionGroupListAdapter> implements FragmentTitle
{
	public SQLQuery.Select mSelect;
	public AccessDatabase mDatabase;
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
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mDatabase = new AccessDatabase(getActivity());

		mFilter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);

		setSearchSupport(false);

		if (getSelect() == null) {
			setSelect(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP));
		}
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
	public TransactionGroupListAdapter onAdapter()
	{
		return new TransactionGroupListAdapter(getActivity()).setSelect(getSelect());
	}

	@Override
	protected ActionModeListener onActionModeListener()
	{
		return null;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		final TransactionObject.Group thisGroup = (TransactionObject.Group) getAdapter().getItem(position);

		TransactionActivity.startInstance(getActivity(), thisGroup.groupId);
	}

	@Override
	public CharSequence getFragmentTitle(Context context)
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