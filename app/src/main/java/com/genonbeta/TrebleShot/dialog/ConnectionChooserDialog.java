package com.genonbeta.TrebleShot.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ManageDevicesActivity;
import com.genonbeta.TrebleShot.callback.OnDeviceSelectedListener;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.util.AddressedInterface;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.NetworkUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.android.database.SQLQuery;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by: veli
 * Date: 5/19/17 12:18 AM
 */

public class ConnectionChooserDialog extends AlertDialog.Builder
{
    final private List<NetworkDevice.Connection> mConnections = new ArrayList<>();
    final private List<AddressedInterface> mNetworkInterfaces = new ArrayList<>();

    private NetworkDevice mNetworkDevice;

    @ColorInt
    private int mActiveColor;

    @ColorInt
    private int mPassiveColor;

    public ConnectionChooserDialog(final Activity activity, NetworkDevice networkDevice, final OnDeviceSelectedListener listener)
    {
        super(activity);

        mNetworkDevice = networkDevice;
        mActiveColor = ContextCompat.getColor(activity, AppUtils.getReference(activity, R.attr.colorAccent));
        mPassiveColor = ContextCompat.getColor(activity, AppUtils.getReference(activity, R.attr.colorControlNormal));

        ConnectionListAdapter adapter = new ConnectionListAdapter();

        if (mConnections.size() > 0)
            setAdapter(adapter, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    List<NetworkDevice.Connection> connections = getConnections();
                    listener.onDeviceSelected(connections.get(which), connections);
                }
            });
        else
            setMessage(R.string.text_noNetworkAvailable);

        setTitle(getContext().getString(R.string.text_availableNetworks, networkDevice.nickname));
        setNegativeButton(R.string.butn_cancel, null);
        setNeutralButton(R.string.text_manageDevices, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                activity.startActivity(new Intent(activity, ManageDevicesActivity.class));
            }
        });
    }

    public synchronized List<NetworkDevice.Connection> getConnections()
    {
        return new ArrayList<>(mConnections);
    }


    private class ConnectionListAdapter extends BaseAdapter
    {
        public ConnectionListAdapter()
        {
            mConnections.addAll(AppUtils.getDatabase(getContext()).castQuery(new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION)
                    .setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=?", mNetworkDevice.deviceId)
                    .setOrderBy(AccessDatabase.FIELD_DEVICECONNECTION_LASTCHECKEDDATE + " DESC"), NetworkDevice.Connection.class));

            mNetworkInterfaces.addAll(NetworkUtils.getInterfaces(true, AppConfig.DEFAULT_DISABLED_INTERFACES));
        }

        @Override
        public int getCount()
        {
            return mConnections.size();
        }

        @Override
        public Object getItem(int position)
        {
            return mConnections.get(position);
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
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_available_interface, parent, false);

            NetworkDevice.Connection address = (NetworkDevice.Connection) getItem(position);

            TextView textView1 = convertView.findViewById(R.id.pending_available_interface_text1);
            TextView textView2 = convertView.findViewById(R.id.pending_available_interface_text2);
            TextView textView3 = convertView.findViewById(R.id.pending_available_interface_text3);

            boolean accessible = false;

            for (AddressedInterface addressedInterface : mNetworkInterfaces)
                if (address.adapterName.equals(addressedInterface.getNetworkInterface().getDisplayName())) {
                    accessible = true;
                    break;
                }

            textView1.setTextColor(accessible ? mActiveColor : mPassiveColor);
            textView1.setText(TextUtils.getAdapterName(getContext(), address));
            textView2.setText(address.ipAddress);
            textView3.setText(TimeUtils.getTimeAgo(getContext(), address.lastCheckedDate));

            return convertView;
        }
    }
}
