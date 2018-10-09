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
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.android.framework.io.DocumentFile;

import java.io.IOException;

/**
 * created by: Veli
 * date: 10.11.2017 14:59
 */

public class TransactionInfoDialog extends AlertDialog.Builder
{
	public TransactionInfoDialog(@NonNull final Context context, final TransferObject transferObject)
	{
		super(context);

		final TransferGroup group = new TransferGroup(transferObject.groupId);

		try {
			AppUtils.getDatabase(context).reconstruct(group);
			AppUtils.getDatabase(context).reconstruct(transferObject);

			DocumentFile attemptedFile = null;
			boolean isIncoming = TransferObject.Type.INCOMING.equals(transferObject.type);

			try {
				// If it is incoming than get the received or cache file
				// If not then try to reach to the source file that is being send
				attemptedFile = isIncoming
						? FileUtils.getIncomingPseudoFile(getContext(), AppUtils.getDefaultPreferences(context), transferObject, group, false)
						: FileUtils.fromUri(getContext(), Uri.parse(transferObject.file));
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
					? FileUtils.getReadableUri(pseudoFile.getUri())
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
							AppUtils.getDatabase(context).remove(transferObject);
						}
					}).show();
				}
			});

			if (isIncoming) {
				incomingDetailsLayout.setVisibility(View.VISIBLE);

				if (TransferObject.Flag.INTERRUPTED.equals(transferObject.flag)
						|| TransferObject.Flag.IN_PROGRESS.equals(transferObject.flag)) {
					setNeutralButton(R.string.butn_retry, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							transferObject.flag = TransferObject.Flag.PENDING;
							AppUtils.getDatabase(context).publish(transferObject);
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

											AppUtils.getDatabase(context).remove(transferObject);

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
				// Could make use of this statement later
				if (fileExists) {
					try {
						final Intent startIntent = new Intent(FileUtils.applySecureOpenIntent(getContext(), attemptedFile, new Intent(Intent.ACTION_VIEW)));

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
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
