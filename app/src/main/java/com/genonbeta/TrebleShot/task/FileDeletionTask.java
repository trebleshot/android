/*
 * Copyright (C) 2020 Veli Tasalı
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

package com.genonbeta.TrebleShot.task;

import android.content.Context;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException;
import com.genonbeta.android.framework.io.DocumentFile;

import java.util.ArrayList;
import java.util.List;

public class FileDeletionTask extends AsyncTask
{
    private final List<? extends FileListAdapter.FileHolder> mList;

    public FileDeletionTask(List<? extends FileListAdapter.FileHolder> list)
    {
        mList = new ArrayList<>(list);
    }

    @Override
    protected void onRun() throws TaskStoppedException
    {
        List<DocumentFile> successfulList = new ArrayList<>();
        progress().addToTotal(mList.size());

        for (FileListAdapter.FileHolder holder : mList) {
            throwIfStopped();

            if (holder.file != null) {
                if (holder.file.isFile())
                    deleteFile(holder.file, successfulList);
                else
                    deleteDirectory(holder.file, successfulList);
            }
        }

        RenameMultipleFilesTask.notifyFileChanges(getContext(), successfulList);
    }

    public void deleteDirectory(DocumentFile folder, List<DocumentFile> successfulList) throws TaskStoppedException
    {
        DocumentFile[] files = folder.listFiles();

        if (files != null) {
            progress().addToTotal(files.length);

            for (DocumentFile file : files) {
                throwIfStopped();
                setOngoingContent(file.getName());

                if (file.isFile())
                    deleteFile(file, successfulList);
                else if (file.isDirectory())
                    deleteDirectory(file, successfulList);
            }

            deleteFile(folder, successfulList);
        }
    }

    public void deleteFile(DocumentFile file, List<DocumentFile> successfulList)
    {
        progress().addToCurrent(1);
        if (file.delete())
            successfulList.add(file);
    }

    @Override
    public String getName(Context context)
    {
        return context.getString(R.string.text_deletingFilesOngoing);
    }
}
