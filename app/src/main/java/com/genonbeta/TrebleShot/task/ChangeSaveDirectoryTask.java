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

import android.net.Uri;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.backgroundservice.BackgroundTask;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.Transfers;
import com.genonbeta.android.framework.io.DocumentFile;

import java.io.IOException;
import java.util.List;

public class ChangeSaveDirectoryTask extends BackgroundTask
{
    private TransferGroup mGroup;
    private Uri mNewSavePath;
    private boolean mSkipMoving = false;

    public ChangeSaveDirectoryTask(TransferGroup group, Uri newSavePath)
    {
        mGroup = group;
        mNewSavePath = newSavePath;
    }

    @Override
    protected void onRun()
    {
        // TODO: 31.03.2020 Should we stop the tasks or not allow this operation while there are ongoing tasks?
        for (BackgroundTask task : getService().findTasksBy(FileTransferTask.identifyWith(mGroup.id,
                TransferObject.Type.INCOMING)))
            task.interrupt(true);

        List<TransferObject> checkList = AppUtils.getKuick(getService()).castQuery(
                Transfers.createIncomingSelection(mGroup.id), TransferObject.class);
        TransferGroup pseudoGroup = new TransferGroup(mGroup.id);

        try {
            if (!mSkipMoving) {
                // Illustrate new change to build the structure accordingly
                kuick().reconstruct(pseudoGroup);
                pseudoGroup.savePath = mNewSavePath.toString();

                for (TransferObject transferObject : checkList) {
                    throwIfStopped();

                    setOngoingContent(transferObject.name);
                    publishStatus();

                    try {
                        DocumentFile file = FileUtils.getIncomingPseudoFile(getService(), transferObject, mGroup,
                                false);
                        DocumentFile pseudoFile = FileUtils.getIncomingPseudoFile(getService(), transferObject,
                                pseudoGroup, true);

                        if (file != null && pseudoFile != null) {
                            if (file.canWrite())
                                FileUtils.move(getService(), file, pseudoFile, this);
                            else
                                throw new IOException("Failed to access: " + file.getUri());
                        }
                    } catch (Exception e) {
                        // TODO: 31.03.2020 Show the errors to the user
                    }
                }
            }

            mGroup.savePath = mNewSavePath.toString();
            kuick().publish(mGroup);
            kuick().broadcast();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public String getTitle()
    {
        return null;
    }

    public ChangeSaveDirectoryTask setSkipMoving(boolean skip)
    {
        mSkipMoving = skip;
        return this;
    }
}
