package com.genonbeta.TrebleShot.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

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

public class FileDeleteDialog<T extends Shareable> extends AlertDialog.Builder
{
	public static final long JOB_FILE_DELETION = 1;

	public FileDeleteDialog(final Activity activity, final ArrayList<T> items, final Listener listener)
	{
		super(activity);

		final ArrayList<T> itemClones = new ArrayList<>(items);

		setTitle(R.string.text_deleteConfirm);
		setMessage(activity.getResources().getQuantityString(R.plurals.ques_deleteFile, itemClones.size(), itemClones.size()));
		setNegativeButton(R.string.butn_cancel, null);
		setPositiveButton(R.string.butn_delete, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dailog, int p2)
					{
						WorkerService.run(activity, new WorkerService.NotifiableRunningTask()
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
							public long getJobId()
							{
								return JOB_FILE_DELETION;
							}

							@Override
							public void onRun()
							{
								for (T shareable : itemClones)
									delete(new File(URI.create(shareable.uri.toString())));

								if (listener != null)
									listener.onCompleted(this, activity, totalDeletion);
							}

							private void delete(File file)
							{
								if (getInterrupter().interrupted())
									return;

								if (file.isDirectory())
									deleteDirectory(file);

								if (file.delete()) {
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

	public interface Listener
	{
		void onFileDeletion(WorkerService.RunningTask runningTask, Context context, File file);

		void onCompleted(WorkerService.RunningTask runningTask, Context context, int fileSize);
	}
}
