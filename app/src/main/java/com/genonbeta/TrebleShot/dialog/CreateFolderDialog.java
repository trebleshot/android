package com.genonbeta.TrebleShot.dialog;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;

import java.io.File;

/**
 * Created by: veli
 * Date: 5/30/17 12:18 PM
 */

public class CreateFolderDialog extends AlertDialog.Builder
{
	private File mCurrentFolder;
	private OnCreatedListener mOnCreatedListener;
	private EditText mFileNameEditText;

	public CreateFolderDialog(final Context context, File currentFolder, OnCreatedListener createdListener)
	{
		super(context);

		final View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_createfolder, null);

		mFileNameEditText = view.findViewById(R.id.layout_createfolder_edittext);

		setTitle(R.string.text_createFolder);
		setNegativeButton(R.string.butn_close, null);
		setPositiveButton(R.string.butn_create, null);
		setView(view);

		mOnCreatedListener = createdListener;
		mCurrentFolder = currentFolder;
	}

	@Override
	public AlertDialog show()
	{
		final AlertDialog dialog = super.show();

		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String fileName = mFileNameEditText.getText().toString();
				File file = new File(mCurrentFolder.getAbsoluteFile() + File.separator + fileName);

				if (file.exists())
					Toast.makeText(getContext(), R.string.mesg_fileAlreadyExists, Toast.LENGTH_SHORT).show();
				else if (file.mkdir()) {
					mOnCreatedListener.onCreated();
					dialog.dismiss();
				} else
					Toast.makeText(getContext(), R.string.mesg_folderCreateError, Toast.LENGTH_SHORT).show();
			}
		});

		return dialog;
	}

	public static interface OnCreatedListener
	{
		void onCreated();
	}
}
