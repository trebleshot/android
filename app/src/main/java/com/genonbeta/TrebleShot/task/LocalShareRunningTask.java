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
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.AddDevicesToTransferActivity;
import com.genonbeta.TrebleShot.activity.ViewTransferActivity;
import com.genonbeta.TrebleShot.activity.WebShareActivity;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.object.TransferDescriptor;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;

import java.util.ArrayList;
import java.util.List;

public class LocalShareRunningTask extends WorkerService.RunningTask
{
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

        final AccessDatabase database = AppUtils.getDatabase(getService());
        final SQLiteDatabase instance = database.getWritableDatabase();
        final TransferGroup group = new TransferGroup(AppUtils.getUniqueNumber());
        final List<TransferObject> objectList = new ArrayList<>();

        getInterrupter().addCloser((userAction -> database.remove(instance, group, null)));

        for (Shareable shareable : mList) {
            if (getInterrupter().interrupted())
                throw new InterruptedException();


        }

        if (mFlagWebShare) {
            group.isServedOnWeb = true;

            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getService(),
                    R.string.text_transferSharedOnBrowser, Toast.LENGTH_SHORT).show());
        }

        AppUtils.getDatabase(getService()).insert(group);
        ViewTransferActivity.startInstance(getService(), group.id);

        if (mFlagWebShare)
            getService().startActivity(new Intent(getService(), WebShareActivity.class).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK));
        else
            AddDevicesToTransferActivity.startInstance(getService(), group.id, mFlagAddNewDevice);

        AppUtils.getDatabase(getService()).broadcast();
    }
}
