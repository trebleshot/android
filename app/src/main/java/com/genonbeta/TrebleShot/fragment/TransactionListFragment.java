package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.TransactionListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.TransactionInfoDialog;
import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.TrebleShot.util.TitleSupport;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;
import com.genonbeta.android.database.SQLQuery;

import java.io.File;
import java.util.ArrayList;

public class TransactionListFragment
		extends EditableListFragment<TransactionObject, TransactionListAdapter>
		implements TitleSupport
{
	public AccessDatabase mDatabase;
	public IntentFilter mFilter = new IntentFilter();
	private ArrayList<TransactionObject> mSelectionList = new ArrayList<>();
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
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		final TransactionObject transactionObject = (TransactionObject) getAdapter().getItem(position);

		if (transactionObject instanceof TransactionListAdapter.TransactionFolder) {
			getAdapter().setPath(transactionObject.directory);
			refreshList();
		} else
			new TransactionInfoDialog(getActivity(), mDatabase, transactionObject).show();
	}


	@Override
	public boolean onPrepareActionMenu(Context context, PowerfulActionMode actionMode)
	{
		super.onPrepareActionMenu(context, actionMode);
		getSelectionList().clear();
		return true;
	}

	@Override
	public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
	{
		super.onCreateActionMenu(context, actionMode, menu);
		actionMode.getMenuInflater().inflate(R.menu.action_mode_transaction, menu);
		return true;
	}

	@Override
	public boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.action_mode_transaction_delete) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

			builder.setTitle(R.string.ques_removeQueue);
			builder.setMessage(getResources().getQuantityString(R.plurals.text_removeQueueSummary, getSelectionList().size(), getSelectionList().size()));
			builder.setNegativeButton(R.string.butn_close, null);
			builder.setPositiveButton(R.string.butn_proceed, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialogInterface, int i)
				{
					new Handler(Looper.myLooper()).post(new Runnable()
					{
						@Override
						public void run()
						{
							for (TransactionObject transactionObject : getSelectionList())
								if (transactionObject instanceof TransactionListAdapter.TransactionFolder) {

									mDatabase.delete(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
											.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND ("
															+ AccessDatabase.FIELD_TRANSFER_DIRECTORY + " LIKE ? OR "
															+ AccessDatabase.FIELD_TRANSFER_DIRECTORY + " = ?)",
													String.valueOf(getAdapter().getGroupId()),
													transactionObject.directory + File.separator + "%",
													transactionObject.directory));
								} else {
									mDatabase.remove(transactionObject);
								}
						}
					});
				}
			});

			builder.show();
		} else if (id == R.id.action_mode_abs_editable_multi_select) {
			setSelection(getListView().getCheckedItemCount() != getAdapter().getCount());
			return false;
		} else
			return false;

		return true;
	}

	@Override
	public void onItemChecked(Context context, PowerfulActionMode actionMode, int position, boolean isSelected)
	{
		super.onItemChecked(context, actionMode, position, isSelected);

		TransactionObject shareable = (TransactionObject) getAdapter().getItem(position);

		if (isSelected)
			getSelectionList().add(shareable);
		else
			getSelectionList().remove(shareable);

		actionMode.setTitle(String.valueOf(getSelectionList().size()));
	}

	public ArrayList<TransactionObject> getSelectionList()
	{
		return mSelectionList;
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_pendingTransfers);
	}
}
