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

import android.app.Activity;
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
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable;
import com.genonbeta.TrebleShot.dataobject.LoadedMember;
import com.genonbeta.TrebleShot.dataobject.TransferItem;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.DeviceLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * created by: veli
 * date: 4/4/19 10:06 AM
 */
public class ChooseMemberDialog extends AlertDialog.Builder
{
    private final List<LoadedMember> mList = new ArrayList<>();
    private final LayoutInflater mInflater;
    private final TextDrawable.IShapeBuilder mIconBuilder;

    public ChooseMemberDialog(@NonNull Activity activity, List<LoadedMember> memberList,
                              DialogInterface.OnClickListener clickListener)
    {
        super(activity);

        mList.addAll(memberList);
        mInflater = LayoutInflater.from(activity);
        mIconBuilder = AppUtils.getDefaultIconBuilder(activity);

        if (memberList.size() > 0)
            setAdapter(new ListAdapter(), clickListener);
        else
            setMessage(R.string.text_listEmpty);

        setTitle(R.string.butn_useKnownDevice);
        setNegativeButton(R.string.butn_close, null);
    }

    private class ListAdapter extends BaseAdapter
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
                convertView = mInflater.inflate(R.layout.list_transfer_member_selector, parent, false);

            LoadedMember member = (LoadedMember) getItem(position);
            ImageView image = convertView.findViewById(R.id.image);
            ImageView actionImage = convertView.findViewById(R.id.actionImage);
            TextView text = convertView.findViewById(R.id.text);

            text.setText(member.device.username);
            actionImage.setImageResource(TransferItem.Type.INCOMING.equals(member.type)
                    ? R.drawable.ic_arrow_down_white_24dp : R.drawable.ic_arrow_up_white_24dp);
            DeviceLoader.showPictureIntoView(member.device, image, mIconBuilder);

            return convertView;
        }
    }
}
