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
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.FileRenameDialog;
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException;
import com.genonbeta.TrebleShot.util.FileUtils;

import java.util.List;

public class RenameMultipleFilesTask extends AsyncTask
{
    private final List<FileListAdapter.FileHolder> mList;
    private final String mNewName;

    public RenameMultipleFilesTask(List<FileListAdapter.FileHolder> fileList, String renameTo)
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

            // TODO: 1.04.2020 Use listener
            renameFile(kuick(), fileHolder, String.format("%s%s", String.format(mNewName, i), ext), null);
        }


        //if (renameListener != null)
        //    renameListener.onFileRenameCompleted(getService());
    }

    @Override
    public String getName(Context context)
    {
        return getContext().getString(R.string.text_renameMultipleItems);
    }

    public boolean renameFile(Kuick kuick, FileListAdapter.FileHolder holder, String renameTo,
                              FileRenameDialog.OnFileRenameListener renameListener)
    {
        try {
            if (FileListAdapter.FileHolder.Type.Bookmarked.equals(holder.getType())
                    || FileListAdapter.FileHolder.Type.Mounted.equals(holder.getType())) {
                holder.friendlyName = renameTo;

                kuick.publish(holder);
                kuick.broadcast();
            } else if (holder.file.canWrite() && holder.file.renameTo(renameTo)) {
                if (renameListener != null)
                    renameListener.onFileRename(holder.file, renameTo);

                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
