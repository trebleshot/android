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
import android.net.Uri;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.Transfer;
import com.genonbeta.TrebleShot.object.TransferItem;
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.Transfers;
import com.genonbeta.android.framework.io.DocumentFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChangeSaveDirectoryTask extends AsyncTask
{
    public static final String ACTION_SAVE_PATH_CHANGED = "org.monora.trebleshot.intent.action.SAVE_PATH_CHANGED";

    public static final String EXTRA_TRANSFER = "extraTransfer";

    private final Transfer mTransfer;
    private final Uri mNewSavePath;
    private boolean mSkipMoving = false;

    public ChangeSaveDirectoryTask(Transfer transfer, Uri newSavePath)
    {
        mTransfer = transfer;
        mNewSavePath = newSavePath;
    }

    @Override
    protected void onRun()
    {
        getApp().interruptTasksBy(FileTransferTask.identifyWith(mTransfer.id, TransferItem.Type.INCOMING), true);

        List<TransferItem> checkList = AppUtils.getKuick(getContext()).castQuery(
                Transfers.createIncomingSelection(mTransfer.id), TransferItem.class);
        Transfer pseudoGroup = new Transfer(mTransfer.id);
        progress().addToTotal(checkList.size());

        try {
            if (!mSkipMoving) {
                // Illustrate new change to build the structure accordingly
                kuick().reconstruct(pseudoGroup);
                pseudoGroup.savePath = mNewSavePath.toString();
                List<TransferItem> erredFiles = new ArrayList<>();

                for (TransferItem transferItem : checkList) {
                    throwIfStopped();

                    progress().addToCurrent(1);
                    setOngoingContent(transferItem.name);
                    publishStatus();

                    try {
                        DocumentFile file = FileUtils.getIncomingPseudoFile(getContext(), transferItem, mTransfer,
                                false);
                        DocumentFile pseudoFile = FileUtils.getIncomingPseudoFile(getContext(), transferItem,
                                pseudoGroup, true);

                        if (file != null && pseudoFile != null) {
                            if (file.canWrite())
                                FileUtils.move(getContext(), file, pseudoFile, this);
                            else
                                throw new IOException("Failed to access: " + file.getUri());
                        }
                    } catch (Exception e) {
                        erredFiles.add(transferItem);
                    }
                }

                if (erredFiles.size() > 0) {
                    StringBuilder fileNames = new StringBuilder("\n");
                    for (TransferItem item : erredFiles)
                        fileNames.append("\n")
                                .append(item.name);

                    post(TaskMessage.newInstance()
                            .setTitle(getName())
                            .setMessage(getContext().getString(R.string.mesg_errorMoveFile, fileNames.toString())));

                }
            }

            mTransfer.savePath = mNewSavePath.toString();
            kuick().publish(mTransfer);
            kuick().broadcast();

            getContext().sendBroadcast(new Intent(ACTION_SAVE_PATH_CHANGED)
                    .putExtra(EXTRA_TRANSFER, mTransfer));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName(Context context)
    {
        return context.getString(R.string.butn_changeSavePath);
    }

    public ChangeSaveDirectoryTask setSkipMoving(boolean skip)
    {
        mSkipMoving = skip;
        return this;
    }
}
