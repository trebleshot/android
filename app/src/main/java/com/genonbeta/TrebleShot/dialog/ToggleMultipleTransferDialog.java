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

package com.genonbeta.TrebleShot.dialog;

import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ViewTransferActivity;
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable;
import com.genonbeta.TrebleShot.object.ShowingAssignee;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.genonbeta.TrebleShot.util.TransferUtils;

import java.util.ArrayList;
import java.util.List;

public class ToggleMultipleTransferDialog extends AlertDialog.Builder
{
    private List<ShowingAssignee> mList = new ArrayList<>();
    private List<String> mActiveList = new ArrayList<>();
    private LayoutInflater mInflater;
    private TextDrawable.IShapeBuilder mIconBuilder;

    public ToggleMultipleTransferDialog(@NonNull final ViewTransferActivity activity,
                                        final TransferGroup group,
                                        final List<ShowingAssignee> deviceList,
                                        final List<String> activeList,
                                        TransferGroup.Index transferIndex)
    {
        super(activity);

        mInflater = LayoutInflater.from(activity);
        mIconBuilder = AppUtils.getDefaultIconBuilder(activity);
        mList.addAll(deviceList);
        mActiveList.addAll(activeList);

        setAdapter(new ActiveListAdapter(), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                ShowingAssignee assignee = mList.get(which);

                if (mActiveList.contains(assignee.deviceId))
                    TransferUtils.pauseTransfer(activity, assignee);
                else
                    TransferUtils.startTransfer(activity, assignee);
            }
        });

        setNegativeButton(R.string.butn_close, null);

        if (transferIndex.outgoingCount > 0)
            setNeutralButton(R.string.butn_addDevices, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    activity.startDeviceAddingActivity();
                }
            });

        ShowingAssignee senderAssignee = null;

        for (ShowingAssignee assignee : deviceList)
            if (TransferObject.Type.INCOMING.equals(assignee.type)) {
                senderAssignee = assignee;
                break;
            }

        if (transferIndex.incomingCount > 0 && senderAssignee != null) {
            mList.remove(senderAssignee);

            final ShowingAssignee finalSenderAssignee = senderAssignee;
            setPositiveButton(R.string.butn_receive, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    TransferUtils.startTransferWithTest(activity, group, finalSenderAssignee);
                }
            });
        }
    }

    private class ActiveListAdapter extends BaseAdapter
    {
        @Override
        public int getCount()
        {
            return mList.size();
        }

        @Override
        public Object getItem(int position)
        {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if (convertView == null)
                convertView = mInflater.inflate(R.layout.list_toggle_transfer, parent,
                        false);

            ShowingAssignee assignee = (ShowingAssignee) getItem(position);
            ImageView image = convertView.findViewById(R.id.image);
            TextView text = convertView.findViewById(R.id.text);
            ImageView actionImage = convertView.findViewById(R.id.actionImage);

            text.setText(assignee.device.nickname);
            actionImage.setImageResource(mActiveList.contains(assignee.deviceId)
                    ? R.drawable.ic_pause_white_24dp
                    : R.drawable.ic_arrow_up_white_24dp);
            NetworkDeviceLoader.showPictureIntoView(assignee.device, image, mIconBuilder);

            return convertView;
        }
    }
}
