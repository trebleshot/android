package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
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

			if (isSelectionActivated() && !PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("helpFolderSelection", false))
				createSnackbar(R.string.mesg_helpFolderSelection)
						.setAction(R.string.butn_gotIt, new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								PreferenceManager.getDefaultSharedPreferences(getActivity())
										.edit()
										.putBoolean("helpFolderSelection", true)
										.apply();
							}
						})
						.show();
		} else if (!setItemSelected(position))
			new TransactionInfoDialog(getActivity(), mDatabase, transactionObject).show();
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
		actionMode.getMenuInflater().inflate(R.menu.action_mode_transaction, menu);
		return true;
	}

	@Override
	public boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item)
	{
		int id = item.getItemId();

		final ArrayList<TransactionObject> selectionList = new ArrayList<>(getSelectionConnection().getSelectedItemList());

		if (id == R.id.action_mode_transaction_delete) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

			builder.setTitle(R.string.ques_removeQueue);
			builder.setMessage(getResources().getQuantityString(R.plurals.text_removeQueueSummary, selectionList.size(), selectionList.size()));
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
							for (TransactionObject transactionObject : selectionList)
								if (transactionObject instanceof TransactionListAdapter.TransactionFolder) {
									mDatabase.delete(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
											.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND ("
															+ AccessDatabase.FIELD_TRANSFER_DIRECTORY + " LIKE ? OR "
															+ AccessDatabase.FIELD_TRANSFER_DIRECTORY + " = ?)",
													String.valueOf(getAdapter().getGroupId()),
													transactionObject.directory + File.separator + "%",
													transactionObject.directory));
								} else
									mDatabase.remove(transactionObject);
						}
					});
				}
			});

			builder.show();
		} else
			return super.onActionMenuItemSelected(context, actionMode, item);

		return true;
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_pendingTransfers);
	}
}
