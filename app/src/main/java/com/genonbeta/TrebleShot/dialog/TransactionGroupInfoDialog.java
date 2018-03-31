package com.genonbeta.TrebleShot.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.io.DocumentFile;
import com.genonbeta.TrebleShot.io.LocalDocumentFile;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TextUtils;

/**
 * created by: Veli
 * date: 21.11.2017 00:42
 */

public class TransactionGroupInfoDialog extends AlertDialog.Builder
{
	private AccessDatabase mDatabase;
	private TransactionObject.Group mGroup;
	private SharedPreferences mPreferences;
	private TransactionObject.Group.Index mTransactionIndex = new TransactionObject.Group.Index();

	public TransactionGroupInfoDialog(Context context, AccessDatabase database, SharedPreferences sharedPreferences, TransactionObject.Group group)
	{
		super(context);

		mDatabase = database;
		mGroup = group;
		mPreferences = sharedPreferences;
	}

	public boolean calculateSpace()
	{
		DocumentFile documentFile = FileUtils.getSavePath(getContext(), mPreferences, mGroup);

		long freeSpace = documentFile instanceof LocalDocumentFile
				? ((LocalDocumentFile) documentFile).getFile().getFreeSpace()
				: -1;

		mDatabase.calculateTransactionSize(mGroup.groupId, getIndex());

		return freeSpace == -1
				|| freeSpace >= getIndex().incoming;
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

		DocumentFile storageFile = FileUtils.getSavePath(getContext(), mPreferences, mGroup);
		Resources resources = getContext().getResources();

		incomingSize.setText(getContext().getString(R.string.mode_itemCountedDetailed,
				resources.getQuantityString(R.plurals.text_files, getIndex().incomingCount, getIndex().incomingCount),
				FileUtils.sizeExpression(getIndex().incoming, false)));

		outgoingSize.setText(getContext().getString(R.string.mode_itemCountedDetailed,
				resources.getQuantityString(R.plurals.text_files, getIndex().outgoingCount, getIndex().outgoingCount),
				FileUtils.sizeExpression(getIndex().outgoing, false)));

		availableDisk.setText(storageFile instanceof LocalDocumentFile
				? FileUtils.sizeExpression(((LocalDocumentFile) storageFile).getFile().getFreeSpace(), false)
				: getContext().getString(R.string.text_unknown));

		savePath.setText(storageFile.getUri().toString());

		NetworkDevice.Connection connection = new NetworkDevice.Connection(mGroup.deviceId, mGroup.connectionAdapter);

		try {
			mDatabase.reconstruct(connection);
			usedConnection.setText(TextUtils.getAdapterName(getContext(), connection));
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
