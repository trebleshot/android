package com.genonbeta.TrebleShot.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.support.v7.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.Shareable;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/21/17 2:21 AM
 */

public class FileDeleteDialog<T extends Shareable> extends AlertDialog.Builder
{
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
						new Thread(new Runnable()
						{
							int totalDeletion = 0;

							@Override
							public void run()
							{
								Looper.prepare();

								for (T shareable : itemClones)
									delete(new File(URI.create(shareable.uri.toString())));

								if (listener != null)
									listener.onCompleted(activity, totalDeletion);

								Looper.loop();
							}

							private void delete(File file)
							{
								if (file.isDirectory())
									deleteDirectory(file);

								if (file.delete())
									totalDeletion++;

								listener.onFileDeletion(activity, file);
							}

							private void deleteDirectory(File folder)
							{
								for (File anotherFile : folder.listFiles())
									delete(anotherFile);
							}
						}).start();
					}
				}
		);
	}

	public interface Listener
	{
		void onFileDeletion(Context context, File file);

		void onCompleted(Context context, int fileSize);
	}
}
