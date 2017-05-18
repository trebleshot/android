package com.genonbeta.TrebleShot.dialog;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.PendingTransferListAdapter;
import com.genonbeta.TrebleShot.database.DeviceRegistry;
import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.ServerService;
import com.genonbeta.android.database.CursorItem;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/18/17 6:09 PM
 */

public class PendingTransferListDialog extends Dialog
{
	public PendingTransferListDialog(final Context context, final DeviceRegistry deviceRegistry, final Transaction transaction, int groupId)
	{
		super(context, android.R.style.Theme_Light_NoTitleBar);
		initialize(context, deviceRegistry, transaction, new PendingTransferListAdapter(getContext(), groupId));
	}

	public PendingTransferListDialog(final Context context, final DeviceRegistry deviceRegistry, final Transaction transaction, String deviceId)
	{
		super(context, android.R.style.Theme_Light_NoTitleBar);
		initialize(context, deviceRegistry, transaction, new PendingTransferListAdapter(getContext(), deviceId));
	}

	private void initialize(final Context context, final DeviceRegistry deviceRegistry, final Transaction transaction, final PendingTransferListAdapter adapter)
	{
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_transaction_editor, null);
		TextView closeTextView = (TextView) view.findViewById(R.id.layout_transaction_editor_close_text);
		TextView removeTextView = (TextView) view.findViewById(R.id.layout_transaction_editor_remove_text);
		ListView listListView = (ListView) view.findViewById(R.id.layout_transaction_editor_list_main);

		listListView.setAdapter(adapter);
		listListView.setDividerHeight(0);

		closeTextView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				cancel();
			}
		});

		removeTextView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());

				dialog.setTitle(R.string.dialog_title_remove_certain_transactions);
				dialog.setMessage(R.string.dialog_message_remove_certain_transaction);

				dialog.setNegativeButton(R.string.cancel, null);
				dialog.setPositiveButton(R.string.remove_all, new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						transaction.getWritableDatabase().delete(adapter.getSelect().tableName, adapter.getSelect().where, adapter.getSelect().whereArgs);
						cancel();
					}
				});

				dialog.show();
			}
		});

		listListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				CursorItem thisItem = (CursorItem) adapter.getItem(position);

				if (thisItem.getInt(MainDatabase.FIELD_TRANSFER_TYPE) == MainDatabase.TYPE_TRANSFER_TYPE_INCOMING)
				{
					final AwaitedFileReceiver receiver = new AwaitedFileReceiver(thisItem);
					final NetworkDevice device = deviceRegistry.getNetworkDeviceById(receiver.deviceId);

					if (device == null)
						Toast.makeText(getContext(), R.string.warning_device_not_exits, Toast.LENGTH_SHORT).show();
					else if (receiver.flag.equals(Transaction.Flag.INTERRUPTED))
					{
						if (device.availableConnections.length > 0)
						{
							new DeviceChooserDialog(context, device, new DeviceChooserDialog.OnDeviceSelectedListener()
							{
								@Override
								public void onDeviceSelected(DeviceChooserDialog.AddressHolder addressHolder, ArrayList<DeviceChooserDialog.AddressHolder> availableInterfaces)
								{
									continueReceiving(transaction, receiver, addressHolder.address);
								}
							}).show();
						}
						else
						{
							continueReceiving(transaction, receiver, device.ip);
						}
					}
				}
			}
		});

		adapter.update();
		adapter.notifyDataSetChanged();

		setContentView(view);
	}

	public void continueReceiving(Transaction transaction, AwaitedFileReceiver receiver, String ipAddress)
	{
		if (!receiver.ip.equals(ipAddress))
		{
			receiver.ip = ipAddress;
			ContentValues values = new ContentValues();
			
			values.put(Transaction.FIELD_TRANSFER_USERIP, ipAddress);
			transaction.updateTransactionGroup(receiver.groupId, values);
		}

		receiver.flag = Transaction.Flag.RESUME;
		transaction.updateTransaction(receiver);

		transaction.getContext().startService(new Intent(transaction.getContext(), ServerService.class)
				.setAction(ServerService.ACTION_START_RECEIVING)
				.putExtra(CommunicationService.EXTRA_GROUP_ID, receiver.groupId));
	}
}
