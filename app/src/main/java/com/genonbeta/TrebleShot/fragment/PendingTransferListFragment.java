package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.PendingTransferListAdapter;
import com.genonbeta.TrebleShot.database.DeviceRegistry;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.dialog.PendingTransferListDialog;
import com.genonbeta.TrebleShot.support.FragmentTitle;
import com.genonbeta.android.database.CursorItem;

public class PendingTransferListFragment extends AbstractEditableListFragment<PendingTransferListAdapter> implements FragmentTitle
{
	public Transaction mTransaction;
	public DeviceRegistry mDeviceRegistry;
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
		mFilter.addAction(Transaction.ACTION_TRANSACTION_REGISTERED);
		mFilter.addAction(Transaction.ACTION_TRANSACTION_UPDATED);
		mFilter.addAction(Transaction.ACTION_TRANSACTION_REMOVED);

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
		return new PendingTransferListAdapter(getActivity());
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
		new PendingTransferListDialog(getActivity(), mDeviceRegistry, mTransaction, item.getInt(Transaction.FIELD_TRANSFER_GROUPID)).show();
	}

	@Override
	public CharSequence getFragmentTitle(Context context)
	{
		return context.getString(R.string.pending_transfers);
	}
}
