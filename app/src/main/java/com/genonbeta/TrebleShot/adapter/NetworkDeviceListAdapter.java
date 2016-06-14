package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.GAnimater;
import com.genonbeta.TrebleShot.helper.NetworkDevice;

import java.util.ArrayList;

public class NetworkDeviceListAdapter extends BaseAdapter
{
    private Context mContext;
    private ArrayList<NetworkDevice> mDeviceList = new ArrayList<NetworkDevice>();

    public NetworkDeviceListAdapter(Context context)
    {
        this.mContext = context;
    }

    @Override
    public void notifyDataSetChanged()
    {
        mDeviceList.clear();

        for (NetworkDevice device : ApplicationHelper.getDeviceList().values())
        {
            if (device.user != null && device.model != null && device.brand != null)
                mDeviceList.add(device);
        }

        super.notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
        return mDeviceList.size();
    }

    @Override
    public Object getItem(int itemId)
    {
        return mDeviceList.get(itemId);
    }

    @Override
    public long getItemId(int p1)
    {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup)
    {
        if (view == null)
        {
            view = LayoutInflater.from(mContext).inflate(R.layout.list_network_device, viewGroup, false);
            AnimationSet set = GAnimater.getAnimation(GAnimater.APPEAR);
            view.setAnimation(set);
        }

        return getViewAt(view, position);
    }

    public View getViewAt(View view, int position)
    {
        TextView deviceText = (TextView) view.findViewById(R.id.network_device_list_device_text);
        TextView ipText = (TextView) view.findViewById(R.id.network_device_list_device_ip_text);
        TextView userText = (TextView) view.findViewById(R.id.network_device_list_device_user_text);

        NetworkDevice device = (NetworkDevice) getItem(position);

        if (!ipText.getText().equals(device.ip) && !ipText.getText().equals(""))
        {
            Animation animation = AnimationUtils.loadAnimation(this.mContext, android.R.anim.fade_in);

            view.setAnimation(animation);
            animation.start();
        }

        deviceText.setText(device.model);
        userText.setText(device.user);
        ipText.setText(device.ip);

        return view;
    }
}
