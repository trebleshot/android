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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.TransferAssigneeListAdapter;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.ShowingAssignee;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.TrebleShot.widget.recyclerview.PaddingItemDecoration;
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

/**
 * created by: veli
 * date: 06.04.2018 12:58
 */
public class TransferAssigneeListFragment
        extends DynamicRecyclerViewFragment<ShowingAssignee, RecyclerViewAdapter.ViewHolder, TransferAssigneeListAdapter>
        implements TitleSupport
{
    public static final String ARG_GROUP_ID = "groupId";
    public static final String ARG_USE_HORIZONTAL_VIEW = "useHorizontalView";

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

    private TransferGroup mHeldGroup;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setEmptyImage(R.drawable.ic_device_hub_white_24dp);
        setEmptyText(getString(R.string.text_noDeviceForTransfer));
        getListView().addItemDecoration(new PaddingItemDecoration((int) getResources().getDimension(R.dimen.padding_list_content_parent_layout), true, isHorizontalOrientation()));
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
                        ShowingAssignee assignee = getAdapter().getList().get(clazz.getAdapterPosition());

                        new DeviceInfoDialog(getActivity(), AppUtils.getDatabase(getContext()), AppUtils.getDefaultPreferences(getContext()), assignee.device)
                                .show();
                    }
                });

                clazz.getView().findViewById(R.id.menu).setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
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
                                    TransferUtils.changeConnection(getActivity(), AppUtils.getDatabase(getContext()), getTransferGroup(), assignee.device, new TransferUtils.ConnectionUpdatedListener()
                                    {
                                        @Override
                                        public void onConnectionUpdated(NetworkDevice.Connection connection, TransferGroup.Assignee assignee)
                                        {
                                            createSnackbar(R.string.mesg_connectionUpdated, TextUtils.getAdapterName(getContext(), connection))
                                                    .show();
                                        }
                                    });
                                } else if (id == R.id.popup_remove) {
                                    AppUtils.getDatabase(getContext()).removeAsynchronous(assignee);
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

        return new TransferAssigneeListAdapter(getContext(), AppUtils.getDatabase(getContext()))
        {
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
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
            mHeldGroup = new TransferGroup(getArguments().getLong(ARG_GROUP_ID, -1));

            try {
                AppUtils.getDatabase(getContext()).reconstruct(mHeldGroup);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return mHeldGroup;
    }
}
