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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.Shareable;

import java.util.ArrayList;

import static com.genonbeta.TrebleShot.activity.ShareActivity.*;

public class ChooseSharingMethodDialog<T extends Shareable> extends AlertDialog.Builder
{
    private LayoutInflater mLayoutInflater;
    private final ArrayList<SharingMethod> mSharingMethodList = new ArrayList<>();

    public ChooseSharingMethodDialog(Context context, Intent p2pIntent)
    {
        super(context);

        mLayoutInflater = LayoutInflater.from(getContext());

        DialogInterface.OnClickListener clickListener = (dialog, which) -> {
            SharingMethod method = mSharingMethodList.get(which);
            method.call();
        };

        mSharingMethodList.add(new SharingMethod(R.drawable.ic_web_white_24dp, R.string.butn_webShare,
                () -> getContext().startActivity(p2pIntent.putExtra(EXTRA_FLAGS, FLAG_WEBSHARE))));

        mSharingMethodList.add(new SharingMethod(R.drawable.ic_compare_arrows_white_24dp,
                R.string.text_devicesWithAppInstalled, () -> getContext().startActivity(p2pIntent.putExtra(EXTRA_FLAGS,
                FLAG_LAUNCH_DEVICE_ADDING))));

        setTitle(R.string.text_chooseSharingMethod);
        setAdapter(new SharingMethodListAdapter(), clickListener);
        setNegativeButton(R.string.butn_cancel, null);
    }

    private class SharingMethodListAdapter extends BaseAdapter
    {
        @Override
        public int getCount()
        {
            return mSharingMethodList.size();
        }

        @Override
        public Object getItem(int position)
        {
            return mSharingMethodList.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            // Since the list will be a static one, item ids do not have importance.
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if (convertView == null)
                convertView = mLayoutInflater.inflate(R.layout.list_sharing_method, parent, false);

            SharingMethod sharingMethod = (SharingMethod) getItem(position);
            ImageView image = convertView.findViewById(R.id.image);
            TextView text1 = convertView.findViewById(R.id.text1);

            image.setImageResource(sharingMethod.mIconRes);
            text1.setText(getContext().getString(sharingMethod.mTitleRes));

            return convertView;
        }
    }

    private static class SharingMethod
    {
        @DrawableRes
        private int mIconRes;
        @StringRes
        private int mTitleRes;
        private Listener mCallable;

        private SharingMethod(@DrawableRes int iconRes, @StringRes int titleRes, Listener callable)
        {
            mIconRes = iconRes;
            mTitleRes = titleRes;
            mCallable = callable;
        }

        private void call()
        {
            mCallable.call();
        }
    }

    private interface Listener
    {
        void call();
    }
}
