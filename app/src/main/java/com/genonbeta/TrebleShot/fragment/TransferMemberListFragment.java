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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.TransferMemberListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.app.EditableListFragmentBase;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.object.LoadedMember;
import com.genonbeta.TrebleShot.object.Transfer;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

/**
 * created by: veli
 * date: 06.04.2018 12:58
 */
public class TransferMemberListFragment extends EditableListFragment<LoadedMember, RecyclerViewAdapter.ViewHolder,
        TransferMemberListAdapter>
{
    public static final String ARG_TRANSFER_ID = "transferId";
    public static final String ARG_USE_HORIZONTAL_VIEW = "useHorizontalView";

    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (Kuick.ACTION_DATABASE_CHANGE.equals(intent.getAction())) {
                Kuick.BroadcastData data = Kuick.toData(intent);
                if (Kuick.TABLE_TRANSFERMEMBER.equals(data.tableName))
                    refreshList();
                else if (Kuick.TABLE_TRANSFER.equals(data.tableName))
                    updateTransferGroup();
            }
        }
    };

    private Transfer mHeldGroup;

    public static <T extends Editable> void showPopupMenu(EditableListFragmentBase<T> fragment,
                                                          TransferMemberListAdapter adapter, Transfer transfer,
                                                          RecyclerViewAdapter.ViewHolder clazz, View v,
                                                          LoadedMember member)
    {
        PopupMenu popupMenu = new PopupMenu(fragment.getContext(), v);
        Menu menu = popupMenu.getMenu();

        popupMenu.getMenuInflater().inflate(R.menu.popup_fragment_transfer_member, menu);
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.popup_device_details)
                new DeviceInfoDialog(fragment.getActivity(), member.device).show();
            else if (id == R.id.popup_remove) {
                AppUtils.getKuick(fragment.getContext()).removeAsynchronous(fragment.getActivity(), member, transfer);
            } else
                return false;

            return true;
        });

        popupMenu.show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setFilteringSupported(false);
        setSortingSupported(false);
        //setUseDefaultPaddingDecoration(true);
        //setUseDefaultPaddingDecorationSpaceForEdges(true);

        if (isScreenLarge())
            setDefaultViewingGridSize(4, 6);
        else if (isScreenNormal())
            setDefaultViewingGridSize(3, 5);
        else
            setDefaultViewingGridSize(2, 4);

        //setDefaultPaddingDecorationSize(getResources().getDimension(R.dimen.padding_list_content_parent_layout));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setListAdapter(new TransferMemberListAdapter(this, getTransferGroup()));
        setEmptyListImage(R.drawable.ic_device_hub_white_24dp);
        setEmptyListText(getString(R.string.text_noDeviceForTransfer));

        updateTransferGroup();

        int paddingRecyclerView = (int) getResources()
                .getDimension(R.dimen.padding_list_content_parent_layout);

        getListView().setPadding(paddingRecyclerView, paddingRecyclerView, paddingRecyclerView, paddingRecyclerView);
        getListView().setClipToPadding(false);
    }


    @Override
    public void onResume()
    {
        super.onResume();
        requireContext().registerReceiver(mReceiver, new IntentFilter(Kuick.ACTION_DATABASE_CHANGE));
    }

    @Override
    public void onPause()
    {
        super.onPause();
        requireContext().unregisterReceiver(mReceiver);
    }

    @Override
    public boolean performDefaultLayoutClick(RecyclerViewAdapter.ViewHolder holder, LoadedMember object)
    {
        new DeviceInfoDialog(requireActivity(), object.device).show();
        return true;
    }

    @Override
    public boolean performDefaultLayoutLongClick(RecyclerViewAdapter.ViewHolder holder, LoadedMember object)
    {
        showPopupMenu(this, getAdapter(), getTransferGroup(), holder, holder.itemView, object);
        return true;
    }

    @Override
    public boolean isHorizontalOrientation()
    {
        return (getArguments() != null && getArguments().getBoolean(ARG_USE_HORIZONTAL_VIEW))
                || super.isHorizontalOrientation();
    }

    @Override
    public CharSequence getDistinctiveTitle(Context context)
    {
        return context.getString(R.string.text_deviceList);
    }

    public Transfer getTransferGroup()
    {
        if (mHeldGroup == null) {
            mHeldGroup = new Transfer(getArguments() == null ? -1 : getArguments().getLong(ARG_TRANSFER_ID,
                    -1));
            updateTransferGroup();
        }

        return mHeldGroup;
    }

    private void updateTransferGroup()
    {
        try {
            AppUtils.getKuick(getContext()).reconstruct(mHeldGroup);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
