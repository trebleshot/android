package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.TransactionListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.TransactionInfoDialog;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.util.TitleSupport;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;
import com.genonbeta.android.database.SQLQuery;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class TransactionListFragment
		extends EditableListFragment<TransferObject, EditableListAdapter.EditableViewHolder, TransactionListAdapter>
		implements TitleSupport
{
	private BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())
					&& AccessDatabase.TABLE_TRANSFER.equals(intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME)))
				refreshList();
		}
	};

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setDefaultOrderingCriteria(TransactionListAdapter.MODE_SORT_ORDER_DESCENDING);
		setDefaultSortingCriteria(TransactionListAdapter.MODE_SORT_BY_DEFAULT);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		setEmptyImage(R.drawable.ic_compare_arrows_white_24dp);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		getActivity().registerReceiver(mReceiver, new IntentFilter(AccessDatabase.ACTION_DATABASE_CHANGE));
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
		options.put(getString(R.string.text_default), TransactionListAdapter.MODE_SORT_BY_DEFAULT);
		options.put(getString(R.string.text_sortByName), TransactionListAdapter.MODE_SORT_BY_NAME);
	}

	@Override
	public TransactionListAdapter onAdapter()
	{
		return new TransactionListAdapter(getActivity(), getDatabase())
		{
			@Override
			public void onBindViewHolder(@NonNull final EditableListAdapter.EditableViewHolder holder, int position)
			{
				super.onBindViewHolder(holder, position);

				holder.getView().setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						final TransferObject transferObject = getAdapter().getItem(holder);

						if (transferObject instanceof TransferFolder) {
							getAdapter().setPath(transferObject.directory);
							refreshList();

							if (isSelectionActivated() && !getDefaultPreferences().getBoolean("helpFolderSelection", false))
								createSnackbar(R.string.mesg_helpFolderSelection)
										.setAction(R.string.butn_gotIt, new View.OnClickListener()
										{
											@Override
											public void onClick(View v)
											{
												getDefaultPreferences()
														.edit()
														.putBoolean("helpFolderSelection", true)
														.apply();
											}
										})
										.show();
						} else if (!setItemSelected(holder))
							new TransactionInfoDialog(getActivity(), getDatabase(), getDefaultPreferences(), transferObject)
									.show();
					}
				});
			}
		};
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

		final ArrayList<TransferObject> selectionList = new ArrayList<>(getSelectionConnection().getSelectedItemList());

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
							for (TransferObject transferObject : selectionList)
								if (transferObject instanceof TransactionListAdapter.TransferFolder) {
									getDatabase().delete(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
											.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND ("
															+ AccessDatabase.FIELD_TRANSFER_DIRECTORY + " LIKE ? OR "
															+ AccessDatabase.FIELD_TRANSFER_DIRECTORY + " = ?)",
													String.valueOf(getAdapter().getGroupId()),
													transferObject.directory + File.separator + "%",
													transferObject.directory));
								} else
									getDatabase().remove(transferObject);
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
