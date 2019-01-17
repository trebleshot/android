package com.genonbeta.TrebleShot.dialog;

import android.content.Context;
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
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;

import java.util.ArrayList;
import java.util.List;

public class PauseMultipleTransferDialog extends AlertDialog.Builder
{
    private List<NetworkDevice> mActiveList = new ArrayList<>();
    private LayoutInflater mInflater;
    private TextDrawable.IShapeBuilder mIconBuilder;

    public PauseMultipleTransferDialog(@NonNull final Context context, final TransferGroup group, List<String> activeList)
    {
        super(context);

        mInflater = LayoutInflater.from(context);
        mIconBuilder = AppUtils.getDefaultIconBuilder(context);

        for (String deviceId : activeList) {
            NetworkDevice device = new NetworkDevice(deviceId);

            try {
                AppUtils.getDatabase(context).reconstruct(device);
            } catch (Exception e) {
                e.printStackTrace();
            }

            mActiveList.add(device);
        }

        setTitle(R.string.butn_pause);
        setAdapter(new ActiveListAdapter(), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                TransferUtils.pauseTransfer(context, group.groupId, mActiveList.get(which).deviceId);
            }
        });

        setNegativeButton(R.string.butn_close, null);
    }

    private class ActiveListAdapter extends BaseAdapter
    {
        @Override
        public int getCount()
        {
            return mActiveList.size();
        }

        @Override
        public Object getItem(int position)
        {
            return mActiveList.get(position);
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
                convertView = mInflater.inflate(R.layout.list_pause_transfer, parent, false);

            NetworkDevice device = (NetworkDevice) getItem(position);
            ImageView image = convertView.findViewById(R.id.image);
            TextView text = convertView.findViewById(R.id.text);

            image.setImageDrawable(mIconBuilder.buildRound(device.nickname));
            text.setText(device.nickname);

            return convertView;
        }
    }
}
