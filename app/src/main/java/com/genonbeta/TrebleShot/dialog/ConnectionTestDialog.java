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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.task.AssessNetworkTask.ConnectionResult;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.TextUtils;

public class ConnectionTestDialog extends AlertDialog.Builder
{
    private final ConnectionResult[] mResults;

    @ColorInt
    private int mActiveColor;

    @ColorInt
    private int mPassiveColor;

    public ConnectionTestDialog(Context context, Device device, ConnectionResult[] results)
    {
        super(context);

        mResults = results;
        mActiveColor = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorAccent));
        mPassiveColor = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorControlNormal));

        setTitle(context.getString(R.string.text_connectionTest, device.username));
        setNegativeButton(R.string.butn_close, null);

        if (results.length < 1)
            setMessage(R.string.text_empty);
        else
            setAdapter(new ConnectionListAdapter(), null);
    }

    private class ConnectionListAdapter extends BaseAdapter
    {
        @Override
        public int getCount()
        {
            return mResults.length;
        }

        @Override
        public Object getItem(int position)
        {
            return mResults[position];
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
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_available_interface, parent,
                        false);

            ConnectionResult result = (ConnectionResult) getItem(position);

            TextView textView1 = convertView.findViewById(R.id.pending_available_interface_text1);
            TextView textView2 = convertView.findViewById(R.id.pending_available_interface_text2);
            TextView textView3 = convertView.findViewById(R.id.pending_available_interface_text3);

            textView1.setTextColor(result.successful ? mActiveColor : mPassiveColor);
            textView1.setText(TextUtils.getAdapterName(getContext(), result.connection));
            textView2.setText(result.connection.ipAddress);

            if (result.successful)
                textView3.setText(getContext().getString(R.string.text_textMillisecond,
                        (long) (result.pingTime / 1e6)));
            else
                textView3.setText(R.string.text_error);

            return convertView;
        }
    }
}
