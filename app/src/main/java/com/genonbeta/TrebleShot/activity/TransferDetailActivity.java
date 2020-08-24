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

package com.genonbeta.TrebleShot.activity;

import android.content.*;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.DialogUtils;
import com.genonbeta.TrebleShot.dialog.ToggleMultipleTransferDialog;
import com.genonbeta.TrebleShot.dialog.TransferInfoDialog;
import com.genonbeta.TrebleShot.exception.ConnectionNotFoundException;
import com.genonbeta.TrebleShot.fragment.TransferItemExplorerFragment;
import com.genonbeta.TrebleShot.object.*;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.task.FileTransferTask;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.Transfers;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.io.StreamInfo;
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by: veli
 * Date: 5/23/17 1:43 PM
 */

public class TransferDetailActivity extends Activity implements SnackbarPlacementProvider, AttachedTaskListener
{
    public static final String TAG = TransferDetailActivity.class.getSimpleName();

    public static final String ACTION_LIST_TRANSFERS = "com.genonbeta.TrebleShot.action.LIST_TRANSFERS";

    public static final String EXTRA_TRANSFER = "extraTransfer";
    public static final String EXTRA_TRANSFER_ITEM_ID = "extraTransferItemId";
    public static final String EXTRA_DEVICE = "extraDevice";
    public static final String EXTRA_TRANSFER_TYPE = "extraTransferType";

    public static final int REQUEST_ADD_DEVICES = 5045;


    private OnBackPressedListener mBackPressedListener;
    private Transfer mTransfer;
    private TransferIndex mIndex;
    private Button mOpenWebShareButton;
    private View mNoDevicesNoticeText;
    private LoadedMember mMember;
    private MenuItem mRetryMenu;
    private MenuItem mShowFilesMenu;
    private MenuItem mAddDeviceMenu;
    private MenuItem mLimitMenu;
    private MenuItem mToggleBrowserShare;
    private int mColorActive;
    private int mColorNormal;
    private CrunchLatestDataTask mDataCruncher;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (Kuick.ACTION_DATABASE_CHANGE.equals(intent.getAction())) {
                Kuick.BroadcastData data = Kuick.toData(intent);

                if (Kuick.TABLE_TRANSFER.equals(data.tableName))
                    reconstructGroup();
                else if (Kuick.TABLE_TRANSFERITEM.equals(data.tableName) && (data.inserted || data.removed)
                        || Kuick.TABLE_TRANSFERMEMBER.equals(data.tableName) && (data.inserted || data.removed))
                    updateCalculations();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_view_transfer);

        TransferItem transferItem = null;
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTransfer = savedInstanceState != null ? savedInstanceState.getParcelable(EXTRA_TRANSFER) : null;
        mOpenWebShareButton = findViewById(R.id.activity_transfer_detail_open_web_share_button);
        mNoDevicesNoticeText = findViewById(R.id.activity_transfer_detail_no_devices_warning);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mColorActive = ContextCompat.getColor(this, AppUtils.getReference(this, R.attr.colorError));
        mColorNormal = ContextCompat.getColor(this, AppUtils.getReference(this, R.attr.colorAccent));

        mOpenWebShareButton.setOnClickListener(v -> startActivity(new Intent(this, WebShareActivity.class)));

        if (mTransfer != null) {
            Log.d(TAG, "onCreate: Created transfer instance from the bundle");
            setTransfer(mTransfer);
        } else if (Intent.ACTION_VIEW.equals(getIntent().getAction()) && getIntent().getData() != null) {
            try {
                StreamInfo streamInfo = StreamInfo.getStreamInfo(this, getIntent().getData());

                Log.d(TAG, "Requested file is: " + streamInfo.friendlyName);

                ContentValues fileData = getDatabase().getFirstFromTable(new SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                        .setWhere(Kuick.FIELD_TRANSFERITEM_FILE + "=? AND " + Kuick.FIELD_TRANSFERITEM_TYPE + "=?",
                                streamInfo.friendlyName, TransferItem.Type.INCOMING.toString()));

                if (fileData == null)
                    throw new Exception("File is not found in the database");

                transferItem = new TransferItem();
                transferItem.reconstruct(getDatabase().getWritableDatabase(), getDatabase(), fileData);

                Transfer transfer = new Transfer(transferItem.transferId);
                getDatabase().reconstruct(transfer);

                setTransfer(transfer);

                new TransferInfoDialog(this, mIndex, transferItem, null).show();

                Log.d(TAG, "Created instance from an file intent. Original has been cleaned " +
                        "and changed to open intent");
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.mesg_notValidTransfer, Toast.LENGTH_SHORT).show();
            }
        } else if (ACTION_LIST_TRANSFERS.equals(getIntent().getAction()) && getIntent().hasExtra(EXTRA_TRANSFER)) {
            setTransfer(getIntent().getParcelableExtra(EXTRA_TRANSFER));

            try {
                if (getIntent().hasExtra(EXTRA_TRANSFER_ITEM_ID) && getIntent().hasExtra(EXTRA_DEVICE)
                        && getIntent().hasExtra(EXTRA_TRANSFER_TYPE)) {
                    long requestId = getIntent().getLongExtra(EXTRA_TRANSFER_ITEM_ID, -1);
                    Device device = getIntent().getParcelableExtra(EXTRA_DEVICE);
                    TransferItem.Type type = (TransferItem.Type) getIntent().getSerializableExtra(EXTRA_TRANSFER_TYPE);

                    transferItem = new TransferItem(mTransfer.id, requestId, type);
                    getDatabase().reconstruct(transferItem);

                    if (device != null)
                        new TransferInfoDialog(this, mIndex, transferItem, device.uid).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mTransfer == null)
            finish();
        else {
            Bundle bundle = new Bundle();
            bundle.putLong(TransferItemExplorerFragment.ARG_TRANSFER_ID, mTransfer.id);
            bundle.putString(TransferItemExplorerFragment.ARG_PATH, transferItem == null
                    || transferItem.directory == null ? null : transferItem.directory);

            TransferItemExplorerFragment fragment = getExplorerFragment();

            if (fragment == null) {
                fragment = (TransferItemExplorerFragment) getSupportFragmentManager().getFragmentFactory()
                        .instantiate(getClassLoader(), TransferItemExplorerFragment.class.getName());
                fragment.setArguments(bundle);

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                transaction.add(R.id.activity_transaction_content_frame, fragment);
                transaction.commit();
            }

            attachListeners(fragment);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        registerReceiver(mReceiver, new IntentFilter(Kuick.ACTION_DATABASE_CHANGE));
        reconstructGroup();
        updateCalculations();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_TRANSFER, mTransfer);
    }

    @Override
    protected void onAttachTasks(List<BaseAttachableAsyncTask> taskList)
    {
        super.onAttachTasks(taskList);

        for (BaseAttachableAsyncTask attachableAsyncTask : taskList) {
            if (attachableAsyncTask instanceof FileTransferTask)
                ((FileTransferTask) attachableAsyncTask).setAnchor(this);
        }

        if (!hasTaskOf(FileTransferTask.class))
            showMenus();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.actions_transfer, menu);

        mRetryMenu = menu.findItem(R.id.actions_transfer_receiver_retry_receiving);
        mShowFilesMenu = menu.findItem(R.id.actions_transfer_receiver_show_files);
        mAddDeviceMenu = menu.findItem(R.id.actions_transfer_sender_add_device);
        mLimitMenu = menu.findItem(R.id.actions_transfer_limit_to);
        mToggleBrowserShare = menu.findItem(R.id.actions_transfer_toggle_browser_share);

        showMenus();

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        int devicePosition = findCurrentDevicePosition();
        Menu thisMenu = menu.findItem(R.id.actions_transfer_limit_to).getSubMenu();

        MenuItem checkedItem = null;

        if ((devicePosition < 0 || (checkedItem = thisMenu.getItem(devicePosition)) == null) && thisMenu.size() > 0)
            checkedItem = thisMenu.getItem(thisMenu.size() - 1);

        if (checkedItem != null)
            checkedItem.setChecked(true);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        } else if (id == R.id.actions_transfer_remove) {
            DialogUtils.showRemoveDialog(this, mTransfer);
        } else if (id == R.id.actions_transfer_receiver_retry_receiving) {
            Transfers.recoverIncomingInterruptions(TransferDetailActivity.this, mTransfer.id);
            createSnackbar(R.string.mesg_retryReceivingNotice).show();
        } else if (id == R.id.actions_transfer_receiver_show_files) {
            startActivity(new Intent(this, FileExplorerActivity.class)
                    .putExtra(FileExplorerActivity.EXTRA_FILE_PATH,
                            FileUtils.getSavePath(this, mTransfer).getUri()));
        } else if (id == R.id.actions_transfer_sender_add_device) {
            startDeviceAddingActivity();
        } else if (item.getItemId() == R.id.actions_transfer_toggle_browser_share) {
            mTransfer.isServedOnWeb = !mTransfer.isServedOnWeb;
            getDatabase().update(mTransfer);
            getDatabase().broadcast();
            showMenus();
        } else if (item.getGroupId() == R.id.actions_abs_view_transfer_activity_limit_to) {
            mMember = item.getOrder() < mIndex.members.length ? mIndex.members[item.getOrder()] : null;

            TransferItemExplorerFragment fragment = (TransferItemExplorerFragment)
                    getSupportFragmentManager()
                            .findFragmentById(R.id.activity_transaction_content_frame);

            if (fragment != null && fragment.getAdapter().setMember(mMember))
                fragment.refreshList();
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public void onBackPressed()
    {
        if (mBackPressedListener == null || !mBackPressedListener.onBackPressed())
            super.onBackPressed();
    }


    @Override
    public void onTaskStateChange(BaseAttachableAsyncTask task,
                                  com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask.State state)
    {
        if (task instanceof FileTransferTask) {
            switch (state) {
                case Finished:
                case Starting:
                    showMenus();
            }
        }
    }

    @Override
    public boolean onTaskMessage(TaskMessage message)
    {
        runOnUiThread(() -> message.toDialogBuilder(this).show());
        return true;
    }

    private void attachListeners(Fragment initiatedItem)
    {
        mBackPressedListener = initiatedItem instanceof OnBackPressedListener ? (OnBackPressedListener) initiatedItem
                : null;
    }

    @Override
    public Snackbar createSnackbar(int resId, Object... objects)
    {
        TransferItemExplorerFragment explorerFragment = (TransferItemExplorerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.activity_transaction_content_frame);

        if (explorerFragment != null && explorerFragment.isAdded())
            return explorerFragment.createSnackbar(resId, objects);

        return Snackbar.make(findViewById(R.id.activity_transaction_content_frame), getString(resId, objects),
                Snackbar.LENGTH_LONG);
    }

    public int findCurrentDevicePosition()
    {
        LoadedMember[] members = mIndex.members;

        if (mMember != null && members.length > 0) {
            for (int i = 0; i < members.length; i++) {
                LoadedMember member = members[i];

                if (mMember.deviceId.equals(member.device.uid))
                    return i;
            }
        }

        return -1;
    }

    public LoadedMember getMember()
    {
        return mMember;
    }

    public TransferItemExplorerFragment getExplorerFragment()
    {
        return (TransferItemExplorerFragment) getSupportFragmentManager().findFragmentById(
                R.id.activity_transaction_content_frame);
    }

    @Override
    public Identity getIdentity()
    {
        return FileTransferTask.identifyWith(mTransfer.id);
    }

    @Nullable
    public ExtendedFloatingActionButton getToggleButton()
    {
        TransferItemExplorerFragment explorerFragment = getExplorerFragment();
        return explorerFragment != null ? explorerFragment.getToggleButton() : null;
    }

    public boolean isDeviceRunning(String deviceId)
    {
        return hasTaskWith(FileTransferTask.identifyWith(mTransfer.id, deviceId));
    }

    public void reconstructGroup()
    {
        try {
            getDatabase().reconstruct(mTransfer);
            showMenus();
        } catch (Exception e) {
            finish();
        }
    }

    private void setTransfer(Transfer transfer)
    {
        mTransfer = transfer;
        mIndex = new TransferIndex(transfer);
    }

    public void showMenus()
    {
        boolean hasRunning = hasTaskOf(FileTransferTask.class);
        boolean hasAnyFiles = mIndex.numberOfTotal() > 0;
        boolean hasIncoming = mIndex.hasIncoming();
        boolean hasOutgoing = mIndex.hasOutgoing();
        ExtendedFloatingActionButton toggleButton = getToggleButton();

        if (mRetryMenu == null || mShowFilesMenu == null)
            return;

        if (toggleButton != null) {
            if (Build.VERSION.SDK_INT <= 14 || !toggleButton.hasOnClickListeners())
                toggleButton.setOnClickListener(v -> toggleTask());

            if (hasAnyFiles || hasRunning) {
                toggleButton.setIconResource(hasRunning ? R.drawable.ic_pause_white_24dp
                        : R.drawable.ic_play_arrow_white_24dp);
                toggleButton.setBackgroundTintList(ColorStateList.valueOf(hasRunning ? mColorActive : mColorNormal));

                if (hasRunning)
                    toggleButton.setText(R.string.butn_pause);
                else
                    toggleButton.setText(hasIncoming == hasOutgoing ? R.string.butn_start
                            : (hasIncoming ? R.string.butn_receive : R.string.butn_send));

                toggleButton.setVisibility(View.VISIBLE);
            } else
                toggleButton.setVisibility(View.GONE);
        }

        mOpenWebShareButton.setVisibility(mTransfer.isServedOnWeb ? View.VISIBLE : View.GONE);
        mNoDevicesNoticeText.setVisibility(mIndex.members.length > 0 || mTransfer.isServedOnWeb
                ? View.GONE : View.VISIBLE);

        mToggleBrowserShare.setTitle(mTransfer.isServedOnWeb ? R.string.butn_hideOnBrowser : R.string.butn_shareOnBrowser);
        mToggleBrowserShare.setVisible(hasOutgoing || mTransfer.isServedOnWeb);
        mAddDeviceMenu.setVisible(hasOutgoing);
        mRetryMenu.setVisible(hasIncoming);
        mShowFilesMenu.setVisible(hasIncoming);

        if (hasOutgoing && (mIndex.members.length > 0 || mMember != null)) {
            Menu dynamicMenu = mLimitMenu.setVisible(true).getSubMenu();
            dynamicMenu.clear();

            int i = 0;
            LoadedMember[] members = mIndex.members;

            if (members.length > 0)
                for (; i < members.length; i++) {
                    LoadedMember member = members[i];

                    dynamicMenu.add(R.id.actions_abs_view_transfer_activity_limit_to, 0, i,
                            member.device.username);
                }

            dynamicMenu.add(R.id.actions_abs_view_transfer_activity_limit_to, 0, i, R.string.text_none);
            dynamicMenu.setGroupCheckable(R.id.actions_abs_view_transfer_activity_limit_to, true,
                    true);
        } else
            mLimitMenu.setVisible(false);

        setTitle(getResources().getQuantityString(R.plurals.text_files, mIndex.numberOfTotal(),
                mIndex.numberOfTotal()));
    }

    public void startDeviceAddingActivity()
    {
        startActivityForResult(new Intent(this, TransferMemberActivity.class)
                .putExtra(TransferMemberActivity.EXTRA_TRANSFER, mTransfer), REQUEST_ADD_DEVICES);
    }

    public static void startInstance(Context context, Transfer transfer)
    {
        context.startActivity(new Intent(context, TransferDetailActivity.class)
                .setAction(ACTION_LIST_TRANSFERS)
                .putExtra(EXTRA_TRANSFER, transfer)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void toggleTask()
    {
        List<LoadedMember> memberList = Transfers.loadMemberList(this, mTransfer.id, null);

        if (memberList.size() > 0) {
            if (memberList.size() == 1) {
                LoadedMember member = memberList.get(0);
                toggleTaskForMember(member);
            } else
                new ToggleMultipleTransferDialog(TransferDetailActivity.this, mIndex).show();
        } else if (mIndex.hasOutgoing())
            startDeviceAddingActivity();
    }

    public void toggleTaskForMember(final LoadedMember member)
    {
        if (hasTaskWith(FileTransferTask.identifyWith(mTransfer.id, member.deviceId)))
            Transfers.pauseTransfer(this, member);
        else {
            try {
                Transfers.getAddressListFor(getDatabase(), member.deviceId);
                Transfers.startTransferWithTest(this, mTransfer, member);
            } catch (ConnectionNotFoundException e) {
                createSnackbar(R.string.mesg_transferConnectionNotSetUpFix).show();
            }
        }
    }

    public synchronized void updateCalculations()
    {
        if (mDataCruncher == null || !mDataCruncher.requestRestart()) {
            mDataCruncher = new CrunchLatestDataTask(this::showMenus);
            mDataCruncher.execute(this);
        }
    }

    public static class CrunchLatestDataTask extends AsyncTask<TransferDetailActivity, Void, Void>
    {
        private final PostExecutionListener mListener;
        private boolean mRestartRequested = false;

        public CrunchLatestDataTask(PostExecutionListener listener)
        {
            mListener = listener;
        }

        /* "possibility of having more than one ViewTransferActivity" < "sun turning into black hole" */
        @Override
        protected Void doInBackground(TransferDetailActivity... activities)
        {
            do {
                mRestartRequested = false;

                for (TransferDetailActivity activity : activities)
                    if (activity.mTransfer != null)
                        Transfers.loadTransferInfo(activity, activity.mIndex, activity.getMember());
            } while (mRestartRequested && !isCancelled());

            return null;
        }

        public boolean requestRestart()
        {
            if (getStatus().equals(Status.RUNNING))
                mRestartRequested = true;

            return mRestartRequested;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            super.onPostExecute(aVoid);
            if (!isCancelled())
                mListener.onPostExecute();
        }

        /* Should we have used a generic type class for this?
         * This interface aims to keep its parent class non-anonymous
         */
        public interface PostExecutionListener
        {
            void onPostExecute();
        }
    }
}
