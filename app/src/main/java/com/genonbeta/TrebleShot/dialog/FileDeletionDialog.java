package com.genonbeta.TrebleShot.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.DynamicNotification;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/21/17 2:21 AM
 */

public class FileDeletionDialog<T extends Shareable> extends AlertDialog.Builder
{
	public static final String TAG = FileDeletionDialog.class.getSimpleName();
	public static final int JOB_FILE_DELETION = 1;

	public ArrayList<T> mItemList = new ArrayList<>();

	public FileDeletionDialog(final Activity activity, final ArrayList<T> items, final Listener listener)
	{
		super(activity);

		getItemList().addAll(items);

		setTitle(R.string.text_deleteConfirm);
		setMessage(R.string.text_previewThenProceedNotice);
		setNegativeButton(R.string.butn_cancel, null);
		setNeutralButton(getContext().getString(R.string.butn_queueCounted, 0), null);
		setPositiveButton(R.string.butn_delete, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dailog, int p2)
					{
						WorkerService.run(activity, new WorkerService.NotifiableRunningTask(TAG, JOB_FILE_DELETION)
						{
							int totalDeletion = 0;

							@Override
							public void onUpdateNotification(DynamicNotification dynamicNotification, UpdateType updateType)
							{
								dynamicNotification.setSmallIcon(R.drawable.ic_delete_white_24dp);

								switch (updateType) {
									case Started:
										dynamicNotification.setContentText(getContext().getString(R.string.text_deletingFilesOngoing));
										break;
									case Done:
										dynamicNotification.setContentText(getContext()
												.getResources()
												.getQuantityString(R.plurals.text_fileDeletionCompleted, totalDeletion, totalDeletion));
										break;
								}
							}

							@Override
							public void onRun()
							{
								for (T shareable : getItemList())
									if (shareable.isSelectableSelected())
										delete(new File(URI.create(shareable.uri.toString())));

								if (listener != null)
									listener.onCompleted(this, activity, totalDeletion);
							}

							private void delete(File file)
							{
								if (getInterrupter().interrupted())
									return;

								boolean isDirectory = file.isDirectory();

								if (isDirectory)
									deleteDirectory(file);

								if (file.delete()) {
									if (!isDirectory)
										totalDeletion++;

									listener.onFileDeletion(this, activity, file);
								}
							}

							private void deleteDirectory(File folder)
							{
								File[] files = folder.listFiles();

								if (files != null)
									for (File anotherFile : files)
										delete(anotherFile);
							}
						});
					}
				}
		);
	}

	public int countSelections()
	{
		int selectedCount = 0;

		synchronized (getItemList()) {
			for (T shareable : getItemList())
				if (shareable.isSelectableSelected())
					selectedCount++;
		}

		return selectedCount;
	}

	public ArrayList<T> getItemList()
	{
		return mItemList;
	}

	@Override
	public AlertDialog show()
	{
		final AlertDialog alertDialog = super.show();
		final Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
		final Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);

		positiveButton.setVisibility(View.GONE);

		neutralButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				new SelectionEditorDialog<>(getContext(), getItemList()).show().setOnDismissListener(new DialogInterface.OnDismissListener()
				{
					@Override
					public void onDismiss(DialogInterface dialog)
					{
						int totalSelected = countSelections();

						neutralButton.setText(getContext().getString(R.string.butn_queueCounted, totalSelected));
						alertDialog.setMessage(getContext().getResources().getQuantityString(R.plurals.ques_deleteFile, totalSelected, totalSelected));

						positiveButton.setVisibility(countSelections() > 0
								? View.VISIBLE
								: View.GONE);
					}
				});
			}
		});

		return alertDialog;
	}

	public interface Listener
	{
		void onFileDeletion(WorkerService.RunningTask runningTask, Context context, File file);

		void onCompleted(WorkerService.RunningTask runningTask, Context context, int fileSize);
	}
}
