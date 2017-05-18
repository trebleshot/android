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
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by: veli
 * Date: 4/15/17 1:16 AM
 */

public class Transaction extends MainDatabase
{
	public static final String TAG = Transaction.class.getSimpleName();

	public static final String ACTION_TRANSACTION_REGISTERED = "com.genonbeta.TrebleShot.intent.action.TRANSACTION_REGISTERED";
	public static final String ACTION_TRANSACTION_UPDATED = "com.genonbeta.TrebleShot.intent.action.TRANSACTION_UPDATED";
	public static final String ACTION_TRANSACTION_REMOVED = "com.genonbeta.TrebleShot.intent.action.TRANSACTION_REMOVED";

	public enum Flag {
		PENDING,
		RESUME,
		RUNNING,
		INTERRUPTED
	}

	private ArrayBlockingQueue<AwaitedFileReceiver> mPendingReceivers = new ArrayBlockingQueue<AwaitedFileReceiver>(2000, true);

	public Transaction(Context context)
	{
		super(context);
	}

	public int acceptPendingReceivers(int groupId)
	{
		int count = 0;

		for (AwaitedFileReceiver receiver : getPendingReceivers())
		{
			if (receiver.groupId != groupId)
				continue;

			registerTransaction(receiver);
			getPendingReceivers().remove(receiver);

			count++;
		}

		return count;
	}

	public boolean applyAccessPort(int requestId, int port)
	{
		ContentValues values = new ContentValues();
		values.put(FIELD_TRANSFER_ACCESSPORT, port);

		return updateTransaction(requestId, values) > 0;
	}

	public ArrayList<AwaitedFileReceiver> getPendingReceiversByAcceptId(int groupId)
	{
		ArrayList<AwaitedFileReceiver> list = new ArrayList<AwaitedFileReceiver>();

		for (AwaitedFileReceiver receiver : getPendingReceivers())
			if (receiver.groupId == groupId)
				list.add(receiver);

		return list;
	}

	public ArrayBlockingQueue<AwaitedFileReceiver> getPendingReceivers()
	{
		return mPendingReceivers;
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

	protected long notifyRemoved()
	{
		getContext().sendBroadcast(new Intent(ACTION_TRANSACTION_REMOVED));
		return getAffectedRowCount();
	}

	protected long notifyUpdated()
	{
		getContext().sendBroadcast(new Intent(ACTION_TRANSACTION_REGISTERED));
		return getAffectedRowCount();
	}

	public boolean registerTransaction(AwaitedTransaction transaction)
	{
		getWritableDatabase().insert(TABLE_TRANSFER, null, transaction.getDatabaseObject());
		return notifyUpdated() > 0;
	}

	public int removePendingReceivers(int groupId)
	{
		int count = 0;

		for (AwaitedFileReceiver receiver : getPendingReceivers())
		{
			if (receiver.groupId != groupId)
				continue;

			getPendingReceivers().remove(receiver);

			count++;
		}

		return count;
	}

	public boolean removeDeviceTransactionGroup(NetworkDevice device)
	{
		getWritableDatabase().delete(TABLE_TRANSFER, FIELD_TRANSFER_DEVICEID + "=?", new String[]{String.valueOf(device.deviceId)});
		return notifyRemoved() > 0;
	}

	public boolean removeTransaction(AwaitedTransaction transaction)
	{
		return removeTransaction(transaction.requestId);
	}

	public boolean removeTransaction(int requestId)
	{
		getWritableDatabase().delete(TABLE_TRANSFER, FIELD_TRANSFER_ID + "=?", new String[]{String.valueOf(requestId)});
		return notifyRemoved() > 0;
	}

	public boolean removeTransactionGroup(AwaitedTransaction transaction)
	{
		return removeTransactionGroup(transaction.groupId);
	}

	public boolean removeTransactionGroup(int groupId)
	{
		getWritableDatabase().delete(TABLE_TRANSFER, FIELD_TRANSFER_GROUPID + "=?", new String[]{String.valueOf(groupId)});
		getContext().sendBroadcast(new Intent(ACTION_TRANSACTION_REMOVED));
		return notifyRemoved() > 0;
	}

	public boolean transactionExists(int requestId)
	{
		return getFirstFromTable(new SQLQuery.Select(TABLE_TRANSFER).setWhere(FIELD_TRANSFER_ID + "=?", String.valueOf(requestId))) != null;
	}

	public boolean updateFlag(int requestId, Flag flag)
	{
		ContentValues values = new ContentValues();
		values.put(FIELD_TRANSFER_FLAG, flag.toString());

		return updateTransaction(requestId, values) > 0;
	}

	public boolean updateFlagGroup(int groupId, Flag flag)
	{
		ContentValues values = new ContentValues();
		values.put(FIELD_TRANSFER_FLAG, flag.toString());

		return updateTransactionGroup(groupId, values) > 0;
	}

	public long updateTransaction(AwaitedTransaction transaction)
	{
		return updateTransaction(transaction.requestId, transaction.getDatabaseObject());
	}

	public long updateTransaction(int requestId, ContentValues values)
	{
		getWritableDatabase().update(TABLE_TRANSFER, values, FIELD_TRANSFER_ID + "=?", new String[] {String.valueOf(requestId)});
		return notifyUpdated();
	}

	public long updateTransactionGroup(int groupId, ContentValues values)
	{
		getWritableDatabase().update(TABLE_TRANSFER, values, FIELD_TRANSFER_GROUPID + "=?", new String[] {String.valueOf(groupId)});
		return notifyUpdated();
	}
}
