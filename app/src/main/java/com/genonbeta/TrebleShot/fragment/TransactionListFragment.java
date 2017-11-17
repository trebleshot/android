package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.TransactionListAdapter;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.TransactionInfoDialog;
import com.genonbeta.TrebleShot.support.FragmentTitle;
import com.genonbeta.TrebleShot.util.TransactionObject;
import com.genonbeta.android.database.SQLQuery;

public class TransactionListFragment extends AbstractEditableListFragment<TransactionObject, TransactionListAdapter> implements FragmentTitle
{
	public AccessDatabase mDatabase;
	public IntentFilter mFilter = new IntentFilter();
	public BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())
					&& intent.hasExtra(AccessDatabase.EXTRA_TABLE_NAME)
					&& intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME).equals(AccessDatabase.TABLE_TRANSFER))
				refreshList();
		}
	};

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		setSearchSupport(false);
		mDatabase = new AccessDatabase(getActivity());

		mFilter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);
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
	public TransactionListAdapter onAdapter()
	{
		return new TransactionListAdapter(getActivity());
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

		final TransactionObject transactionObject = (TransactionObject) getAdapter().getItem(position);

		if (transactionObject instanceof TransactionListAdapter.TransactionFolder) {
			getAdapter().setPath(transactionObject.directory);
			refreshList();
		}
		else
			new TransactionInfoDialog(getActivity(), mDatabase, transactionObject).show();
	}

	@Override
	public CharSequence getFragmentTitle(Context context)
	{
		return context.getString(R.string.text_pendingTransfers);
	}
}
