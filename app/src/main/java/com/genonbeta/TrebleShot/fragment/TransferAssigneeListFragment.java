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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.TransferAssigneeListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.ShowingAssignee;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * created by: veli
 * date: 06.04.2018 12:58
 */
public class TransferAssigneeListFragment
		extends EditableListFragment<ShowingAssignee, EditableListAdapter.EditableViewHolder, TransferAssigneeListAdapter>
		implements TitleSupport
{
	public static final String ARG_GROUP_ID = "groupId";
	public static final String ARG_USE_HORIZONTAL_VIEW = "useHorizontalView";

	private BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())) {
				if (AccessDatabase.TABLE_TRANSFERASSIGNEE.equals(intent.getStringExtra(
						AccessDatabase.EXTRA_TABLE_NAME)))
					refreshList();
				else if (AccessDatabase.TABLE_TRANSFERGROUP.equals(intent.getStringExtra(
						AccessDatabase.EXTRA_TABLE_NAME)))
					updateTransferGroup();
			}
		}
	};

	private TransferGroup mHeldGroup;

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

		setEmptyImage(R.drawable.ic_device_hub_white_24dp);
		setEmptyText(getString(R.string.text_noDeviceForTransfer));
		useEmptyActionButton(getString(R.string.butn_shareOnBrowser), new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mHeldGroup.isServedOnWeb = !mHeldGroup.isServedOnWeb;
				AppUtils.getDatabase(getContext()).update(mHeldGroup);

				if (mHeldGroup.isServedOnWeb)
					AppUtils.startWebShareActivity(getActivity(), true);
			}
		});

		getEmptyActionButton().setOnLongClickListener(new View.OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				AppUtils.startWebShareActivity(getActivity(), false);
				return true;
			}
		});

		updateTransferGroup();

		int paddingRecyclerView = (int) getResources()
				.getDimension(R.dimen.padding_list_content_parent_layout);

		getListView().setPadding(paddingRecyclerView, paddingRecyclerView, paddingRecyclerView,
				paddingRecyclerView);
		getListView().setClipToPadding(false);
	}

	@Override
	public TransferAssigneeListAdapter onAdapter()
	{
		final AppUtils.QuickActions<EditableListAdapter.EditableViewHolder> actions
				= new AppUtils.QuickActions<EditableListAdapter.EditableViewHolder>()
		{
			@Override
			public void onQuickActions(final EditableListAdapter.EditableViewHolder clazz)
			{
				registerLayoutViewClicks(clazz);

				clazz.getView().findViewById(R.id.menu).setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						showPopupMenu(clazz, v);
					}
				});
			}
		};

		return new TransferAssigneeListAdapter(getContext())
		{
			@NonNull
			@Override
			public EditableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
			{
				return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), actions);
			}
		}.setGroup(getTransferGroup());
	}

	@Override
	public void onResume()
	{
		super.onResume();
		getActivity().registerReceiver(mReceiver, new IntentFilter(AccessDatabase.ACTION_DATABASE_CHANGE));
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getActivity().unregisterReceiver(mReceiver);
	}

	@Override
	public boolean onDefaultClickAction(EditableListAdapter.EditableViewHolder holder)
	{
		try {
			ShowingAssignee assignee = getAdapter().getItem(holder);

			new DeviceInfoDialog(getActivity(), AppUtils.getDatabase(getContext()), assignee.device)
					.show();
			return true;
		} catch (Exception e) {
			// do nothing
		}

		return false;
	}

	@Override
	public boolean onDefaultLongClickAction(EditableListAdapter.EditableViewHolder holder)
	{
		showPopupMenu(holder, holder.getView());
		return true;
	}

	@Override
	public boolean isHorizontalOrientation()
	{
		return (getArguments() != null && getArguments().getBoolean(ARG_USE_HORIZONTAL_VIEW))
				|| super.isHorizontalOrientation();
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_deviceList);
	}

	public TransferGroup getTransferGroup()
	{
		if (mHeldGroup == null) {
			mHeldGroup = new TransferGroup(getArguments() == null ? -1 : getArguments().getLong(
					ARG_GROUP_ID, -1));
			updateTransferGroup();
		}

		return mHeldGroup;
	}

	private void showPopupMenu(EditableListAdapter.EditableViewHolder clazz, View v)
	{
		final ShowingAssignee assignee = getAdapter().getList().get(clazz.getAdapterPosition());

		PopupMenu popupMenu = new PopupMenu(getContext(), v);
		Menu menu = popupMenu.getMenu();

		popupMenu.getMenuInflater().inflate(R.menu.popup_fragment_transfer_assignee, menu);

		popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
		{
			@Override
			public boolean onMenuItemClick(MenuItem item)
			{
				int id = item.getItemId();

				if (id == R.id.popup_changeChangeConnection) {
					TransferUtils.changeConnection(getActivity(), assignee.device,
							assignee, new TransferUtils.ConnectionUpdatedListener()
							{
								@Override
								public void onConnectionUpdated(NetworkDevice.Connection connection,
																TransferGroup.Assignee assignee)
								{
									createSnackbar(R.string.mesg_connectionUpdated, TextUtils
											.getAdapterName(getContext(), connection)).show();
								}
							});
				} else if (id == R.id.popup_remove) {
					AppUtils.getDatabase(getContext()).removeAsynchronous(getActivity(), assignee);
				} else
					return false;

				return true;
			}
		});

		popupMenu.show();
	}

	private void updateTransferGroup()
	{
		try {
			AppUtils.getDatabase(getContext()).reconstruct(mHeldGroup);
			getEmptyActionButton().setText(mHeldGroup.isServedOnWeb ? R.string.butn_hideOnBrowser
					: R.string.butn_shareOnBrowser);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
