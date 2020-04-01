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

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.service.backgroundservice.BackgroundTask;

public class FileDeletionTask extends BackgroundTask
{
    @Override
    protected void onRun() throws InterruptedException
    {
        int fileId = 0;

        for (T fileHolder : getItemList()) {
            publishStatusText(fileHolder.friendlyName);

            String ext = FileUtils.getFileFormat(fileHolder.file.getName());
            ext = ext != null ? String.format(".%s", ext) : "";

            renameFile(fileHolder, String.format("%s%s", String.format(renameTo, fileId), ext),
                    renameListener);
            fileId++;
        }

        if (renameListener != null)
            renameListener.onFileRenameCompleted(getService());
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public String getTitle()
    {
        return getService().getString(R.string.text_renameMultipleItems);
    }
}
