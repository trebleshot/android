package com.genonbeta.TrebleShot.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
import com.genonbeta.TrebleShot.helper.AwaitedFileSender;
import com.genonbeta.TrebleShot.helper.AwaitedTransaction;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 4/15/17 1:16 AM
 */

public class Transaction extends MainDatabase
{
	public static final String TAG = Transaction.class.getSimpleName();

	public static final String ACTION_TRANSACTION_CHANGE = "com.genonbeta.TrebleShot.intent.action.TRANSACTION_CHANGE";

	public static final String EXTRA_COUNT_REMOVED = "countRemoved";
	public static final String EXTRA_COUNT_REGISTERED = "countRegistered";
	public static final String EXTRA_COUNT_UPDATED = "countUpdated";

	public Transaction(Context context)
	{
		super(context);
	}

	public EditingSession edit()
	{
		return new EditingSession();
	}

	public ArrayList<AwaitedFileReceiver> getReceivers()
	{
		return getReceivers(new SQLQuery.Select(TABLE_TRANSFER)
				.setWhere(FIELD_TRANSFER_TYPE + "=?", String.valueOf(TYPE_TRANSFER_TYPE_INCOMING)));
	}

	public ArrayList<AwaitedFileReceiver> getReceivers(SQLQuery.Select select)
	{
		ArrayList<CursorItem> list = getTable(select);
		ArrayList<AwaitedFileReceiver> outputList = new ArrayList<>();

		for (CursorItem item : list)
			outputList.add(new AwaitedFileReceiver(item));

		return outputList;
	}

	public ArrayList<AwaitedFileSender> getSenders()
	{
		return getSenders(new SQLQuery.Select(TABLE_TRANSFER)
				.setWhere(FIELD_TRANSFER_TYPE + "=?", String.valueOf(TYPE_TRANSFER_TYPE_OUTGOING)));
	}

	public ArrayList<AwaitedFileSender> getSenders(SQLQuery.Select select)
	{
		ArrayList<CursorItem> list = getTable(select);
		ArrayList<AwaitedFileSender> outputList = new ArrayList<>();

		for (CursorItem item : list)
			outputList.add(new AwaitedFileSender(item));

		return outputList;
	}

	public CursorItem getTransaction(int requestId)
	{
		return getFirstFromTable(new SQLQuery.Select(TABLE_TRANSFER)
				.setWhere(FIELD_TRANSFER_ID + "=?", String.valueOf(requestId)));
	}

	public boolean transactionExists(int requestId)
	{
		return getFirstFromTable(new SQLQuery.Select(TABLE_TRANSFER)
				.setWhere(FIELD_TRANSFER_ID + "=?", String.valueOf(requestId))) != null;
	}

	public boolean transactionGroupExists(int groupId)
	{
		return getFirstFromTable(new SQLQuery.Select(TABLE_TRANSFER)
				.setWhere(FIELD_TRANSFER_GROUPID + "=?", String.valueOf(groupId))) != null;
	}

	public enum Flag
	{
		PENDING,
		RESUME,
		RUNNING,
		INTERRUPTED
	}

	public class EditingSession
	{
		private boolean mClosed = false;

		private int mCountRegistered = 0;
		private int mCountRemoved = 0;
		private int mCountUpdated = 0;

		public void done()
		{
			if (mCountRegistered == 0 && mCountRemoved == 0 && mCountUpdated == 0)
				return;

			Intent updateIntent = new Intent(ACTION_TRANSACTION_CHANGE);

			updateIntent.putExtra(EXTRA_COUNT_REGISTERED, mCountRegistered);
			updateIntent.putExtra(EXTRA_COUNT_REMOVED, mCountRemoved);
			updateIntent.putExtra(EXTRA_COUNT_UPDATED, mCountUpdated);

			getContext().sendBroadcast(updateIntent);

			mClosed = true;
		}

		public EditingSession registerTransaction(AwaitedTransaction transaction)
		{
			getWritableDatabase().insert(TABLE_TRANSFER, null, transaction.getDatabaseObject());

			if (getAffectedRowCount() > 0)
				mCountRegistered++;

			return this;
		}

		public EditingSession removeDeviceTransactionGroup(AwaitedTransaction transaction)
		{
			return removeDeviceTransactionGroup(transaction.deviceId);
		}

		public EditingSession removeDeviceTransactionGroup(NetworkDevice device)
		{
			return removeDeviceTransactionGroup(device.deviceId);
		}

		public EditingSession removeDeviceTransactionGroup(String deviceId)
		{
			return removeTransaction(new SQLQuery.Select(TABLE_TRANSFER)
					.setWhere(FIELD_TRANSFER_DEVICEID + "=?", deviceId));
		}

		public EditingSession removeTransaction(AwaitedTransaction transaction)
		{
			return removeTransaction(transaction.requestId);
		}

		public EditingSession removeTransaction(int requestId)
		{
			return removeTransaction(new SQLQuery.Select(TABLE_TRANSFER)
					.setWhere(FIELD_TRANSFER_ID + "=?", String.valueOf(requestId)));
		}

		public EditingSession removeTransactionGroup(AwaitedTransaction transaction)
		{
			return removeTransactionGroup(transaction.groupId);
		}

		public EditingSession removeTransactionGroup(int groupId)
		{
			return removeTransaction(new SQLQuery.Select(TABLE_TRANSFER)
					.setWhere(FIELD_TRANSFER_GROUPID + "=?", String.valueOf(groupId)));
		}

		public EditingSession removeTransaction(SQLQuery.Select select)
		{
			delete(select);

			mCountRemoved += getAffectedRowCount();

			return this;
		}

		public EditingSession updateAccessPort(int requestId, int port)
		{
			ContentValues values = new ContentValues();
			values.put(FIELD_TRANSFER_ACCESSPORT, port);

			updateTransaction(requestId, values);

			return this;
		}

		public EditingSession updateFlag(int requestId, Flag flag)
		{
			ContentValues values = new ContentValues();
			values.put(FIELD_TRANSFER_FLAG, flag.toString());

			updateTransaction(requestId, values);

			return this;
		}

		public EditingSession updateFlagGroup(int groupId, Flag flag)
		{
			ContentValues values = new ContentValues();
			values.put(FIELD_TRANSFER_FLAG, flag.toString());

			updateTransactionGroup(groupId, values);

			return this;
		}

		public EditingSession updateTransaction(AwaitedTransaction transaction)
		{
			return updateTransaction(transaction.requestId, transaction.getDatabaseObject());
		}

		public EditingSession updateTransaction(int requestId, ContentValues values)
		{
			return updateTransaction(new SQLQuery.Select(TABLE_TRANSFER)
					.setWhere(FIELD_TRANSFER_ID + "=?", String.valueOf(requestId)), values);
		}

		public EditingSession updateTransaction(SQLQuery.Select select, ContentValues values)
		{
			update(select, values);

			mCountUpdated += getAffectedRowCount();

			return this;
		}

		public EditingSession updateTransactionGroup(int groupId, ContentValues values)
		{
			return updateTransaction(new SQLQuery.Select(TABLE_TRANSFER)
					.setWhere(FIELD_TRANSFER_GROUPID + "=?", String.valueOf(groupId)), values);
		}
	}

	public class ClosedSessionException extends Exception
	{
	}
}
