package com.genonbeta.TrebleShot.dialog;

import android.content.Context;
import android.support.v7.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.FileUtils;

import java.util.ArrayList;

/**
 * created by: Veli
 * date: 26.02.2018 08:53
 */

public class FileRenameDialog<T extends FileListAdapter.GenericFileHolder> extends AbstractSingleTextInputDialog
{
	public static final String TAG = FileRenameDialog.class.getSimpleName();
	public static final int JOB_RENAME_FILES = 0;

	private ArrayList<T> mItemList = new ArrayList<>();

	public FileRenameDialog(Context context, ArrayList<T> itemList, final OnFileRenameListener renameListener)
	{
		super(context);

		mItemList.addAll(itemList);

		setTitle(mItemList.size() > 1
				? R.string.text_renameMultipleItems
				: R.string.text_rename);

		getEditText().setText(mItemList.size() > 1
				? "%d"
				: mItemList.get(0).fileName);

		setOnProceedClickListener(R.string.butn_rename, new OnProceedClickListener()
		{
			@Override
			public boolean onProceedClick(AlertDialog dialog)
			{
				final String renameTo = getEditText().getText().toString();

				if (getItemList().size() == 1
						&& renameFile(getItemList().get(0).file, renameTo, renameListener)) {
					if (renameListener != null)
						renameListener.onFileRenameCompleted(getContext());
					return true;
				}

				try {
					String.format(renameTo, getItemList().size());
				} catch (Exception e) {
					return false;
				}

				return WorkerService.run(getContext(), new WorkerService.RunningTask(TAG, JOB_RENAME_FILES)
				{
					@Override
					protected void onRun()
					{
						publishStatusText(getService().getString(R.string.text_renameMultipleItems));

						int fileId = 0;

						for (T fileHolder : getItemList()) {
							renameFile(fileHolder.file, String.format(renameTo, fileId) + FileUtils.getFileExtension(fileHolder.file.getName()), renameListener);
							fileId++;
						}

						if (renameListener != null)
							renameListener.onFileRenameCompleted(getService());
					}
				});
			}
		});
	}

	public ArrayList<T> getItemList()
	{
		return mItemList;
	}

	public boolean renameFile(DocumentFile file, String renameTo, OnFileRenameListener renameListener)
	{
		try {
			if (file.canWrite() && file.renameTo(renameTo)) {
				if (renameListener != null)
					renameListener.onFileRename(file, renameTo);

				return true;
			}
		} catch (Exception e) {
		}

		return false;
	}

	public interface OnFileRenameListener
	{
		void onFileRename(DocumentFile file, String displayName);

		void onFileRenameCompleted(Context context);
	}
}
