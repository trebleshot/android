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

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.AddDevicesToTransferActivity;
import com.genonbeta.TrebleShot.activity.TransferDetailActivity;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.Transfer;
import com.genonbeta.TrebleShot.object.TransferItem;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.Transfers;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.io.DocumentFile;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class OrganizeSharingTask extends AttachableAsyncTask<AttachedTaskListener>
{
    private final List<Uri> mUriList;

    public OrganizeSharingTask(List<Uri> fileUris)
    {
        mUriList = fileUris;
    }

    @Override
    public void onRun() throws TaskStoppedException
    {
        final SQLiteDatabase db = kuick().getWritableDatabase();
        final Transfer transfer = new Transfer(AppUtils.getUniqueNumber());
        final List<TransferItem> list = new ArrayList<>();

        progress().addToTotal(mUriList.size());
        publishStatus();

        for (Uri uri : mUriList) {
            throwIfStopped();

            progress().addToCurrent(1);

            try {
                DocumentFile file = FileUtils.fromUri(getContext(), uri);
                setOngoingContent(file.getName());
                publishStatus();

                if (file.isDirectory())
                    Transfers.createFolderStructure(list, transfer.id, file, file.getName(), this, progressListener());
                else
                    list.add(TransferItem.from(file, transfer.id, null));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (list.size() > 0) {
            kuick().insert(db, list, transfer, progressListener());
            kuick().insert(db, transfer, null, progressListener());
            addCloser((userAction) -> kuick().remove(db, new SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                    .setWhere(String.format("%s = ?", Kuick.FIELD_TRANSFER_TRANSFERID), String.valueOf(transfer.id))));

            TransferDetailActivity.startInstance(getContext(), transfer);
            AddDevicesToTransferActivity.startInstance(getContext(), transfer, true);
            kuick().broadcast();
        }
    }

    @Override
    public String getDescription()
    {
        return getContext().getString(R.string.mesg_organizingFiles);
    }

    @Override
    public String getTitle()
    {
        return getContext().getString(R.string.mesg_organizingFiles);
    }
}
