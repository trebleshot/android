package com.genonbeta.TrebleShot.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.ServerService;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TransactionObject;

/**
 * created by: Veli
 * date: 10.11.2017 14:59
 */

public class TransactionInfoDialog extends AlertDialog.Builder
{
	public TransactionInfoDialog(@NonNull final Context context, final AccessDatabase database, final TransactionObject transactionObject)
	{
		super(context);

		try {
			database.reconstruct(transactionObject);

			@SuppressLint("InflateParams")
			View rootView = LayoutInflater.from(context).inflate(R.layout.layout_transaction_info, null);

			TextView nameText = rootView.findViewById(R.id.transaction_info_file_name);
			TextView sizeText = rootView.findViewById(R.id.transaction_info_file_size);
			TextView typeText = rootView.findViewById(R.id.transaction_info_file_mime);
			TextView flagText = rootView.findViewById(R.id.transaction_info_file_status);

			setTitle(R.string.text_transactionDetails);
			setView(rootView);

			nameText.setText(transactionObject.friendlyName);
			sizeText.setText(FileUtils.sizeExpression(transactionObject.fileSize, false));
			typeText.setText(transactionObject.fileMimeType);
			flagText.setText(TextUtils.getTransactionFlagString(transactionObject.flag));

			setPositiveButton(R.string.butn_close, null);
			setNegativeButton(R.string.butn_remove, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialogInterface, int i)
				{
					AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());

					dialog.setTitle(R.string.ques_removeQueue);
					dialog.setMessage(getContext().getString(R.string.text_removePendingTransferSummary, transactionObject.friendlyName));

					dialog.setNegativeButton(R.string.butn_close, null);
					dialog.setPositiveButton(R.string.butn_proceed, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							database.remove(transactionObject);
						}
					}).show();
				}
			});

			if (TransactionObject.Type.INCOMING.equals(transactionObject.type))
				setNeutralButton(R.string.butn_resume, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int i)
					{
						transactionObject.flag = TransactionObject.Flag.RESUME;

						database.publish(transactionObject);

						getContext().startService(new Intent(getContext(), ServerService.class)
								.setAction(ServerService.ACTION_START_RECEIVING)
								.putExtra(CommunicationService.EXTRA_GROUP_ID, transactionObject.groupId));
					}
				});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
