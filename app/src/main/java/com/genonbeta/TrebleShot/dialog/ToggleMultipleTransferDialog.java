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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.TransferDetailActivity;
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable;
import com.genonbeta.TrebleShot.dataobject.TransferIndex;
import com.genonbeta.TrebleShot.dataobject.LoadedMember;
import com.genonbeta.TrebleShot.dataobject.TransferItem;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.DeviceLoader;
import com.genonbeta.TrebleShot.util.Transfers;

public class ToggleMultipleTransferDialog extends AlertDialog.Builder
{
    private final TransferDetailActivity mActivity;
    private final LoadedMember[] mMembers;
    private final LayoutInflater mInflater;
    private final TextDrawable.IShapeBuilder mIconBuilder;

    public ToggleMultipleTransferDialog(@NonNull final TransferDetailActivity activity, final TransferIndex index)
    {
        super(activity);

        mActivity = activity;
        mInflater = LayoutInflater.from(activity);
        mIconBuilder = AppUtils.getDefaultIconBuilder(activity);
        mMembers = index.members;

        if (mMembers.length > 0)
            setAdapter(new ActiveListAdapter(), (dialog, which) -> startTransfer(activity, index, mMembers[which]));

        setNegativeButton(R.string.butn_close, null);

        if (index.hasOutgoing())
            setNeutralButton(R.string.butn_addDevices, (dialog, which) -> activity.startDeviceAddingActivity());

        LoadedMember senderMember = null;

        for (LoadedMember member : index.members)
            if (TransferItem.Type.INCOMING.equals(member.type)) {
                senderMember = member;
                break;
            }

        if (index.hasIncoming() && senderMember != null) {
            LoadedMember finalSenderMember = senderMember;
            setPositiveButton(R.string.butn_receive, (dialog, which) -> startTransfer(activity, index,
                    finalSenderMember));
        }
    }

    private void startTransfer(TransferDetailActivity activity, TransferIndex index, LoadedMember member)
    {
        if (mActivity.isDeviceRunning(member.deviceId))
            Transfers.pauseTransfer(activity, member);
        else
            Transfers.startTransferWithTest(activity, index.transfer, member);
    }

    private class ActiveListAdapter extends BaseAdapter
    {
        @Override
        public int getCount()
        {
            return mMembers.length;
        }

        @Override
        public Object getItem(int position)
        {
            return mMembers[position];
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
                convertView = mInflater.inflate(R.layout.list_toggle_transfer, parent, false);

            LoadedMember member = (LoadedMember) getItem(position);
            ImageView image = convertView.findViewById(R.id.image);
            TextView text = convertView.findViewById(R.id.text);
            ImageView actionImage = convertView.findViewById(R.id.actionImage);

            text.setText(member.device.username);
            actionImage.setImageResource(mActivity.isDeviceRunning(member.deviceId) ? R.drawable.ic_pause_white_24dp
                    : (TransferItem.Type.INCOMING.equals(member.type) ? R.drawable.ic_arrow_down_white_24dp
                    : R.drawable.ic_arrow_up_white_24dp));
            DeviceLoader.showPictureIntoView(member.device, image, mIconBuilder);

            return convertView;
        }
    }
}
