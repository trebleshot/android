package com.genonbeta.TrebleShot.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TransactionObject;

import java.io.File;

/**
 * created by: Veli
 * date: 21.11.2017 00:42
 */

public class TransactionGroupInfoDialog extends AlertDialog.Builder
{
	private AccessDatabase mDatabase;
	private TransactionObject.Group mGroup;
	private TransactionObject.Group.Index mTransactionIndex = new TransactionObject.Group.Index();

	public TransactionGroupInfoDialog(Context context, AccessDatabase database, TransactionObject.Group group)
	{
		super(context);

		mDatabase = database;
		mGroup = group;
	}

	public boolean calculateSpace()
	{
		long freeSpace = FileUtils.getSavePath(getContext(), mGroup).getFreeSpace();

		mDatabase.calculateTransactionSize(mGroup.groupId, getIndex());

		return freeSpace >= getIndex().incoming;
	}

	@SuppressLint("SetTextI18n")
	@Override
	public AlertDialog show()
	{
		@SuppressLint("InflateParams")
		View rootView = LayoutInflater.from(getContext()).inflate(R.layout.layout_transaction_group_info, null);

		TextView incomingSize = rootView.findViewById(R.id.transaction_group_info_incoming_size);
		TextView outgoingSize = rootView.findViewById(R.id.transaction_group_info_outgoing_size);
		TextView availableDisk = rootView.findViewById(R.id.transaction_group_info_available_disk_space);
		TextView savePath = rootView.findViewById(R.id.transaction_group_info_save_path);
		TextView usedConnection = rootView.findViewById(R.id.transaction_group_info_connection);

		File savePathFile = FileUtils.getSavePath(getContext(), mGroup);

		incomingSize.setText(FileUtils.sizeExpression(getIndex().incoming, false));
		outgoingSize.setText(FileUtils.sizeExpression(getIndex().outgoing, false));
		availableDisk.setText(FileUtils.sizeExpression(savePathFile.getFreeSpace(), false));
		savePath.setText(savePathFile.getAbsolutePath());

		NetworkDevice.Connection connection = new NetworkDevice.Connection(mGroup.deviceId, mGroup.connectionAdapter);

		try {
			mDatabase.reconstruct(connection);

			int nameRes = TextUtils.getAdapterName(connection);
			usedConnection.setText((nameRes == -1 ? connection.adapterName : getContext().getString(nameRes)));
		} catch (Exception e) {
			e.printStackTrace();
		}

		setTitle(R.string.text_transactionGroupDetails);
		setView(rootView);
		setPositiveButton(R.string.butn_close, null);

		return super.show();
	}

	public TransactionObject.Group.Index getIndex()
	{
		return mTransactionIndex;
	}
}
