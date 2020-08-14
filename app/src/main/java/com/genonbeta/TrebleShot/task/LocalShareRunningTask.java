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

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.AddDevicesToTransferActivity;
import com.genonbeta.TrebleShot.activity.TransferDetailActivity;
import com.genonbeta.TrebleShot.activity.WebShareActivity;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.io.Containable;
import com.genonbeta.TrebleShot.object.Container;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.object.Transfer;
import com.genonbeta.TrebleShot.object.TransferItem;
import com.genonbeta.TrebleShot.service.backgroundservice.BackgroundTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.Transfers;
import com.genonbeta.android.framework.io.DocumentFile;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class LocalShareRunningTask extends BackgroundTask
{
    public static final String TAG = LocalShareRunningTask.class.getSimpleName();

    public List<? extends Shareable> mList;
    private final boolean mFlagAddNewDevice;
    private final boolean mFlagWebShare;

    public LocalShareRunningTask(List<? extends Shareable> list, boolean addNewDevice, boolean webShare)
    {
        mList = list;
        mFlagAddNewDevice = addNewDevice;
        mFlagWebShare = webShare;
    }

    @Override
    protected void onRun() throws TaskStoppedException
    {
        if (mList.size() <= 0)
            return;

        final Kuick kuick = AppUtils.getKuick(getService());
        final SQLiteDatabase db = kuick.getWritableDatabase();
        final Transfer transfer = new Transfer(AppUtils.getUniqueNumber());
        final List<TransferItem> list = new ArrayList<>();

        for (Shareable shareable : mList) {
            Containable containable = shareable instanceof Container ? ((Container) shareable).expand() : null;

            throwIfStopped();

            if (shareable instanceof FileListAdapter.FileHolder) {
                DocumentFile file = ((FileListAdapter.FileHolder) shareable).file;
                Transfers.createFolderStructure(list, transfer.id, file, shareable.fileName, this,
                        null);
            } else
                list.add(TransferItem.from(shareable, transfer.id, containable == null ? null : shareable.friendlyName));

            if (containable != null)
                for (Uri uri : containable.children)
                    try {
                        list.add(TransferItem.from(FileUtils.fromUri(getService(), uri), transfer.id,
                                shareable.friendlyName));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
        }

        if (list.size() <= 0) {
            // TODO: 9.03.2020 Make this more sophisticated. User may not be able to understand that there is no content.
            Log.d(TAG, "onRun: No content is located with uri data");
            return;
        }

        addCloser((userAction -> kuick.remove(db, transfer, null, null)));
        kuick.insert(db, list, transfer, null);

        if (mFlagWebShare) {
            transfer.isServedOnWeb = true;

            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getService(),
                    R.string.text_transferSharedOnBrowser, Toast.LENGTH_SHORT).show());
        }

        kuick.insert(db, transfer, null, null);
        TransferDetailActivity.startInstance(getService(), transfer);

        if (mFlagWebShare)
            getService().startActivity(new Intent(getService(), WebShareActivity.class).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK));
        else
            AddDevicesToTransferActivity.startInstance(getService(), transfer, mFlagAddNewDevice);

        kuick.broadcast();
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

}
