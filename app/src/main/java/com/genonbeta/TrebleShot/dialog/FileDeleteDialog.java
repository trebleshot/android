package com.genonbeta.TrebleShot.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.support.v7.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.helper.NotificationUtils;

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

		setTitle(R.string.delete_confirm);
		setMessage(activity.getString(R.string.delete_warning, files.size()));
		setNegativeButton(R.string.cancel, null);
		setPositiveButton(R.string.delete, new DialogInterface.OnClickListener()
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

								for (URI filePath : files)
								{
									File file = new File(filePath);

									if (file.delete() && listener != null)
										listener.onFileDeletion(activity, file);
								}

								if (listener != null)
									listener.onCompleted(activity, files.size());

								Looper.loop();
							}
						}).start();
					}
				}
		);
	}

	public static interface Listener
	{
		public void onFileDeletion(Context context, File file);
		public void onCompleted(Context context, int fileSize);
	}
}
