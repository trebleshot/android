package com.genonbeta.TrebleShot.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
import com.genonbeta.TrebleShot.helper.FileUtils;

import java.io.File;

/**
 * Created by: veli
 * Date: 5/30/17 4:57 PM
 */

public class FixFilePathDialog extends AlertDialog.Builder
{
	public FixFilePathDialog(@NonNull final Context context, final Transaction transaction, final AwaitedFileReceiver receiver, FileUtils.Conflict conflict)
	{
		super(context);

		setTitle(R.string.text_fixFileSaveLocation);

		if (FileUtils.Conflict.SET_PATH_UNAVAILABLE.equals(conflict)) {
			setMessage(getContext().getString(R.string.text_fileConflictUseDefault, receiver.fileName));

			setPositiveButton(R.string.butn_proceed, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					fixConflict(context, transaction, receiver, false);
				}
			});
		} else {
			setMessage(getContext().getString(R.string.text_fileConflictNoChoice, receiver.fileName));

			setNeutralButton(R.string.butn_keepFile, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					fixConflict(context, transaction, receiver, false);
				}
			});

			setPositiveButton(R.string.butn_newFile, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					fixConflict(context, transaction, receiver, true);
				}
			});
		}

		setNegativeButton(R.string.butn_close, null);
	}

	public void fixConflict(Context context, Transaction transaction, AwaitedFileReceiver receiver, boolean newFile)
	{
		receiver.fileAddress = null;

		if (newFile) {
			File uniqueFile = FileUtils.getUniqueFile(new File(FileUtils.getSaveLocationForFile(context, receiver.fileName)));
			receiver.fileName = uniqueFile.getName();
		}

		transaction
				.edit()
				.updateTransaction(receiver)
				.done();
	}
}
