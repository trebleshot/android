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

package com.genonbeta.TrebleShot.task;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.AddDevicesToTransferActivity;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.activity.ViewTransferActivity;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class OrganizeSharingRunningTask extends WorkerService.RunningTask<ShareActivity>
{
    private List<Uri> mFileUris;
    private List<CharSequence> mFileNames;
    private Intent mOriginalIntent;

    public OrganizeSharingRunningTask(List<Uri> fileUris, List<CharSequence> fileNames, Intent originalIntent)
    {
        mFileUris = fileUris;
        mFileNames = fileNames;
        mOriginalIntent = originalIntent;
    }

    @Override
    public void onRun()
    {
        final WorkerService.RunningTask thisTask = this;

        if (getAnchorListener() != null) {
            getAnchorListener().getProgressBar().setMax(mFileUris.size());
            getAnchorListener().updateText(thisTask, getService().getString(R.string.mesg_organizingFiles));
        }

        final List<ShareActivity.SelectableStream> measuredObjects = new ArrayList<>();
        final List<TransferObject> pendingObjects = new ArrayList<>();
        final TransferGroup groupInstance = new TransferGroup(AppUtils.getUniqueNumber());

        for (int position = 0; position < mFileUris.size(); position++) {
            if (getInterrupter().interrupted())
                break;

            publishStatusText(getService().getString(R.string.text_transferStatusFiles,
                    position, mFileUris.size()));

            if (getAnchorListener() != null) {
                getAnchorListener().updateProgress(getAnchorListener().getProgressBar().getMax(),
                        getAnchorListener().getProgressBar().getProgress() + 1);
            }

            Uri fileUri = mFileUris.get(position);
            String fileName = mFileNames != null ? String.valueOf(mFileNames.get(position)) : null;

            try {
                ShareActivity.SelectableStream selectableStream =
                        new ShareActivity.SelectableStream(getService(), fileUri, null);

                if (selectableStream.getDocumentFile().isDirectory())
                    ShareActivity.createFolderStructure(selectableStream.getDocumentFile(),
                            selectableStream.getDocumentFile().getName(), measuredObjects,
                            this);
                else {
                    if (fileName != null)
                        selectableStream.setFriendlyName(fileName);

                    measuredObjects.add(selectableStream);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        for (ShareActivity.SelectableStream selectableStream : measuredObjects) {
            if (getInterrupter().interrupted())
                break;

            publishStatusText(selectableStream.getSelectableTitle());

            long requestId = AppUtils.getUniqueNumber();

            TransferObject transferObject = new TransferObject(requestId,
                    groupInstance.id,
                    selectableStream.getSelectableTitle(),
                    selectableStream.getDocumentFile().getUri().toString(),
                    selectableStream.getDocumentFile().getType(),
                    selectableStream.getDocumentFile().length(), TransferObject.Type.OUTGOING);

            if (selectableStream.getDirectory() != null)
                transferObject.directory = selectableStream.getDirectory();

            pendingObjects.add(transferObject);
        }

        if (getAnchorListener() != null)
            getAnchorListener().updateText(thisTask, getService().getString(R.string.mesg_completing));

        AppUtils.getDatabase(getService()).insert(pendingObjects, new SQLiteDatabase.ProgressUpdater()
        {
            @Override
            public void onProgressChange(int total, int current)
            {
                if (getAnchorListener() != null)
                    getAnchorListener().updateProgress(total, current);
            }

            @Override
            public boolean onProgressState()
            {
                return !getInterrupter().interrupted();
            }
        });

        if (getInterrupter().interrupted()) {
            AppUtils.getDatabase(getService()).remove(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                    .setWhere(String.format("%s = ?", AccessDatabase.FIELD_TRANSFER_GROUPID),
                            String.valueOf(groupInstance.id)));
        } else {
            int flags = mOriginalIntent.getIntExtra(ShareActivity.EXTRA_FLAGS, 0);
            boolean flagAddNewDevice = (flags & ShareActivity.FLAG_LAUNCH_DEVICE_ADDING) != 0;
            boolean flagWebShare = (flags & ShareActivity.FLAG_WEBSHARE) != 0;

            if (flagWebShare) {
                groupInstance.isServedOnWeb = true;

                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getService(),
                        R.string.text_transferSharedOnBrowser, Toast.LENGTH_SHORT).show());
            }

            if (!flagAddNewDevice)
                AddDevicesToTransferActivity.startInstance(getService(), groupInstance.id);

            AppUtils.getDatabase(getService()).insert(groupInstance);
            ViewTransferActivity.startInstance(getService(), groupInstance.id);
        }

        AppUtils.getDatabase(getService()).broadcast();

        if (getAnchorListener() != null)
            getAnchorListener().finish();
    }
}
