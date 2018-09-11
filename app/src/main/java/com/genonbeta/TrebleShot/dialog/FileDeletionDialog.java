package com.genonbeta.TrebleShot.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.DynamicNotification;
import com.genonbeta.android.framework.io.DocumentFile;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/21/17 2:21 AM
 */

public class FileDeletionDialog<T extends FileListAdapter.GenericFileHolder> extends AlertDialog.Builder
{
	public static final String TAG = FileDeletionDialog.class.getSimpleName();
	public static final int JOB_FILE_DELETION = 1;

	public FileDeletionDialog(final Activity activity, final ArrayList<T> items, final Listener listener)
	{
		super(activity);

		final ArrayList<T> copiedItems = new ArrayList<>(items);

		setTitle(R.string.text_deleteConfirm);
		setMessage(getContext().getResources().getQuantityString(R.plurals.ques_deleteFile, copiedItems.size(), copiedItems.size()));

		setNegativeButton(R.string.butn_cancel, null);
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
								switch (updateType) {
									case Started:
										dynamicNotification.setSmallIcon(R.drawable.ic_delete_white_24dp)
												.setContentText(getContext().getString(R.string.text_deletingFilesOngoing));
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
								for (T fileItem : copiedItems)
									delete(fileItem.file);

								if (listener != null)
									listener.onCompleted(this, activity, totalDeletion);
							}

							private void delete(DocumentFile file)
							{
								try {
									yell();

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
								} catch (ExitedException e) {
									e.printStackTrace();
								}
							}

							private void deleteDirectory(DocumentFile folder)
							{
								DocumentFile[] files = folder.listFiles();

								if (files != null)
									for (DocumentFile anotherFile : files)
										delete(anotherFile);
							}
						});
					}
				}
		);
	}

	public interface Listener
	{
		void onFileDeletion(WorkerService.RunningTask runningTask, Context context, DocumentFile file);

		void onCompleted(WorkerService.RunningTask runningTask, Context context, int fileSize);
	}
}
