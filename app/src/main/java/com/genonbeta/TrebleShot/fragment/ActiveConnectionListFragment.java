package com.genonbeta.TrebleShot.fragment;

import android.bluetooth.BluetoothA2dp;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ActiveConnectionListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.dialog.WebShareDetailsDialog;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.receiver.NetworkStatusReceiver;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * created by: veli
 * date: 4/7/19 10:59 PM
 */
public class ActiveConnectionListFragment extends EditableListFragment<ActiveConnectionListAdapter
        .AddressedEditableInterface, EditableListAdapter.EditableViewHolder,
        ActiveConnectionListAdapter>
{
    private IntentFilter mFilter = new IntentFilter();
    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (CommunicationService.ACTION_HOTSPOT_STATUS.equals(intent.getAction())
                    || NetworkStatusReceiver.WIFI_AP_STATE_CHANGED.equals(intent.getAction())
                    || ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())
                    || WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
                    || WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(intent.getAction())
                    || BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction()))
                refreshList();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setSortingSupported(false);
        setFilteringSupported(true);

        mFilter.addAction(CommunicationService.ACTION_HOTSPOT_STATUS);
        mFilter.addAction(NetworkStatusReceiver.WIFI_AP_STATE_CHANGED);
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        getActivity().registerReceiver(mReceiver, mFilter);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        setEmptyImage(R.drawable.ic_share_white_24dp);
        setEmptyText(getString(R.string.text_listEmptyConnection));
    }

    @Override
    public ActiveConnectionListAdapter onAdapter()
    {
        final AppUtils.QuickActions<EditableListAdapter.EditableViewHolder> quickActions = new AppUtils.QuickActions<EditableListAdapter.EditableViewHolder>()
        {
            @Override
            public void onQuickActions(final EditableListAdapter.EditableViewHolder clazz)
            {
                registerLayoutViewClicks(clazz);

                clazz.getView().findViewById(R.id.visitView).setOnClickListener(
                        new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                performLayoutClickOpen(clazz);
                            }
                        });

                clazz.getView().findViewById(R.id.selector).setOnClickListener(
                        new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                if (getSelectionConnection() != null)
                                    getSelectionConnection().setSelected(clazz.getAdapterPosition());
                            }
                        });
            }
        };

        return new ActiveConnectionListAdapter(getActivity())
        {
            @NonNull
            @Override
            public EditableListAdapter.EditableViewHolder onCreateViewHolder(
                    @NonNull ViewGroup parent, int viewType)
            {
                return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), quickActions);
            }
        };
    }

    @Override
    public boolean onDefaultClickAction(EditableListAdapter.EditableViewHolder holder)
    {
        try {
            ActiveConnectionListAdapter.AddressedEditableInterface editableInterface =
                    getAdapter().getItem(holder);

            new WebShareDetailsDialog(getContext(), TextUtils.makeWebShareLink(getContext(),
                    editableInterface.getInterface().getAssociatedAddress())).show();
        } catch (NotReadyException e) {
            return false;
        }

        return true;
    }

    @Override
    public boolean performLayoutClickOpen(EditableListAdapter.EditableViewHolder holder)
    {
        if (!super.performLayoutClickOpen(holder)) {
            try {
                ActiveConnectionListAdapter.AddressedEditableInterface editableInterface =
                        getAdapter().getItem(holder);
                Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(
                        TextUtils.makeWebShareLink(getContext(), editableInterface.getInterface()
                                .getAssociatedAddress())));

                startActivity(intent);
            } catch (NotReadyException e) {
                return false;
            }
        }

        return true;
    }
}
