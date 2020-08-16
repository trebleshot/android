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
import android.content.Intent;
import android.media.MediaScannerConnection;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.fragment.FileListFragment;
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.android.framework.io.DocumentFile;

import java.util.ArrayList;
import java.util.List;

public class RenameMultipleFilesTask extends AsyncTask
{
    private final List<? extends FileListAdapter.FileHolder> mList;
    private final String mNewName;

    public RenameMultipleFilesTask(List<? extends FileListAdapter.FileHolder> fileList, String renameTo)
    {
        mList = fileList;
        mNewName = renameTo;
    }

    @Override
    protected void onRun() throws TaskStoppedException
    {
        if (mList.size() <= 0)
            return;

        progress().addToTotal(mList.size());

        List<DocumentFile> scannerList = new ArrayList<>();

        for (int i = 0; i < mList.size(); i++) {
            throwIfStopped();

            FileListAdapter.FileHolder fileHolder = mList.get(i);
            setOngoingContent(fileHolder.friendlyName);
            progress().addToCurrent(1);
            publishStatus();

            if (fileHolder.file == null)
                continue;

            String ext = FileUtils.getFileFormat(fileHolder.file.getName());
            ext = ext != null ? String.format(".%s", ext) : "";

            renameFile(kuick(), fileHolder, String.format("%s%s", String.format(mNewName, i), ext), scannerList);
        }

        notifyFileChanges(getContext(), scannerList);
    }

    @Override
    public String getName(Context context)
    {
        return getContext().getString(R.string.text_renameMultipleItems);
    }

    public static void notifyFileChanges(Context context, List<DocumentFile> scannerList)
    {
        if (scannerList.size() < 1)
            return;

        String[] paths = new String[scannerList.size()];
        String[] mimeTypes = new String[scannerList.size()];

        for (int i = 0; i < scannerList.size(); i++) {
            DocumentFile file = scannerList.get(i);
            paths[i] = file.getOriginalUri().toString();
            mimeTypes[i] = file.getType();
        }

        MediaScannerConnection.scanFile(context, paths, mimeTypes, null);
        context.sendBroadcast(new Intent(FileListFragment.ACTION_FILE_RENAME_COMPLETED));
    }

    public static boolean renameFile(Kuick kuick, FileListAdapter.FileHolder holder, String renameTo,
                                     List<DocumentFile> scannerList)
    {
        try {
            if (FileListAdapter.FileHolder.Type.Bookmarked.equals(holder.getType())
                    || FileListAdapter.FileHolder.Type.Mounted.equals(holder.getType())) {
                holder.friendlyName = renameTo;

                kuick.publish(holder);
                kuick.broadcast();

                return true;
            } else if (holder.file != null && holder.file.canWrite() && holder.file.renameTo(renameTo)) {
                scannerList.add(holder.file);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
