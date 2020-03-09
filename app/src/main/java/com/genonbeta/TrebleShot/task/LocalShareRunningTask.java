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
import com.genonbeta.TrebleShot.activity.ViewTransferActivity;
import com.genonbeta.TrebleShot.activity.WebShareActivity;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.io.Containable;
import com.genonbeta.TrebleShot.object.Container;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.android.framework.io.DocumentFile;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class LocalShareRunningTask extends WorkerService.RunningTask
{
    public static final String TAG = LocalShareRunningTask.class.getSimpleName();

    public List<? extends Shareable> mList;
    private boolean mFlagAddNewDevice;
    private boolean mFlagWebShare;

    public LocalShareRunningTask(List<? extends Shareable> list, boolean addNewDevice, boolean webShare)
    {
        mList = list;
        mFlagAddNewDevice = addNewDevice;
        mFlagWebShare = webShare;
    }

    @Override
    protected void onRun() throws InterruptedException
    {
        if (mList.size() <= 0)
            return;

        final Kuick kuick = AppUtils.getKuick(getService());
        final SQLiteDatabase db = kuick.getWritableDatabase();
        final TransferGroup group = new TransferGroup(AppUtils.getUniqueNumber());
        final List<TransferObject> list = new ArrayList<>();

        for (Shareable shareable : mList) {
            Containable containable = shareable instanceof Container ? ((Container) shareable).expand() : null;

            if (getInterrupter().interrupted())
                throw new InterruptedException();

            if (shareable instanceof FileListAdapter.FileHolder) {
                DocumentFile file = ((FileListAdapter.FileHolder) shareable).file;
                TransferUtils.createFolderStructure(list, group.id, file, shareable.fileName, getInterrupter(),
                        null);
            } else
                list.add(TransferObject.from(shareable, group.id, containable == null ? null : shareable.friendlyName));

            if (containable != null)
                for (Uri uri : containable.children)
                    try {
                        list.add(TransferObject.from(FileUtils.fromUri(getService(), uri), group.id,
                                shareable.friendlyName));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
        }

        if (list.size() <= 0) {
            // TODO: 9.03.2020 Make this more sophisticaed. User may not be able to understand that there is no content.
            Log.d(TAG, "onRun: No content is located with uri data");
            return;
        }

        getInterrupter().addCloser((userAction -> kuick.remove(db, group, null, null)));
        kuick.insert(db, list, group, null);

        if (mFlagWebShare) {
            group.isServedOnWeb = true;

            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getService(),
                    R.string.text_transferSharedOnBrowser, Toast.LENGTH_SHORT).show());
        }

        kuick.insert(db, group, null, null);
        ViewTransferActivity.startInstance(getService(), group.id);

        if (mFlagWebShare)
            getService().startActivity(new Intent(getService(), WebShareActivity.class).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK));
        else
            AddDevicesToTransferActivity.startInstance(getService(), group.id, mFlagAddNewDevice);

        kuick.broadcast();
    }

}
