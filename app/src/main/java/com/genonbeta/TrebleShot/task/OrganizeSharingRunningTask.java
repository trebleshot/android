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
import android.widget.ProgressBar;
import android.widget.Toast;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.AddDevicesToTransferActivity;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.activity.ViewTransferActivity;
import com.genonbeta.TrebleShot.activity.WebShareActivity;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TransferDescriptor;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.io.DocumentFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class OrganizeSharingRunningTask extends WorkerService.AttachableRunningTask<ShareActivity>
{
    private List<Uri> mFileUris;

    public OrganizeSharingRunningTask(List<Uri> fileUris)
    {
        mFileUris = fileUris;
    }

    @Override
    public void onRun()
    {
        final WorkerService.RunningTask thisTask = this;

        if (getAnchorListener() != null) {
            getAnchorListener().getProgressBar().setMax(mFileUris.size());
            getAnchorListener().updateText(thisTask, getService().getString(R.string.mesg_organizingFiles));
        }

        final List<TransferDescriptor> measuredObjects = new ArrayList<>();
        final List<TransferObject> transferObjectList = new ArrayList<>();
        final TransferGroup group = new TransferGroup(AppUtils.getUniqueNumber());

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

            try {
                TransferDescriptor stream = new TransferDescriptor(getService(), fileUri, null);
                measure(stream, measuredObjects, stream.file.isDirectory());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        for (TransferDescriptor descriptor : measuredObjects) {
            if (getInterrupter().interrupted())
                break;

            publishStatusText(descriptor.title);

            long requestId = AppUtils.getUniqueNumber();

            TransferObject transferObject = new TransferObject(requestId, group.id, descriptor.title,
                    descriptor.file.getUri().toString(), descriptor.file.getType(), descriptor.file.length(),
                    TransferObject.Type.OUTGOING);

            if (descriptor.directory != null)
                transferObject.directory = descriptor.title;

            transferObjectList.add(transferObject);
        }

        if (getAnchorListener() != null)
            getAnchorListener().updateText(thisTask, getService().getString(R.string.mesg_completing));

        AppUtils.getDatabase(getService()).insert(transferObjectList, (total, current) -> {
            if (getAnchorListener() != null)
                getAnchorListener().updateProgress(total, current);

            return !getInterrupter().interrupted();
        });

        if (getInterrupter().interrupted()) {
            AppUtils.getDatabase(getService()).remove(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                    .setWhere(String.format("%s = ?", AccessDatabase.FIELD_TRANSFER_GROUPID),
                            String.valueOf(group.id)));
        } else {
            int flags = mOriginalIntent.getIntExtra(EXTRA_FLAGS, 0);
            boolean flagAddNewDevice = (flags & FLAG_LAUNCH_DEVICE_ADDING) != 0;
            boolean flagWebShare = (flags & FLAG_WEBSHARE) != 0;

            if (flagWebShare) {
                group.isServedOnWeb = true;

                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getService(),
                        R.string.text_transferSharedOnBrowser, Toast.LENGTH_SHORT).show());
            }

            AppUtils.getDatabase(getService()).insert(group);
            ViewTransferActivity.startInstance(getService(), group.id);

            if (flagWebShare)
                getService().startActivity(new Intent(getService(), WebShareActivity.class).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK));
            else
                AddDevicesToTransferActivity.startInstance(getService(), group.id, flagAddNewDevice);
        }

        AppUtils.getDatabase(getService()).broadcast();

        if (getAnchorListener() != null)
            getAnchorListener().finish();
    }

    public void createFolderStructure(DocumentFile file, String folderName, List<TransferDescriptor> list)
    {
        DocumentFile[] files = file.listFiles();

        if (files != null) {
            if (getAnchorListener() != null) {
                ProgressBar progressBar = getAnchorListener().getProgressBar();
                progressBar.setMax(getAnchorListener().getProgressBar().getMax() + files.length);
            }

            for (DocumentFile thisFile : files) {
                if (getAnchorListener() != null) {
                    ProgressBar progressBar = getAnchorListener().getProgressBar();
                    progressBar.setProgress(progressBar.getProgress() + 1);
                }

                if (getInterrupter().interrupted())
                    break;

                if (thisFile.isDirectory()) {
                    createFolderStructure(thisFile, (folderName != null ? folderName + File.separator
                            : null) + thisFile.getName(), list);
                    continue;
                }

                try {
                    list.add(new TransferDescriptor(thisFile, folderName));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void measure(TransferDescriptor descriptor, List<TransferDescriptor> objects, boolean isDirectory)
    {
        if (isDirectory)
            createFolderStructure(descriptor.file, descriptor.file.getName(), objects);
        else
            objects.add(descriptor);
    }
}
