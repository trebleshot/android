package com.genonbeta.TrebleShot.fragment.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.helper.NotificationPublisher;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class FileDeleteDialogFragment extends DialogFragment
{
	private ArrayList<URI> mFiles = new ArrayList<URI>();
	private OnDeleteCompletedListener mDeleteListener = null;
	private Context mContext;

	public void setItems(List<URI> items)
	{
		mFiles.clear();
		mFiles.addAll(items);
	}

	public void setItems(Object[] items)
	{
		mFiles.clear();

		for (Object path : items)
			mFiles.add(URI.create(path.toString()));
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		this.mContext = getActivity();

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
		final NotificationPublisher publisher = new NotificationPublisher(getActivity().getApplicationContext());

		dialogBuilder.setTitle(R.string.delete_confirm);
		dialogBuilder.setMessage(getString(R.string.delete_warning, mFiles.size()));

		dialogBuilder.setNegativeButton(R.string.cancel, null);

		dialogBuilder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener()
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

								for (URI filePath : mFiles)
								{
									File file = new File(filePath);
									file.delete();
								}

								if (mDeleteListener != null)
									mDeleteListener.onFilesDeleted(FileDeleteDialogFragment.this, mFiles.size());

								try
								{
									publisher.makeToast(getString(R.string.delete_completed, mFiles.size()));
								} catch (IllegalStateException e)
								{
									e.printStackTrace();
								}

								Looper.loop();
							}
						}
						).start();
					}
				}
		);

		return dialogBuilder.show();
	}

	@Override
	public void onPause()
	{
		super.onPause();

		FragmentTransaction ft = getFragmentManager().beginTransaction();

		ft.detach(this);
		ft.commit();
	}

	public Context getContext()
	{
		return this.mContext;
	}

	public void setOnDeleteCompletedListener(OnDeleteCompletedListener listener)
	{
		mDeleteListener = listener;
	}

	public static interface OnDeleteCompletedListener
	{
		public void onFilesDeleted(FileDeleteDialogFragment fragment, int fileSize);
	}
}
