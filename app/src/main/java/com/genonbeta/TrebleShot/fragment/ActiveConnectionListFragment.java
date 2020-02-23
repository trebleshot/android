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
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ActiveConnectionListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.dialog.WebShareDetailsDialog;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.ui.callback.IconProvider;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.NetworkUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

import static com.genonbeta.TrebleShot.fragment.HotspotManagerFragment.WIFI_AP_STATE_CHANGED;

/**
 * created by: veli
 * date: 4/7/19 10:59 PM
 */
public class ActiveConnectionListFragment extends EditableListFragment<
        ActiveConnectionListAdapter.EditableNetworkInterface, RecyclerViewAdapter.ViewHolder,
        ActiveConnectionListAdapter> implements IconProvider
{
    private IntentFilter mFilter = new IntentFilter();
    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (CommunicationService.ACTION_HOTSPOT_STATUS.equals(intent.getAction())
                    || WIFI_AP_STATE_CHANGED.equals(intent.getAction())
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
        setUseDefaultPaddingDecoration(true);
        setUseDefaultPaddingDecorationSpaceForEdges(true);
        setDefaultPaddingDecorationSize(getResources().getDimension(R.dimen.padding_list_content_parent_layout));

        mFilter.addAction(CommunicationService.ACTION_HOTSPOT_STATUS);
        mFilter.addAction(WIFI_AP_STATE_CHANGED);
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
    }

    @Override
    protected RecyclerView onListView(View mainContainer, ViewGroup listViewContainer)
    {
        CoordinatorLayout view = (CoordinatorLayout) getLayoutInflater().inflate(R.layout.layout_active_connection,
                null, false);
        final CardView webShareInfo = view.findViewById(R.id.card_web_share_info);
        Button webShareInfoHideButton = view.findViewById(R.id.card_web_share_info_hide_button);
        final String helpWebShareInfo = "help_webShareInfo";

        if (AppUtils.getDefaultPreferences(getContext()).getBoolean(helpWebShareInfo, true)) {
            webShareInfo.setVisibility(View.VISIBLE);
            webShareInfoHideButton.setOnClickListener(v -> {
                webShareInfo.setVisibility(View.GONE);
                TransitionManager.beginDelayedTransition((ViewGroup) webShareInfo.getParent());

                AppUtils.getDefaultPreferences(getContext()).edit()
                        .putBoolean(helpWebShareInfo, false)
                        .apply();
            });
        }

        listViewContainer.addView(view);

        return super.onListView(mainContainer, view.findViewById(R.id.container));
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
        final AppUtils.QuickActions<RecyclerViewAdapter.ViewHolder> quickActions = clazz -> {
            registerLayoutViewClicks(clazz);

            clazz.itemView.findViewById(R.id.visitView).setOnClickListener(v -> performLayoutClickOpen(clazz));
            clazz.itemView.findViewById(R.id.selector).setOnClickListener(v -> setItemSelected(clazz, true));
        };

        return new ActiveConnectionListAdapter(getActivity())
        {
            @NonNull
            @Override
            public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), quickActions);
            }
        };
    }

    @Override
    public boolean onDefaultClickAction(RecyclerViewAdapter.ViewHolder holder)
    {
        try {
            ActiveConnectionListAdapter.EditableNetworkInterface editableInterface = getAdapter().getItem(holder);

            new WebShareDetailsDialog(getContext(), TextUtils.makeWebShareLink(getContext(),
                    NetworkUtils.getFirstInet4Address(editableInterface).getHostAddress())).show();
        } catch (NotReadyException e) {
            return false;
        }

        return true;
    }

    @Override
    public int getIconRes()
    {
        return R.drawable.ic_web_white_24dp;
    }

    @Override
    public CharSequence getDistinctiveTitle(Context context)
    {
        return context.getString(R.string.text_webShare);
    }

    @Override
    public boolean performLayoutClickOpen(RecyclerViewAdapter.ViewHolder holder)
    {
        if (!super.performLayoutClickOpen(holder)) {
            try {
                ActiveConnectionListAdapter.EditableNetworkInterface editableInterface = getAdapter().getItem(holder);

                Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(TextUtils.makeWebShareLink(
                        getContext(), NetworkUtils.getFirstInet4Address(editableInterface).getHostAddress())));

                startActivity(intent);
            } catch (NotReadyException e) {
                return false;
            }
        }

        return true;
    }
}
