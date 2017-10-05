package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.PendingTransferListActivity;
import com.genonbeta.TrebleShot.adapter.PendingTransferListAdapter;
import com.genonbeta.TrebleShot.database.DeviceRegistry;
import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.dialog.DeviceChooserDialog;
import com.genonbeta.TrebleShot.dialog.FixFilePathDialog;
import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
import com.genonbeta.TrebleShot.helper.FileUtils;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.ServerService;
import com.genonbeta.TrebleShot.support.FragmentTitle;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;

public class PendingTransferListFragment extends AbstractEditableListFragment<CursorItem, PendingTransferListAdapter> implements FragmentTitle
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
			refreshList();
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
	public PendingTransferListAdapter onAdapter()
	{
		return new PendingTransferListAdapter(getActivity()).setSelect(mSelect);
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
	protected ActionModeListener onActionModeListener()
	{
		return null;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		final CursorItem thisItem = (CursorItem) getAdapter().getItem(position);

		if (getSelect() == null || getSelect().getItems().exists(PendingTransferListAdapter.FLAG_GROUP)
				&& getSelect().getItems().getBoolean(PendingTransferListAdapter.FLAG_GROUP))
			PendingTransferListActivity.startInstance(getContext(), thisItem.getInt(Transaction.FIELD_TRANSFER_GROUPID));
		else {
			if (thisItem.getInt(MainDatabase.FIELD_TRANSFER_TYPE) == MainDatabase.TYPE_TRANSFER_TYPE_INCOMING) {
				final AwaitedFileReceiver receiver = new AwaitedFileReceiver(thisItem);
				final NetworkDevice device = mDeviceRegistry.getNetworkDeviceById(receiver.deviceId);

				FileUtils.Conflict conflict = FileUtils.isFileConflicted(getContext(), receiver);

				if (!FileUtils.Conflict.CURRENTLY_OK.equals(conflict)) {
					new FixFilePathDialog(getContext(), mTransaction, receiver, conflict).show();
				} else if (device != null) {
					new DeviceChooserDialog(getActivity(), device, new DeviceChooserDialog.OnDeviceSelectedListener()
					{
						@Override
						public void onDeviceSelected(DeviceChooserDialog.AddressHolder addressHolder, ArrayList<DeviceChooserDialog.AddressHolder> availableInterfaces)
						{
							Transaction.EditingSession editingSession = mTransaction.edit();

							if (receiver.flag.equals(Transaction.Flag.INTERRUPTED)) {
								receiver.flag = Transaction.Flag.RESUME;

								editingSession.updateTransaction(receiver);
							}

							if (!receiver.ip.equals(addressHolder.address)) {
								receiver.ip = addressHolder.address;
								ContentValues values = new ContentValues();

								values.put(Transaction.FIELD_TRANSFER_USERIP, addressHolder.address);
								editingSession.updateTransactionGroup(receiver.groupId, values);
							}

							getContext().startService(new Intent(mTransaction.getContext(), ServerService.class)
									.setAction(ServerService.ACTION_START_RECEIVING)
									.putExtra(CommunicationService.EXTRA_GROUP_ID, receiver.groupId));

							editingSession.done();
						}
					}).show();
				} else
					Toast.makeText(getActivity(), R.string.mesg_deviceNotExits, Toast.LENGTH_LONG).show();
			}
		}
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

	public PendingTransferListFragment setSelect(SQLQuery.Select select)
	{
		mSelect = select;
		return this;
	}
}
