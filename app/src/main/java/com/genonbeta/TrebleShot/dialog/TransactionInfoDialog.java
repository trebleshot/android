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
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TextUtils;

import java.io.File;

/**
 * created by: Veli
 * date: 10.11.2017 14:59
 */

public class TransactionInfoDialog extends AlertDialog.Builder
{
	public TransactionInfoDialog(@NonNull final Context context, final AccessDatabase database, final TransactionObject transactionObject)
	{
		super(context);

		final TransactionObject.Group group = new TransactionObject.Group(transactionObject.groupId);

		try {
			database.reconstruct(group);
			database.reconstruct(transactionObject);

			final File pseudoFile = FileUtils.getIncomingPseudoFile(getContext(), transactionObject, group);
			boolean fileExists = pseudoFile.isFile() && pseudoFile.canRead();

			@SuppressLint("InflateParams")
			View rootView = LayoutInflater.from(context).inflate(R.layout.layout_transaction_info, null);

			TextView nameText = rootView.findViewById(R.id.transaction_info_file_name);
			TextView sizeText = rootView.findViewById(R.id.transaction_info_file_size);
			TextView typeText = rootView.findViewById(R.id.transaction_info_file_mime);
			TextView flagText = rootView.findViewById(R.id.transaction_info_file_status);

			View incomingDetailsLayout = rootView.findViewById(R.id.transaction_info_incoming_details_layout);
			TextView receivedSizeText = rootView.findViewById(R.id.transaction_info_received_size);
			TextView locationText = rootView.findViewById(R.id.transaction_info_pseudo_location);

			setTitle(R.string.text_transactionDetails);
			setView(rootView);

			nameText.setText(transactionObject.friendlyName);
			sizeText.setText(FileUtils.sizeExpression(transactionObject.fileSize, false));
			typeText.setText(transactionObject.fileMimeType);
			flagText.setText(TextUtils.getTransactionFlagString(transactionObject.flag));
			receivedSizeText.setText(fileExists ? FileUtils.sizeExpression(pseudoFile.length(), false) : "-");
			locationText.setText(pseudoFile.getAbsolutePath());

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

			if (TransactionObject.Type.INCOMING.equals(transactionObject.type)) {
				incomingDetailsLayout.setVisibility(View.VISIBLE);

				if (fileExists
						&& TransactionObject.Flag.REMOVED.equals(transactionObject.flag))
					setNeutralButton(R.string.butn_saveAnyway, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							AlertDialog.Builder saveAnyway = new AlertDialog.Builder(getContext());

							saveAnyway.setTitle(R.string.ques_saveAnyway);
							saveAnyway.setMessage(R.string.text_saveAnywaySummary);
							saveAnyway.setNegativeButton(R.string.butn_cancel, null);
							saveAnyway.setPositiveButton(R.string.butn_proceed, new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									FileUtils.saveReceivedFile(pseudoFile, transactionObject);
									database.remove(transactionObject);

									Toast.makeText(getContext(), R.string.mesg_fileSaved, Toast.LENGTH_SHORT).show();
								}
							});

							saveAnyway.show();
						}
					});
				else if (TransactionObject.Flag.INTERRUPTED.equals(transactionObject.flag))
					setNeutralButton(R.string.butn_resume, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							transactionObject.flag = TransactionObject.Flag.PENDING;

							database.publish(transactionObject);

							AppUtils.startForegroundService(getContext(), new Intent(getContext(), CommunicationService.class)
									.setAction(CommunicationService.ACTION_SEAMLESS_RECEIVE)
									.putExtra(CommunicationService.EXTRA_GROUP_ID, transactionObject.groupId));
						}
					});
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
