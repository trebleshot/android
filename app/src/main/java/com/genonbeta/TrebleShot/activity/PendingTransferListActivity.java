package com.genonbeta.TrebleShot.activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.PendingTransferListAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.fragment.PendingTransferListFragment;
import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
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

	public static final int REQUEST_CHOOSE_FOLDER = 1;

	private Transaction mTransaction;
	private PendingTransferListFragment mPendingFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.layout_transaction_editor);

		mTransaction = new Transaction(this);
		mPendingFragment = (PendingTransferListFragment) getSupportFragmentManager().findFragmentById(R.id.layout_transaction_editor_fragment_pendinglist);

		TextView closeTextView = (TextView) findViewById(R.id.layout_transaction_editor_close_text);
		TextView removeTextView = (TextView) findViewById(R.id.layout_transaction_editor_remove_text);
		TextView saveToTextView = (TextView) findViewById(R.id.layout_transaction_editor_saveto_text);

		closeTextView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				finish();
			}
		});

		saveToTextView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivityForResult(new Intent(PendingTransferListActivity.this, FilePickerActivity.class)
						.setAction(FilePickerActivity.ACTION_CHOOSE_DIRECTORY), REQUEST_CHOOSE_FOLDER);
			}
		});

		removeTextView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder dialog = new AlertDialog.Builder(PendingTransferListActivity.this);

				dialog.setTitle(R.string.question_removeAll);
				dialog.setMessage(R.string.text_removeCertainPendingTransfersSummary);

				dialog.setNegativeButton(R.string.butn_cancel, null);
				dialog.setPositiveButton(R.string.butn_removeAll, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						mTransaction
								.edit()
								.removeTransaction(mPendingFragment.getAdapter().getSelect())
								.done();
					}
				});

				dialog.show();
			}
		});

		mPendingFragment.setSelect(createLoader());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (data != null) {
			if (resultCode == Activity.RESULT_OK) {
				switch (requestCode) {
					case REQUEST_CHOOSE_FOLDER:
						if (data.hasExtra(FilePickerActivity.EXTRA_CHOSEN_PATH)) {
							String selectedPath = data.getStringExtra(FilePickerActivity.EXTRA_CHOSEN_PATH);
							ArrayList<CursorItem> list = mTransaction.getTable(mPendingFragment.getAdapter().getSelect());
							Transaction.EditingSession editingSession = mTransaction.edit();

							for (CursorItem cursorItem : list) {
								Log.d(PendingTransferListActivity.class.getSimpleName(), "We're there");

								if (cursorItem.getInt(Transaction.FIELD_TRANSFER_TYPE) != Transaction.TYPE_TRANSFER_TYPE_INCOMING)
									continue;

								AwaitedFileReceiver fileReceiver = new AwaitedFileReceiver(cursorItem);

								Log.d(PendingTransferListActivity.class.getSimpleName(), "Going on");

								if (Transaction.Flag.PENDING.equals(fileReceiver.flag)) {
									Log.d(PendingTransferListActivity.class.getSimpleName(), "Done");

									ContentValues values = new ContentValues();
									values.put(Transaction.FIELD_TRANSFER_FILE, selectedPath);

									editingSession.updateTransaction(fileReceiver.requestId, values);
								}
							}

							editingSession.done();

							Toast.makeText(this, "We're okay: " + data.getStringExtra(FilePickerActivity.EXTRA_CHOSEN_PATH), Toast.LENGTH_SHORT).show();
						}
						break;
				}
			}
		}
	}

	public SQLQuery.Select createLoader()
	{
		SQLQuery.Select select = new SQLQuery.Select(Transaction.TABLE_TRANSFER);

		select.getItems()
				.put(PendingTransferListAdapter.FLAG_GROUP, false);

		if (ACTION_LIST_TRANSFERS.equals(getIntent().getAction()) && getIntent().hasExtra(EXTRA_DEVICE_ID))
			select.setWhere(Transaction.FIELD_TRANSFER_DEVICEID + "=?", getIntent().getStringExtra(EXTRA_DEVICE_ID));
		else if (ACTION_LIST_TRANSFERS.equals(getIntent().getAction()) && getIntent().hasExtra(EXTRA_GROUP_ID))
			select.setWhere(Transaction.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(getIntent().getIntExtra(EXTRA_GROUP_ID, -1)));

		return select;
	}


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
}
