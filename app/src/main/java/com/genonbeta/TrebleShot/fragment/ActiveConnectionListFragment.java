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
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.TextEditorActivity;
import com.genonbeta.TrebleShot.adapter.ActiveConnectionListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.dialog.WebShareDetailsDialog;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.receiver.NetworkStatusReceiver;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

/**
 * created by: veli
 * date: 4/7/19 10:59 PM
 */
public class ActiveConnectionListFragment
        extends EditableListFragment<ActiveConnectionListAdapter.AddressedEditableInterface,
        EditableListAdapter.EditableViewHolder, ActiveConnectionListAdapter>
        implements TitleSupport
{
    private FloatingActionButton mFAB;
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
            else if (CommunicationService.ACTION_WEBSHARE_STATUS.equals(intent.getAction()))
                updateWebShareStatus(intent.getBooleanExtra(CommunicationService.EXTRA_STATUS_STARTED,
                        false));
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

        mFilter.addAction(CommunicationService.ACTION_WEBSHARE_STATUS);
        mFilter.addAction(CommunicationService.ACTION_HOTSPOT_STATUS);
        mFilter.addAction(NetworkStatusReceiver.WIFI_AP_STATE_CHANGED);
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
    }

    @Override
    protected RecyclerView onListView(View mainContainer, ViewGroup listViewContainer)
    {
        CoordinatorLayout view = (CoordinatorLayout) getLayoutInflater().inflate(R.layout.layout_active_connection, null, false);
        final CardView webShareInfo = view.findViewById(R.id.card_web_share_info);
        Button webShareInfoHideButton = view.findViewById(R.id.card_web_share_info_hide_button);
        mFAB = view.findViewById(R.id.content_fab);
        final String helpWebShareInfo = "help_webShareInfo";

        if (AppUtils.getDefaultPreferences(getContext()).getBoolean(helpWebShareInfo, true)) {
            webShareInfo.setVisibility(View.VISIBLE);
            webShareInfoHideButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    webShareInfo.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition((ViewGroup) webShareInfo.getParent());

                    AppUtils.getDefaultPreferences(getContext()).edit()
                            .putBoolean(helpWebShareInfo, false)
                            .apply();
                }
            });
        }

        listViewContainer.addView(view);
        mFAB.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AppUtils.toggleWebShare(getContext(), false);
            }
        });

        return super.onListView(mainContainer, (FrameLayout) view.findViewById(R.id.container));
    }

    @Override
    public void onResume()
    {
        super.onResume();
        getActivity().registerReceiver(mReceiver, mFilter);
        requestWebShareStatus();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
        mFAB.setAnimation(null);
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
    public CharSequence getTitle(Context context)
    {
        return context.getString(R.string.text_connect);
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

    public void requestWebShareStatus()
    {
        AppUtils.startForegroundService(getContext(), new Intent(getContext(), CommunicationService.class)
                .setAction(CommunicationService.ACTION_REQUEST_WEBSHARE_STATUS));
    }

    public void updateWebShareStatus(boolean running)
    {
        mFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(),
                running ? R.color.colorError : R.color.colorSecondary)));
        mFAB.setImageResource(running ? R.drawable.ic_pause_white_24dp
                : R.drawable.ic_play_arrow_white_24dp);

        if (mFAB.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
            ((CoordinatorLayout.LayoutParams) mFAB.getLayoutParams()).gravity = running
                    ? Gravity.BOTTOM | Gravity.END
                    : Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

            mFAB.setLayoutParams(mFAB.getLayoutParams());

            if (mFAB.getParent() != null && mFAB.getParent() instanceof ViewGroup)
                TransitionManager.beginDelayedTransition((ViewGroup) mFAB.getParent());
        }

        if (running) {
            mFAB.setAnimation(null);
        } else {
            mFAB.setVisibility(View.VISIBLE);
            mFAB.setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.pulse));
        }
    }
}
