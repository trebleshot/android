package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.OngoingListAdapter;
import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.ServerService;
import com.genonbeta.TrebleShot.support.FragmentTitle;
import com.genonbeta.android.database.CursorItem;

public class OngoingListFragment extends AbstractEditableListFragment<OngoingListAdapter> implements FragmentTitle
{
	public Transaction mTransaction;
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
	protected OngoingListAdapter onAdapter()
	{
		return new OngoingListAdapter(getActivity());
	}

	@Override
	protected ActionModeListener onActionModeListener()
	{
		return new ChoiceListener();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		CursorItem item = (CursorItem) getAdapter().getItem(position);

		if (item.getInt(MainDatabase.FIELD_TRANSFER_TYPE) == MainDatabase.TYPE_TRANSFER_TYPE_INCOMING)
		{
			AwaitedFileReceiver receiver = new AwaitedFileReceiver(item);

			if (receiver.flag.equals(Transaction.Flag.INTERRUPTED))
			{
				receiver.flag = Transaction.Flag.RESUME;
				mTransaction.updateTransaction(receiver);
			}

			getActivity().startService(new Intent(getActivity(), ServerService.class)
					.setAction(ServerService.ACTION_START_RECEIVING)
					.putExtra(CommunicationService.EXTRA_ACCEPT_ID, receiver.acceptId));
		}
	}

	@Override
	public CharSequence getFragmentTitle(Context context)
	{
		return context.getString(R.string.ongoing_process);
	}

	private class ChoiceListener extends ActionModeListener
	{
		public Uri onItemChecked(ActionMode mode, int pos, long id, boolean isChecked)
		{
			getAdapter().getItem(pos);

			return null;
		}
	}
}
