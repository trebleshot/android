package com.genonbeta.TrebleShot.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.android.framework.io.DocumentFile;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by: veli
 * Date: 5/21/17 2:21 AM
 */

public class FileDeletionDialog extends AlertDialog.Builder
{
    public FileDeletionDialog(final Context context, final List<FileListAdapter.GenericFileHolder> items, final Listener listener)
    {
        super(context);

        final List<Uri> copiedItems = new ArrayList<>();

        for (FileListAdapter.GenericFileHolder item : items)
            if (item.file != null)
                copiedItems.add(item.file.getUri());

        setTitle(R.string.text_deleteConfirm);
        setMessage(getContext().getResources().getQuantityString(R.plurals.ques_deleteFile, copiedItems.size(), copiedItems.size()));

        setNegativeButton(R.string.butn_cancel, null);
        setPositiveButton(R.string.butn_delete, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int p2)
                    {
                        new WorkerService.RunningTask()
                        {
                            int mTotalDeletion = 0;

                            @Override
                            public void onRun()
                            {
                                for (Uri currentUri : copiedItems) {
                                    try {
                                        DocumentFile file = FileUtils.fromUri(getService(), currentUri);

                                        delete(file);
                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                }

                                if (listener != null)
                                    listener.onCompleted(this, getService(), mTotalDeletion);
                            }

                            private void delete(DocumentFile file)
                            {
                                if (getInterrupter().interrupted())
                                    return;

                                boolean isDirectory = file.isDirectory();
                                boolean isFile = file.isFile();

                                if (isDirectory)
                                    deleteDirectory(file);

                                if (file.delete()) {
                                    if (isFile)
                                        mTotalDeletion++;

                                    listener.onFileDeletion(this, getContext(), file);
                                    publishStatusText(file.getName());
                                }
                            }

                            private void deleteDirectory(DocumentFile folder)
                            {
                                DocumentFile[] files = folder.listFiles();

                                if (files != null)
                                    for (DocumentFile anotherFile : files)
                                        delete(anotherFile);
                            }
                        }.setTitle(getContext().getString(R.string.text_deletingFilesOngoing))
                                .setIconRes(R.drawable.ic_folder_white_24dp_static)
                                .run(context);
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
