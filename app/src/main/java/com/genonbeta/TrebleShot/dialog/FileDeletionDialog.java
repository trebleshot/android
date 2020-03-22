/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.service.backgroundservice.BackgroundTask;
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
    public FileDeletionDialog(final Context context, final List<FileListAdapter.FileHolder> items,
                              final Listener listener)
    {
        super(context);

        final List<Uri> copiedItems = new ArrayList<>();

        for (FileListAdapter.FileHolder item : items)
            if (item.file != null)
                copiedItems.add(item.file.getUri());

        setTitle(R.string.text_deleteConfirm);
        setMessage(getContext().getResources().getQuantityString(R.plurals.ques_deleteFile, copiedItems.size(), copiedItems.size()));

        setNegativeButton(R.string.butn_cancel, null);
        setPositiveButton(R.string.butn_delete, (dialog, p2) -> {
            // FIXME: 21.03.2020
            /*
            new BackgroundTask()
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
                    if (isInterrupted())
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

             */
        }
        );
    }

    public interface Listener
    {
        void onFileDeletion(BackgroundTask runningTask, Context context, DocumentFile file);

        void onCompleted(BackgroundTask runningTask, Context context, int fileSize);
    }
}
