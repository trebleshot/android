package com.genonbeta.TrebleShot.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.DeviceScannerService;
import com.genonbeta.TrebleShot.util.AddressedInterface;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.TrebleShot.util.NetworkUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.util.Interrupter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

/**
 * Created by: veli
 * Date: 5/19/17 12:18 AM
 */

public class ConnectionChooserDialog extends AlertDialog.Builder
{
    final private ArrayList<NetworkDevice.Connection> mConnections = new ArrayList<>();
    final private ArrayList<AddressedInterface> mNetworkInterfaces = new ArrayList<>();

    private AlertDialog mDialog;
    private AccessDatabase mDatabase;
    private NetworkDevice mNetworkDevice;
    private ConnectionListAdapter mAdapter;
    private OnDeviceSelectedListener mDeviceSelectedListener;
    private Activity mActivity;

    @ColorInt
    private int mActiveColor;

    @ColorInt
    private int mPassiveColor;


    public ConnectionChooserDialog(Activity activity, NetworkDevice networkDevice, final OnDeviceSelectedListener listener, boolean refreshProvided)
    {
        super(activity);

        mAdapter = new ConnectionListAdapter();
        mActivity = activity;
        mDatabase = AppUtils.getDatabase(getContext());
        mNetworkDevice = networkDevice;
        mDeviceSelectedListener = listener;

        mActiveColor = ContextCompat.getColor(activity, AppUtils.getReference(activity, R.attr.colorAccent));
        mPassiveColor = ContextCompat.getColor(activity, AppUtils.getReference(activity, R.attr.colorControlNormal));

        setAdapter(mAdapter, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                ArrayList<NetworkDevice.Connection> connections = getConnections();
                mDeviceSelectedListener.onDeviceSelected(connections.get(which), connections);
            }
        });

        setTitle(getContext().getString(R.string.text_availableNetworks, networkDevice.nickname));
        setNegativeButton(R.string.butn_cancel, null);

        if (!refreshProvided)
            setPositiveButton(R.string.text_refresh, null);
    }

    public synchronized ArrayList<NetworkDevice.Connection> getConnections()
    {
        return new ArrayList<>(mConnections);
    }

    @Override
    public AlertDialog show()
    {
        mAdapter.notifyDataSetChanged();

        final ArrayList<NetworkDevice.Connection> tmpList = getConnections();

        if (tmpList.size() > 0) {
            setMessage(null);
            setNeutralButton(R.string.butn_feelLucky, null);
        } else
            setMessage(R.string.text_noNetworkAvailable);

        mDialog = super.show();

        startRefreshing();

        Button buttonPositive = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);

        if (buttonPositive != null)
            buttonPositive.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    AppUtils.toggleDeviceScanning(getContext());
                }
            });

        mDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final ProgressDialog feelLucky = new ProgressDialog(getContext());
                final Interrupter interrupter = new Interrupter();

                feelLucky.setTitle(R.string.text_feelLuckyOngoing);
                feelLucky.setMax(tmpList.size());
                feelLucky.setCancelable(false);
                feelLucky.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                feelLucky.setButton(ProgressDialog.BUTTON_NEGATIVE, getContext().getString(R.string.butn_cancel), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        interrupter.interrupt();
                    }
                });

                feelLucky.show();

                new Thread()
                {
                    @Override
                    public void run()
                    {
                        super.run();

                        Looper.prepare();

                        @SuppressLint("UseSparseArrays") final HashMap<Integer, NetworkDevice.Connection> calculatedConnections = new HashMap<>();

                        for (final NetworkDevice.Connection connection : tmpList) {
                            if (interrupter.interrupted())
                                break;

                            feelLucky.setProgress(feelLucky.getProgress() + 1);

                            if (!NetworkUtils.ping(connection.ipAddress, 500))
                                continue;

                            Integer calculatedTime = CommunicationBridge.connect(mDatabase, Integer.class, new CommunicationBridge.Client.ConnectionHandler()
                            {
                                @Override
                                public void onConnect(CommunicationBridge.Client client)
                                {
                                    int outTime = -1;

                                    try {
                                        final long startTime = System.currentTimeMillis();
                                        final CoolSocket.ActiveConnection activeConnection = client.connect(connection);
                                        final Interrupter.Closer selfCloser = new Interrupter.Closer()
                                        {
                                            @Override
                                            public void onClose(boolean userAction)
                                            {
                                                try {
                                                    activeConnection.getSocket().close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        };

                                        interrupter.addCloser(selfCloser);

                                        client.handshake(activeConnection, true);
                                        client.updateDeviceIfOkay(activeConnection, mNetworkDevice);

                                        interrupter.removeCloser(selfCloser);

                                        outTime = (int) (System.currentTimeMillis() - startTime);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        client.setReturn(outTime);
                                    }
                                }
                            });

                            if (calculatedTime != null && calculatedTime > -1)
                                calculatedConnections.put(calculatedTime, connection);
                        }

                        feelLucky.cancel();

                        if (!interrupter.interrupted())
                            if (calculatedConnections.size() < 1) {
                                AlertDialog.Builder sorryDialog = new AlertDialog.Builder(getContext());

                                sorryDialog.setTitle(R.string.text_error);
                                sorryDialog.setMessage(R.string.text_feelLuckyFailed);
                                sorryDialog.setNegativeButton(R.string.butn_close, null);

                                sorryDialog.show();
                            } else {
                                final ArrayList<Integer> comparedList = new ArrayList<>(calculatedConnections.keySet());

                                Collections.sort(comparedList, new Comparator<Integer>()
                                {
                                    @Override
                                    public int compare(Integer integer1, Integer integer2)
                                    {
                                        return integer1 < integer2 ? -1 : 1;
                                    }
                                });

                                if (mActivity != null)
                                    mActivity.runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            mDeviceSelectedListener.onDeviceSelected(calculatedConnections.get(comparedList.get(0)), tmpList);
                                        }
                                    });

                                mDialog.cancel();
                            }

                        Looper.loop();
                    }
                }.start();
            }
        });

        return mDialog;
    }

    public void startRefreshing()
    {
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if (mActivity != null && mDialog != null && mDialog.isShowing()) {
                    mActivity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            boolean previousState = mConnections.size() > 0;

                            mAdapter.notifyDataSetChanged();

                            if (previousState != (mConnections.size() > 0)) {
                                if (mDialog != null && mDialog.isShowing())
                                    mDialog.cancel();

                                show();
                            }

                            Button refreshButton = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);

                            refreshButton.setText(DeviceScannerService.getDeviceScanner().isScannerAvailable()
                                    ? R.string.butn_refresh
                                    : R.string.butn_pause);
                        }
                    });

                    startRefreshing();
                }
            }
        }, 2000);
    }

    public abstract static class OnDeviceSelectedListener
    {
        public abstract void onDeviceSelected(NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> availableInterfaces);
    }

    private class ConnectionListAdapter extends BaseAdapter
    {
        public ConnectionListAdapter()
        {
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

            textView1.setTextColor(accessible ?  mActiveColor : mPassiveColor);

            int availableName = TextUtils.getAdapterName(address);

            if (availableName == -1)
                textView1.setText(address.adapterName);
            else
                textView1.setText(availableName);

            textView2.setText(address.ipAddress);
            textView3.setText(TimeUtils.getTimeAgo(getContext(), address.lastCheckedDate));

            return convertView;
        }

        @Override
        public synchronized void notifyDataSetChanged()
        {
            notifyDataSetChanged(mConnections, mNetworkInterfaces);
        }

        public void notifyDataSetChanged(ArrayList<NetworkDevice.Connection> connections, ArrayList<AddressedInterface> addressedInterfaces)
        {
            connections.clear();
            connections.addAll(mDatabase.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION)
                    .setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=?", mNetworkDevice.deviceId)
                    .setOrderBy(AccessDatabase.FIELD_DEVICECONNECTION_LASTCHECKEDDATE + " DESC"), NetworkDevice.Connection.class));

            addressedInterfaces.clear();
            addressedInterfaces.addAll(NetworkUtils.getInterfaces(true, AppConfig.DEFAULT_DISABLED_INTERFACES));

            super.notifyDataSetChanged();
        }
    }
}
