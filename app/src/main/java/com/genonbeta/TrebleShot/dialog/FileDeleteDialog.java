package com.genonbeta.TrebleShot.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.support.v7.app.AlertDialog;

import com.genonbeta.TrebleShot.R;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/21/17 2:21 AM
 */

public class FileDeleteDialog extends AlertDialog.Builder
{
	public FileDeleteDialog(final Activity activity, final Object[] items, final Listener listener)
	{
		super(activity);

		final ArrayList<URI> files = new ArrayList<>();

		for (Object path : items)
			files.add(URI.create(path.toString()));

		setTitle(R.string.text_deleteConfirm);
		setMessage(activity.getResources().getQuantityString(R.plurals.ques_deleteFile, files.size(), files.size()));
		setNegativeButton(R.string.butn_cancel, null);
		setPositiveButton(R.string.butn_delete, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dailog, int p2)
					{
						new Thread(new Runnable()
						{
							@Override
							public void run()
							{
								Looper.prepare();

								File parentDir = null;

								for (URI filePath : files) {
									File file = new File(filePath);

									if (parentDir == null)
										parentDir = file.getParentFile();

									if (file.delete() && listener != null)
										listener.onFileDeletion(activity, file);
								}

								if (listener != null)
									listener.onCompleted(activity, files.size(), parentDir);

								Looper.loop();
							}
						}).start();
					}
				}
		);
	}

	public interface Listener
	{
		void onFileDeletion(Context context, File file);

		void onCompleted(Context context, int fileSize, File parent);
	}
}
