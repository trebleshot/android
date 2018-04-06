package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.TransferAssigneeListAdapter;
import com.genonbeta.TrebleShot.app.RecyclerViewFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.TitleSupport;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;

/**
 * created by: veli
 * date: 06.04.2018 12:58
 */
public class TransferAssigneeListFragment
		extends RecyclerViewFragment<TransferAssigneeListAdapter.ShowingAssignee, RecyclerViewAdapter.ViewHolder, TransferAssigneeListAdapter>
		implements TitleSupport
{
	private TransferGroup mGroup;
	private BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())
					&& AccessDatabase.TABLE_TRANSFERASSIGNEE.equals(intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME)))
				refreshList();
		}
	};

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.actions_transfer_assignee, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.actions_transfer_assignee_send_another) {

		} else
			return super.onOptionsItemSelected(item);

		return true;
	}

	@Override
	public TransferAssigneeListAdapter onAdapter()
	{
		final AppUtils.QuickActions<RecyclerViewAdapter.ViewHolder> actions = new AppUtils.QuickActions<RecyclerViewAdapter.ViewHolder>()
		{
			@Override
			public void onQuickActions(final RecyclerViewAdapter.ViewHolder clazz)
			{
				clazz.getView().setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						TransferAssigneeListAdapter.ShowingAssignee assignee = getAdapter().getList().get(clazz.getAdapterPosition());

						new DeviceInfoDialog(getActivity(), getDatabase(), getDefaultPreferences(), assignee.device)
								.show();
					}
				});

				clazz.getView().findViewById(R.id.menu).setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						final TransferAssigneeListAdapter.ShowingAssignee assignee = getAdapter().getList().get(clazz.getAdapterPosition());

						PopupMenu popupMenu = new PopupMenu(getContext(), v);
						popupMenu.getMenuInflater().inflate(R.menu.popup_fragment_transfer_assignee, popupMenu.getMenu());

						popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
						{
							@Override
							public boolean onMenuItemClick(MenuItem item)
							{
								int id = item.getItemId();

								if (id == R.id.popup_changeChangeConnection) {
									TransferUtils.changeConnection(getActivity(), getDatabase(), mGroup, assignee.device, null);
								} else if (id == R.id.popup_remove) {
									getDatabase().remove(assignee);
								} else
									return false;

								return true;
							}
						});

						popupMenu.show();
					}
				});
			}
		};

		return new TransferAssigneeListAdapter(getContext(), getDatabase())
		{
			@NonNull
			@Override
			public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
			{
				return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), actions);
			}
		}.setGroup(mGroup);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

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
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_deviceList);
	}

	public TransferAssigneeListFragment setGroup(TransferGroup group)
	{
		mGroup = group;
		return this;
	}
}
