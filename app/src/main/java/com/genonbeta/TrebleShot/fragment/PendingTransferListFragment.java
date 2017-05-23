package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.PendingTransferListActivity;
import com.genonbeta.TrebleShot.adapter.PendingTransferListAdapter;
import com.genonbeta.TrebleShot.database.DeviceRegistry;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.support.FragmentTitle;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

public class PendingTransferListFragment extends AbstractEditableListFragment<PendingTransferListAdapter> implements FragmentTitle
{
	public Transaction mTransaction;
	public DeviceRegistry mDeviceRegistry;
	public SQLQuery.Select mSelect;
	public IntentFilter mFilter = new IntentFilter();
	public BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			updateInBackground();
		}
	};

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		mFilter.addAction(Transaction.ACTION_TRANSACTION_CHANGE);

		mTransaction = new Transaction(getContext());
		mDeviceRegistry = new DeviceRegistry(getContext());

		setSearchSupport(false);
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
	protected PendingTransferListAdapter onAdapter()
	{
		return new PendingTransferListAdapter(getActivity()).setSelect(mSelect);
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

		CursorItem item = (CursorItem) getAdapter().getItem(position);
		PendingTransferListActivity.startInstance(getContext(), item.getInt(Transaction.FIELD_TRANSFER_GROUPID));
	}

	@Override
	public CharSequence getFragmentTitle(Context context)
	{
		return context.getString(R.string.pending_transfers);
	}

	public SQLQuery.Select getSelect()
	{
		return mSelect;
	}

	public PendingTransferListFragment setSelect(SQLQuery.Select select)
	{
		mSelect = select;
		return this;
	}
}
