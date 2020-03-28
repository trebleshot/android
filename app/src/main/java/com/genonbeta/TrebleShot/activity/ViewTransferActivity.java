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
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.*;
import com.genonbeta.TrebleShot.fragment.TransferFileExplorerFragment;
import com.genonbeta.TrebleShot.object.*;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableBgTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.task.FileTransferTask;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.io.StreamInfo;
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * Created by: veli
 * Date: 5/23/17 1:43 PM
 */

public class ViewTransferActivity extends Activity implements SnackbarPlacementProvider, AttachedTaskListener
{
    public static final String TAG = ViewTransferActivity.class.getSimpleName();

    public static final String ACTION_LIST_TRANSFERS = "com.genonbeta.TrebleShot.action.LIST_TRANSFERS";
    public static final String EXTRA_GROUP_ID = "extraGroupId";
    public static final String EXTRA_REQUEST_ID = "extraRequestId";
    public static final String EXTRA_DEVICE_ID = "extraDeviceId";
    public static final String EXTRA_REQUEST_TYPE = "extraRequestType";

    public static final int REQUEST_ADD_DEVICES = 5045;
    private OnBackPressedListener mBackPressedListener;
    private TransferGroup mGroup;
    private IndexOfTransferGroup mIndex;
    private TransferObject mTransferObject;
    private ShowingAssignee mAssignee;
    private MenuItem mCnTestMenu;
    private MenuItem mRetryMenu;
    private MenuItem mShowFilesMenu;
    private MenuItem mAddDeviceMenu;
    private MenuItem mLimitMenu;
    private MenuItem mToggleBrowserShare;
    private int mColorActive;
    private int mColorNormal;
    private CrunchLatestDataTask mDataCruncher;
    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (Kuick.ACTION_DATABASE_CHANGE.equals(intent.getAction())) {
                Kuick.BroadcastData data = Kuick.toData(intent);

                if (Kuick.TABLE_TRANSFERGROUP.equals(data.tableName))
                    reconstructGroup();
                else if (Kuick.TABLE_TRANSFER.equals(data.tableName) && (data.inserted || data.removed))
                    updateCalculations();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_view_transfer);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mColorActive = ContextCompat.getColor(this, AppUtils.getReference(this, R.attr.colorError));
        mColorNormal = ContextCompat.getColor(this, AppUtils.getReference(this, R.attr.colorAccent));

        if (Intent.ACTION_VIEW.equals(getIntent().getAction()) && getIntent().getData() != null) {
            try {
                StreamInfo streamInfo = StreamInfo.getStreamInfo(this, getIntent().getData());

                Log.d(TAG, "Requested file is: " + streamInfo.friendlyName);

                ContentValues fileData = getDatabase().getFirstFromTable(new SQLQuery.Select(Kuick.TABLE_TRANSFER)
                        .setWhere(Kuick.FIELD_TRANSFER_FILE + "=? AND " + Kuick.FIELD_TRANSFER_TYPE + "=?",
                                streamInfo.friendlyName, TransferObject.Type.INCOMING.toString()));

                if (fileData == null)
                    throw new Exception("File is not found in the database");

                mTransferObject = new TransferObject();
                mTransferObject.reconstruct(getDatabase().getWritableDatabase(), getDatabase(), fileData);

                mGroup = new TransferGroup(mTransferObject.groupId);
                mIndex = new IndexOfTransferGroup(mGroup);
                getDatabase().reconstruct(mGroup);

                getIntent().setAction(ACTION_LIST_TRANSFERS)
                        .putExtra(EXTRA_GROUP_ID, mGroup.id);

                new TransferInfoDialog(ViewTransferActivity.this, mIndex, mTransferObject,
                        mAssignee == null ? null : mAssignee.deviceId).show();

                Log.d(TAG, "Created instance from an file intent. Original has been cleaned " +
                        "and changed to open intent");
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.mesg_notValidTransfer, Toast.LENGTH_SHORT).show();
            }
        } else if (ACTION_LIST_TRANSFERS.equals(getIntent().getAction()) && getIntent().hasExtra(EXTRA_GROUP_ID)) {
            try {
                mGroup = new TransferGroup(getIntent().getLongExtra(EXTRA_GROUP_ID, -1));
                getDatabase().reconstruct(mGroup);

                mIndex = new IndexOfTransferGroup(mGroup);

                if (getIntent().hasExtra(EXTRA_REQUEST_ID) && getIntent().hasExtra(EXTRA_DEVICE_ID)
                        && getIntent().hasExtra(EXTRA_REQUEST_TYPE)) {
                    long requestId = getIntent().getLongExtra(EXTRA_REQUEST_ID, -1);
                    String deviceId = getIntent().getStringExtra(EXTRA_DEVICE_ID);

                    try {
                        TransferObject.Type type = TransferObject.Type.valueOf(getIntent().getStringExtra(
                                EXTRA_REQUEST_TYPE));

                        TransferObject object = new TransferObject(mGroup.id, requestId, type);
                        getDatabase().reconstruct(object);

                        new TransferInfoDialog(ViewTransferActivity.this, mIndex, object, deviceId).show();
                    } catch (Exception e) {
                        // do nothing
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mGroup == null)
            finish();
        else {
            Bundle bundle = new Bundle();
            bundle.putLong(TransferFileExplorerFragment.ARG_GROUP_ID, mGroup.id);
            bundle.putString(TransferFileExplorerFragment.ARG_PATH, mTransferObject == null
                    || mTransferObject.directory == null ? null : mTransferObject.directory);

            TransferFileExplorerFragment fragment = getExplorerFragment();

            if (fragment == null) {
                fragment = (TransferFileExplorerFragment) getSupportFragmentManager().getFragmentFactory().instantiate(
                        getClassLoader(), TransferFileExplorerFragment.class.getName());
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

        IntentFilter filter = new IntentFilter();

        filter.addAction(Kuick.ACTION_DATABASE_CHANGE);
        filter.addAction(BackgroundService.ACTION_TASK_CHANGE);

        registerReceiver(mReceiver, filter);
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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.actions_transfer, menu);

        mCnTestMenu = menu.findItem(R.id.actions_transfer_test_connection);
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
            DialogUtils.showRemoveDialog(this, mGroup);
        } else if (id == R.id.actions_transfer_receiver_retry_receiving) {
            TransferUtils.recoverIncomingInterruptions(ViewTransferActivity.this, mGroup.id);
            createSnackbar(R.string.mesg_retryReceivingNotice).show();
        } else if (id == R.id.actions_transfer_receiver_show_files) {
            startActivity(new Intent(this, FileExplorerActivity.class)
                    .putExtra(FileExplorerActivity.EXTRA_FILE_PATH,
                            FileUtils.getSavePath(this, mGroup).getUri()));
        } else if (id == R.id.actions_transfer_sender_add_device) {
            startDeviceAddingActivity();
        } else if (id == R.id.actions_transfer_test_connection) {
            final List<ShowingAssignee> assignees = TransferUtils.loadAssigneeList(this, mGroup.id, null);

            if (assignees.size() == 1)
                EstablishConnectionDialog.show(ViewTransferActivity.this, assignees.get(0).device, null);
            else if (assignees.size() > 1) {
                new ChooseAssigneeDialog(this, assignees, (dialog, which) -> EstablishConnectionDialog.show(
                        this, assignees.get(which).device, null)).show();
            }
        } else if (item.getItemId() == R.id.actions_transfer_toggle_browser_share) {
            mGroup.isServedOnWeb = !mGroup.isServedOnWeb;
            getDatabase().update(mGroup);
            getDatabase().broadcast();
            showMenus();
        } else if (item.getGroupId() == R.id.actions_abs_view_transfer_activity_limit_to) {
            mAssignee = item.getOrder() < mIndex.assignees.length ? mIndex.assignees[item.getOrder()] : null;

            TransferFileExplorerFragment fragment = (TransferFileExplorerFragment)
                    getSupportFragmentManager()
                            .findFragmentById(R.id.activity_transaction_content_frame);

            if (fragment != null && fragment.getAdapter().setAssignee(mAssignee))
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

    private void attachListeners(Fragment initiatedItem)
    {
        mBackPressedListener = initiatedItem instanceof OnBackPressedListener ? (OnBackPressedListener) initiatedItem
                : null;
    }

    @Override
    public Snackbar createSnackbar(int resId, Object... objects)
    {
        TransferFileExplorerFragment explorerFragment = (TransferFileExplorerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.activity_transaction_content_frame);

        if (explorerFragment != null && explorerFragment.isAdded())
            return explorerFragment.createSnackbar(resId, objects);

        return Snackbar.make(findViewById(R.id.activity_transaction_content_frame), getString(resId, objects),
                Snackbar.LENGTH_LONG);
    }

    public int findCurrentDevicePosition()
    {
        ShowingAssignee[] assignees = mIndex.assignees;

        if (mAssignee != null && assignees.length > 0) {
            for (int i = 0; i < assignees.length; i++) {
                ShowingAssignee assignee = assignees[i];

                if (mAssignee.deviceId.equals(assignee.device.id))
                    return i;
            }
        }

        return -1;
    }

    public ShowingAssignee getAssignee()
    {
        return mAssignee;
    }

    public TransferFileExplorerFragment getExplorerFragment()
    {
        return (TransferFileExplorerFragment) getSupportFragmentManager().findFragmentById(
                R.id.activity_transaction_content_frame);
    }

    @Override
    public Identity getIdentity()
    {
        return FileTransferTask.identifyWith(mGroup.id);
    }

    @Nullable
    public ExtendedFloatingActionButton getToggleButton()
    {
        TransferFileExplorerFragment explorerFragment = getExplorerFragment();
        return explorerFragment != null ? explorerFragment.getToggleButton() : null;
    }

    public boolean isDeviceRunning(String deviceId)
    {
        return hasTaskWith(FileTransferTask.identifyWith(mGroup.id, deviceId));
    }

    public void reconstructGroup()
    {
        try {
            getDatabase().reconstruct(mGroup);
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
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

        mToggleBrowserShare.setTitle(mGroup.isServedOnWeb ? R.string.butn_hideOnBrowser : R.string.butn_shareOnBrowser);
        mToggleBrowserShare.setVisible(hasOutgoing || mGroup.isServedOnWeb);
        mCnTestMenu.setVisible(hasAnyFiles);
        mAddDeviceMenu.setVisible(hasOutgoing);
        mRetryMenu.setVisible(hasIncoming);
        mShowFilesMenu.setVisible(hasIncoming);

        if (hasOutgoing && (mIndex.assignees.length > 0 || mAssignee != null)) {
            Menu dynamicMenu = mLimitMenu.setVisible(true).getSubMenu();
            dynamicMenu.clear();

            int i = 0;
            ShowingAssignee[] assignees = mIndex.assignees;

            if (assignees.length > 0)
                for (; i < assignees.length; i++) {
                    ShowingAssignee assignee = assignees[i];

                    dynamicMenu.add(R.id.actions_abs_view_transfer_activity_limit_to, 0, i,
                            assignee.device.nickname);
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
        startActivityForResult(new Intent(this, AddDevicesToTransferActivity.class)
                .putExtra(AddDevicesToTransferActivity.EXTRA_GROUP_ID, mGroup.id), REQUEST_ADD_DEVICES);
    }

    public static void startInstance(Context context, long groupId)
    {
        context.startActivity(new Intent(context, ViewTransferActivity.class)
                .setAction(ACTION_LIST_TRANSFERS)
                .putExtra(EXTRA_GROUP_ID, groupId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void toggleTask()
    {
        List<ShowingAssignee> assigneeList = TransferUtils.loadAssigneeList(this, mGroup.id, null);

        if (assigneeList.size() > 0) {
            if (assigneeList.size() == 1) {
                ShowingAssignee assignee = assigneeList.get(0);
                toggleTaskForAssignee(assignee);
            } else
                new ToggleMultipleTransferDialog(ViewTransferActivity.this, mIndex).show();
        } else if (mIndex.hasOutgoing())
            startDeviceAddingActivity();
    }

    public void toggleTaskForAssignee(final ShowingAssignee assignee)
    {
        try {
            if (hasTaskWith(FileTransferTask.identifyWith(mGroup.id, assignee.deviceId)))
                TransferUtils.pauseTransfer(this, assignee);
            else {
                getDatabase().reconstruct(new DeviceConnection(assignee));
                TransferUtils.startTransferWithTest(this, mGroup, assignee);
            }
        } catch (Exception e) {
            e.printStackTrace();

            createSnackbar(R.string.mesg_transferConnectionNotSetUpFix)
                    .setAction(R.string.butn_setUp, v -> TransferUtils.changeConnection(ViewTransferActivity.this,
                            assignee.device, assignee, (connection, assignee1) -> createSnackbar(
                                    R.string.mesg_connectionUpdated, TextUtils.getAdapterName(getApplicationContext(),
                                            connection)).show())).show();
        }
    }

    public synchronized void updateCalculations()
    {
        if (mDataCruncher == null || !mDataCruncher.requestRestart()) {
            mDataCruncher = new CrunchLatestDataTask(() -> {
                showMenus();
                findViewById(R.id.activity_transaction_no_devices_warning).setVisibility(
                        mIndex.assignees.length > 0 ? View.GONE : View.VISIBLE);

                if (mIndex.assignees.length == 0)
                    if (mTransferObject != null) {
                        new TransferInfoDialog(ViewTransferActivity.this, mIndex,
                                mTransferObject, mAssignee == null ? null : mAssignee.deviceId).show();
                        mTransferObject = null;
                    }
            });

            mDataCruncher.execute(this);
        }
    }

    @Override
    public void onTaskStateChanged(BaseAttachableBgTask task)
    {
        if (task instanceof FileTransferTask)
            ((FileTransferTask) task).setAnchor(this);
    }

    @Override
    public boolean onTaskMessage(TaskMessage message)
    {
        return false;
    }

    public static class CrunchLatestDataTask extends AsyncTask<ViewTransferActivity, Void, Void>
    {
        private PostExecuteListener mListener;
        private boolean mRestartRequested = false;

        public CrunchLatestDataTask(PostExecuteListener listener)
        {
            mListener = listener;
        }

        /* "possibility of having more than one ViewTransferActivity" < "sun turning into black hole" */
        @Override
        protected Void doInBackground(ViewTransferActivity... activities)
        {
            do {
                mRestartRequested = false;

                for (ViewTransferActivity activity : activities)
                    if (activity.mGroup != null)
                        TransferUtils.loadGroupInfo(activity, activity.mIndex, activity.getAssignee());
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
            mListener.onPostExecute();
        }

        /* Should we have used a generic type class for this?
         * This interface aims to keep its parent class non-anonymous
         */
        public interface PostExecuteListener
        {
            void onPostExecute();
        }
    }
}
