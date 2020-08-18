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
import android.database.sqlite.SQLiteDatabase;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.Transfer;
import com.genonbeta.TrebleShot.object.TransferItem;
import com.genonbeta.TrebleShot.object.TransferMember;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class IndexTransferTask extends AsyncTask
{
    private final long mTransferId;
    private final boolean mNoPrompt;
    private final Device mDevice;
    private final String mJsonIndex;

    public IndexTransferTask(final long transferId, final String jsonIndex, final Device device, final boolean noPrompt)
    {
        mTransferId = transferId;
        mJsonIndex = jsonIndex;
        mDevice = device;
        mNoPrompt = noPrompt;
    }

    @Override
    protected void onRun() throws TaskStoppedException
    {
        final SQLiteDatabase db = kuick().getWritableDatabase();
        final JSONArray jsonArray;
        Transfer transfer = new Transfer(mTransferId);
        TransferMember member = new TransferMember(transfer, mDevice, TransferItem.Type.INCOMING);

        try {
            jsonArray = new JSONArray(mJsonIndex);
        } catch (Exception e) {
            return;
        }

        progress().addToTotal(jsonArray.length());

        try {
            kuick().reconstruct(transfer);
            return;
        } catch (Exception ignored) {
        }

        kuick().publish(transfer);
        kuick().publish(member);

        long uniqueId = System.currentTimeMillis(); // The uniqueIds
        List<TransferItem> itemList = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            throwIfStopped();
            progress().addToCurrent(1);

            try {
                JSONObject index = jsonArray.getJSONObject(i);
                TransferItem transferItem = new TransferItem(index.getLong(Keyword.TRANSFER_REQUEST_ID), mTransferId,
                        index.getString(Keyword.INDEX_FILE_NAME), "." + (uniqueId++) + "." + AppConfig.EXT_FILE_PART,
                        index.getString(Keyword.INDEX_FILE_MIME), index.getLong(Keyword.INDEX_FILE_SIZE),
                        TransferItem.Type.INCOMING);

                setOngoingContent(transferItem.name);

                if (index.has(Keyword.INDEX_DIRECTORY))
                    transferItem.directory = index.getString(Keyword.INDEX_DIRECTORY);

                itemList.add(transferItem);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (itemList.size() > 0) {
            kuick().insert(db, itemList, transfer, progressListener());
            getContext().sendBroadcast(new Intent(BackgroundService.ACTION_INCOMING_TRANSFER_READY)
                    .putExtra(BackgroundService.EXTRA_TRANSFER, transfer)
                    .putExtra(BackgroundService.EXTRA_DEVICE, mDevice));

            if (mNoPrompt)
                try {
                    getApp().run(FileTransferTask.createFrom(kuick(), transfer, mDevice, TransferItem.Type.INCOMING));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            else
                getApp().notifyFileRequest(mDevice, transfer, itemList);
        }

        kuick().broadcast();
    }

    @Override
    public String getName(Context context)
    {
        return context.getString(R.string.text_preparingFiles);
    }
}
