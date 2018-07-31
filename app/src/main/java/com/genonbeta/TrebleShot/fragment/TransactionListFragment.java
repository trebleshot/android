package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.TransactionGroupListAdapter;
import com.genonbeta.TrebleShot.adapter.TransactionListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.app.GroupEditableListFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.TransactionInfoDialog;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.framework.widget.PowerfulActionMode;
import com.genonbeta.android.database.SQLQuery;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class TransactionListFragment
		extends GroupEditableListFragment<TransactionListAdapter.GroupEditableTransferObject, GroupEditableListAdapter.GroupViewHolder, TransactionListAdapter>
		implements TitleSupport
{
	public static final String TAG = "TransactionListFragment";
	public static final int TASK_REMOVE_TRANSFER_OBJECTS = 0;

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
		setDefaultGroupingCriteria(TransactionListAdapter.MODE_GROUP_BY_DEFAULT);
		setDefaultSelectionCallback(new SelectionCallback(this));
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
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
		options.put(getString(R.string.text_sortBySize), TransactionGroupListAdapter.MODE_SORT_BY_SIZE);
	}

	@Override
	public TransactionListAdapter onAdapter()
	{
		final AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder> quickActions = new AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder>()
		{
			@Override
			public void onQuickActions(final GroupEditableListAdapter.GroupViewHolder clazz)
			{
				if (!clazz.isRepresentative()) {
					registerLayoutViewClicks(clazz);

					if (getSelectionConnection() != null)
						clazz.getView().findViewById(R.id.layout_image).setOnClickListener(new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								getSelectionConnection().setSelected(clazz.getAdapterPosition());
							}
						});
				}
			}
		};

		return new TransactionListAdapter(getActivity(), AppUtils.getDatabase(getContext()))
		{
			@NonNull
			@Override
			public GroupEditableListAdapter.GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
			{
				return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), quickActions);
			}
		};
	}

	@Override
	public int onGridSpanSize(int viewType, int currentSpanSize)
	{
		return viewType == TransactionListAdapter.VIEW_TYPE_REPRESENTATIVE
				? currentSpanSize
				: super.onGridSpanSize(viewType, currentSpanSize);
	}

	@Override
	public boolean onDefaultClickAction(GroupEditableListAdapter.GroupViewHolder holder)
	{
		final TransferObject transferObject = getAdapter().getItem(holder);

		new TransactionInfoDialog(getActivity(), AppUtils.getDatabase(getContext()), AppUtils.getDefaultPreferences(getContext()), transferObject)
				.show();

		return false;
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_pendingTransfers);
	}

	@Override
	public boolean performLayoutClick(GroupEditableListAdapter.GroupViewHolder holder)
	{
		final TransferObject transferObject = getAdapter().getItem(holder);

		if (transferObject instanceof TransactionListAdapter.TransferFolder) {
			getAdapter().setPath(transferObject.directory);
			refreshList();

			if (getSelectionCallback() != null && getSelectionCallback().isSelectionActivated() && !AppUtils.getDefaultPreferences(getContext()).getBoolean("helpFolderSelection", false))
				createSnackbar(R.string.mesg_helpFolderSelection)
						.setAction(R.string.butn_gotIt, new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								AppUtils.getDefaultPreferences(getContext())
										.edit()
										.putBoolean("helpFolderSelection", true)
										.apply();
							}
						})
						.show();
		} else
			return super.performLayoutClick(holder);

		return true;
	}

	private static class SelectionCallback extends EditableListFragment.SelectionCallback<TransactionListAdapter.GroupEditableTransferObject>
	{
		private TransactionListAdapter mAdapter;

		public SelectionCallback(TransactionListFragment fragment)
		{
			super(fragment);
			mAdapter = fragment.getAdapter();
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

			final ArrayList<TransactionListAdapter.GroupEditableTransferObject> selectionList = new ArrayList<>(getFragment().getSelectionConnection().getSelectedItemList());

			if (id == R.id.action_mode_transaction_delete) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getFragment().getActivity());

				builder.setTitle(R.string.ques_removeQueue);
				builder.setMessage(getFragment().getContext().getResources().getQuantityString(R.plurals.text_removeQueueSummary, selectionList.size(), selectionList.size()));
				builder.setNegativeButton(R.string.butn_close, null);
				builder.setPositiveButton(R.string.butn_proceed, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int i)
					{
						WorkerService.run(getFragment().getContext(), new WorkerService.RunningTask(TAG, TASK_REMOVE_TRANSFER_OBJECTS)
						{
							@Override
							protected void onRun()
							{
								for (TransactionListAdapter.GroupEditableTransferObject transferObject : selectionList)
									if (transferObject instanceof TransactionListAdapter.TransferFolder) {
										AppUtils.getDatabase(getFragment().getContext()).delete(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
												.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND ("
																+ AccessDatabase.FIELD_TRANSFER_DIRECTORY + " LIKE ? OR "
																+ AccessDatabase.FIELD_TRANSFER_DIRECTORY + " = ?)",
														String.valueOf(mAdapter.getGroupId()),
														transferObject.directory + File.separator + "%",
														transferObject.directory));
									} else
										AppUtils.getDatabase(getFragment().getContext()).remove(transferObject);
							}
						});
					}
				});

				builder.show();
			} else
				return super.onActionMenuItemSelected(context, actionMode, item);

			return true;
		}
	}
}
