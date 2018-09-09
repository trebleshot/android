package com.genonbeta.TrebleShot.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.io.StreamInfo;

import org.w3c.dom.Document;

import java.io.IOException;

/**
 * created by: Veli
 * date: 10.11.2017 14:59
 */

public class TransactionInfoDialog extends AlertDialog.Builder
{
	public TransactionInfoDialog(@NonNull final Context context, final AccessDatabase database, SharedPreferences preferences, final TransferObject transferObject)
	{
		super(context);

		final TransferGroup group = new TransferGroup(transferObject.groupId);

		try {
			database.reconstruct(group);
			database.reconstruct(transferObject);

			DocumentFile attemptedFile = null;

			try {
				// This file should also indicate the received file if it is 'done'
				attemptedFile = FileUtils.getIncomingPseudoFile(getContext(), preferences, transferObject, group, false);
			} catch (Exception e) {
				e.printStackTrace();
			}

			final DocumentFile pseudoFile = attemptedFile;
			boolean fileExists = pseudoFile != null && pseudoFile.canRead();

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

			nameText.setText(transferObject.friendlyName);
			sizeText.setText(FileUtils.sizeExpression(transferObject.fileSize, false));
			typeText.setText(transferObject.fileMimeType);
			flagText.setText(TextUtils.getTransactionFlagString(transferObject.flag));

			receivedSizeText.setText(fileExists
					? FileUtils.sizeExpression(pseudoFile.length(), false)
					: getContext().getString(R.string.text_unknown));

			locationText.setText(fileExists
					? pseudoFile.getUri().toString()
					: getContext().getString(R.string.text_unknown));

			setPositiveButton(R.string.butn_close, null);
			setNegativeButton(R.string.butn_remove, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialogInterface, int i)
				{
					AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());

					dialog.setTitle(R.string.ques_removeQueue);
					dialog.setMessage(getContext().getString(R.string.text_removePendingTransferSummary, transferObject.friendlyName));

					dialog.setNegativeButton(R.string.butn_close, null);
					dialog.setPositiveButton(R.string.butn_proceed, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							database.remove(transferObject);
						}
					}).show();
				}
			});

			if (TransferObject.Type.INCOMING.equals(transferObject.type)) {
				incomingDetailsLayout.setVisibility(View.VISIBLE);

				if (TransferObject.Flag.INTERRUPTED.equals(transferObject.flag)
						|| TransferObject.Flag.IN_PROGRESS.equals(transferObject.flag)) {
					setNeutralButton(R.string.butn_resume, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							transferObject.flag = TransferObject.Flag.PENDING;
							database.publish(transferObject);

							CursorItem assigneeInstance = database.getFirstFromTable(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
									.setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(group.groupId)));

							if (assigneeInstance != null) {
								TransferGroup.Assignee assignee = new TransferGroup.Assignee();
								assignee.reconstruct(assigneeInstance);

								TransferUtils.resumeTransfer(getContext(), group, assignee);
							}
						}
					});
				} else if (fileExists && pseudoFile.getParentFile() != null) {
					if (TransferObject.Flag.REMOVED.equals(transferObject.flag)) {
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
										try {
											FileUtils.saveReceivedFile(pseudoFile.getParentFile(), pseudoFile, transferObject);

											database.remove(transferObject);

											Toast.makeText(getContext(), R.string.mesg_fileSaved, Toast.LENGTH_SHORT).show();
										} catch (IOException e) {
											e.printStackTrace();
										}
									}
								});

								saveAnyway.show();
							}
						});
					} else if (TransferObject.Flag.DONE.equals(transferObject.flag)) {
						setNeutralButton(R.string.butn_open, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								try {
									getContext().startActivity(new Intent(FileUtils.applySecureOpenIntent(getContext(), pseudoFile, new Intent(Intent.ACTION_VIEW))));
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
					}
				}
			} else if (TransferObject.Type.OUTGOING.equals(transferObject.type)) {
				try {
					final DocumentFile sharedFile = FileUtils.fromUri(getContext(), Uri.parse(transferObject.file));
					final Intent startIntent = new Intent(FileUtils.applySecureOpenIntent(getContext(), sharedFile, new Intent(Intent.ACTION_VIEW)));

					setNeutralButton(R.string.butn_open, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							try {
								getContext().startActivity(startIntent);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				} catch (Exception e) {

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
