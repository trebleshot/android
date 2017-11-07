package com.genonbeta.TrebleShot.fragment;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.PendingTransferListActivity;
import com.genonbeta.TrebleShot.adapter.PendingTransferListAdapter;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.DeviceChooserDialog;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.ServerService;
import com.genonbeta.TrebleShot.support.FragmentTitle;
import com.genonbeta.TrebleShot.util.TransactionObject;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;

public class PendingTransferListFragment extends AbstractEditableListFragment<CursorItem, PendingTransferListAdapter> implements FragmentTitle
{
	public SQLQuery.Select mSelect;
	public AccessDatabase mDatabase;

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		setSearchSupport(false);
		mDatabase = new AccessDatabase(getActivity());
	}

	@Override
	public PendingTransferListAdapter onAdapter()
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

		final CursorItem thisItem = (CursorItem) getAdapter().getItem(position);

		if (getSelect() == null || getSelect().getItems().exists(PendingTransferListAdapter.FLAG_GROUP)
				&& getSelect().getItems().getBoolean(PendingTransferListAdapter.FLAG_GROUP))
			PendingTransferListActivity.startInstance(getContext(), thisItem.getInt(AccessDatabase.FIELD_TRANSFER_GROUPID));
		else {
			if (TransactionObject.Type.INCOMING.equals(TransactionObject.Type.valueOf(thisItem.getString(AccessDatabase.FIELD_TRANSFER_TYPE)))) {
				final TransactionObject transaction = new TransactionObject(thisItem);

				try {
					final TransactionObject.Group groupInstance = new TransactionObject.Group(transaction.groupId);
					mDatabase.reconstruct(groupInstance);

					final NetworkDevice device = new NetworkDevice(groupInstance.deviceId);
					mDatabase.reconstruct(device);

					new DeviceChooserDialog(getActivity(), mDatabase, device, new DeviceChooserDialog.OnDeviceSelectedListener()
					{
						@Override
						public void onDeviceSelected(NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> connectionList)
						{
							transaction.flag = TransactionObject.Flag.RESUME;
							groupInstance.connectionAdapter = connection.adapterName;

							mDatabase.publish(transaction);
							mDatabase.publish(groupInstance);

							getContext().startService(new Intent(mDatabase.getContext(), ServerService.class)
									.setAction(ServerService.ACTION_START_RECEIVING)
									.putExtra(CommunicationService.EXTRA_GROUP_ID, transaction.groupId));
						}
					}).show();
				} catch (Exception e) {
					Toast.makeText(getActivity(), R.string.mesg_deviceNotExits, Toast.LENGTH_LONG).show();
				}
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
