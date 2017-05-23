package com.genonbeta.TrebleShot.activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.database.DeviceRegistry;
import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.dialog.DeviceChooserDialog;
import com.genonbeta.TrebleShot.fragment.PendingTransferListFragment;
import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.ServerService;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/23/17 1:43 PM
 */

public class PendingTransferListActivity extends Activity
{
	public static final String ACTION_LIST_TRANSFERS = "com.genonbeta.TrebleShot.action.LIST_TRANSFERS";

	public static final String EXTRA_GROUP_ID = "extraGroupId";
	public static final String EXTRA_DEVICE_ID = "extraDeviceId";

	public static void startInstance(Context context, String deviceId)
	{
		context.startActivity(new Intent(context, PendingTransferListActivity.class)
				.setAction(ACTION_LIST_TRANSFERS)
				.putExtra(EXTRA_DEVICE_ID, deviceId));
	}

	public static void startInstance(Context context, int groupId)
	{
		context.startActivity(new Intent(context, PendingTransferListActivity.class)
				.setAction(ACTION_LIST_TRANSFERS)
				.putExtra(EXTRA_GROUP_ID, groupId));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.layout_transaction_editor);

		final DeviceRegistry deviceRegistry = new DeviceRegistry(this);
		final Transaction transaction = new Transaction(this);
		final PendingTransferListFragment pendingFragment = (PendingTransferListFragment) getSupportFragmentManager().findFragmentById(R.id.layout_transaction_editor_fragment_pendinglist);

		TextView closeTextView = (TextView) findViewById(R.id.layout_transaction_editor_close_text);
		TextView removeTextView = (TextView) findViewById(R.id.layout_transaction_editor_remove_text);

		closeTextView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				finish();
			}
		});

		removeTextView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder dialog = new AlertDialog.Builder(PendingTransferListActivity.this);

				dialog.setTitle(R.string.dialog_title_remove_certain_transactions);
				dialog.setMessage(R.string.dialog_message_remove_certain_transaction);

				dialog.setNegativeButton(R.string.cancel, null);
				dialog.setPositiveButton(R.string.remove_all, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						transaction
								.edit()
								.removeTransaction(pendingFragment.getAdapter().getSelect())
								.done();
					}
				});

				dialog.show();
			}
		});

		pendingFragment.setSelect(createLoader());
		pendingFragment.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				final CursorItem thisItem = (CursorItem) pendingFragment.getAdapter().getItem(position);

				if (thisItem.getInt(MainDatabase.FIELD_TRANSFER_TYPE) == MainDatabase.TYPE_TRANSFER_TYPE_INCOMING)
				{
					final AwaitedFileReceiver receiver = new AwaitedFileReceiver(thisItem);
					final NetworkDevice device = deviceRegistry.getNetworkDeviceById(receiver.deviceId);

					if (device == null)
						Toast.makeText(PendingTransferListActivity.this, R.string.warning_device_not_exits, Toast.LENGTH_LONG).show();
					else if (receiver.flag.equals(Transaction.Flag.INTERRUPTED))
					{
						new DeviceChooserDialog(PendingTransferListActivity.this, device, new DeviceChooserDialog.OnDeviceSelectedListener()
						{
							@Override
							public void onDeviceSelected(DeviceChooserDialog.AddressHolder addressHolder, ArrayList<DeviceChooserDialog.AddressHolder> availableInterfaces)
							{
								Transaction.EditingSession editingSession = transaction.edit();

								if (!receiver.ip.equals(addressHolder.address))
								{
									receiver.ip = addressHolder.address;
									ContentValues values = new ContentValues();

									values.put(Transaction.FIELD_TRANSFER_USERIP, addressHolder.address);
									editingSession.updateTransactionGroup(receiver.groupId, values);
								}

								receiver.flag = Transaction.Flag.RESUME;

								editingSession
										.updateTransaction(receiver)
										.done();

								startService(new Intent(transaction.getContext(), ServerService.class)
										.setAction(ServerService.ACTION_START_RECEIVING)
										.putExtra(CommunicationService.EXTRA_GROUP_ID, receiver.groupId));
							}
						}).show();

					}
				}
			}
		});
	}

	public SQLQuery.Select createLoader()
	{
		SQLQuery.Select select = new SQLQuery.Select(Transaction.TABLE_TRANSFER);

		if (ACTION_LIST_TRANSFERS.equals(getIntent().getAction()) && getIntent().hasExtra(EXTRA_DEVICE_ID))
			select.setWhere(Transaction.FIELD_TRANSFER_DEVICEID + "=?", getIntent().getStringExtra(EXTRA_DEVICE_ID));
		else if (ACTION_LIST_TRANSFERS.equals(getIntent().getAction()) && getIntent().hasExtra(EXTRA_GROUP_ID))
			select.setWhere(Transaction.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(getIntent().getIntExtra(EXTRA_GROUP_ID, -1)));

		return select;
	}
}
