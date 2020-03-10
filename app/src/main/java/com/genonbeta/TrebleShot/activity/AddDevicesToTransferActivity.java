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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.fragment.TransferAssigneeListFragment;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.task.AddDeviceRunningTask;
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class AddDevicesToTransferActivity extends Activity implements SnackbarPlacementProvider,
        WorkerService.AttachedTaskListener
{
    public static final String TAG = AddDevicesToTransferActivity.class.getSimpleName();

    public static final int REQUEST_CODE_CHOOSE_DEVICE = 0;

    public static final String
            EXTRA_DEVICE_ID = "extraDeviceId",
            EXTRA_GROUP_ID = "extraGroupId",
            EXTRA_FLAGS = "extraFlags";

    public static final int
            FLAG_LAUNCH_DEVICE_CHOOSER = 1;

    private TransferGroup mGroup = null;
    private AddDeviceRunningTask mTask;
    private ExtendedFloatingActionButton mActionButton;
    private ProgressBar mProgressBar;
    private ViewGroup mLayoutStatusContainer;
    private TextView mProgressTextLeft;
    private TextView mProgressTextRight;
    private IntentFilter mFilter = new IntentFilter(Kuick.ACTION_DATABASE_CHANGE);
    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (Kuick.ACTION_DATABASE_CHANGE.equals(intent.getAction())) {
                Kuick.BroadcastData data = Kuick.toData(intent);
                if (Kuick.TABLE_TRANSFERGROUP.equals(data.tableName) && !checkGroupIntegrity())
                    finish();
            }
        }
    };

    public static void startInstance(Context context, long groupId, boolean addingNewDevice)
    {
        context.startActivity(new Intent(context, AddDevicesToTransferActivity.class)
                .putExtra(EXTRA_GROUP_ID, groupId)
                .putExtra(EXTRA_FLAGS, addingNewDevice ? FLAG_LAUNCH_DEVICE_CHOOSER : 0)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_devices_to_transfer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (checkGroupIntegrity()) {
            int flags = getIntent().getIntExtra(EXTRA_FLAGS, 0);
            if ((flags & FLAG_LAUNCH_DEVICE_CHOOSER) != 0)
                startConnectionManagerActivity();
        } else
            return;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Bundle assigneeFragmentArgs = new Bundle();
        assigneeFragmentArgs.putLong(TransferAssigneeListFragment.ARG_GROUP_ID, mGroup.id);
        assigneeFragmentArgs.putBoolean(TransferAssigneeListFragment.ARG_USE_HORIZONTAL_VIEW, false);

        mProgressBar = findViewById(R.id.progressBar);
        mProgressTextLeft = findViewById(R.id.text1);
        mProgressTextRight = findViewById(R.id.text2);
        mActionButton = findViewById(R.id.content_fab);
        mLayoutStatusContainer = findViewById(R.id.layoutStatusContainer);

        TransferAssigneeListFragment assigneeListFragment = (TransferAssigneeListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.assigneeListFragment);

        if (assigneeListFragment == null) {
            assigneeListFragment = (TransferAssigneeListFragment) getSupportFragmentManager().getFragmentFactory()
                    .instantiate(this.getClassLoader(), TransferAssigneeListFragment.class.getName());
            assigneeListFragment.setArguments(assigneeFragmentArgs);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.add(R.id.assigneeListFragment, assigneeListFragment);
            transaction.commit();
        }

        resetStatusViews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home || id == R.id.actions_add_devices_done) {
            if (mTask != null)
                mTask.getInterrupter().interrupt();

            finish();
        } else if (id == R.id.actions_add_devices_help) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(R.string.text_help)
                    .setMessage(R.string.text_addDeviceHelp)
                    .setPositiveButton(R.string.butn_close, null);

            builder.show();
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.actions_add_devices, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == android.app.Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE_DEVICE && data != null
                    && data.hasExtra(AddDeviceActivity.EXTRA_DEVICE_ID)
                    && data.hasExtra(AddDeviceActivity.EXTRA_CONNECTION_ADAPTER)) {
                String deviceId = data.getStringExtra(AddDeviceActivity.EXTRA_DEVICE_ID);
                String connectionAdapter = data.getStringExtra(AddDeviceActivity.EXTRA_CONNECTION_ADAPTER);

                try {
                    NetworkDevice networkDevice = new NetworkDevice(deviceId);
                    DeviceConnection connection = new DeviceConnection(deviceId, connectionAdapter);

                    getDatabase().reconstruct(networkDevice);
                    getDatabase().reconstruct(connection);

                    doCommunicate(networkDevice, connection);
                } catch (Exception e) {
                    Toast.makeText(AddDevicesToTransferActivity.this,
                            R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onPreviousRunningTask(@Nullable WorkerService.BaseAttachableRunningTask task)
    {
        super.onPreviousRunningTask(task);

        if (task instanceof AddDeviceRunningTask) {
            mTask = ((AddDeviceRunningTask) task);
            mTask.setAnchorListener(this);
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        checkForTasks();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(mReceiver, mFilter);

        if (!checkGroupIntegrity())
            finish();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onAttachedToTask(WorkerService.BaseAttachableRunningTask task)
    {
        takeOnProcessMode();
    }

    public boolean checkGroupIntegrity()
    {
        try {
            if (getIntent() == null || !getIntent().hasExtra(EXTRA_GROUP_ID))
                throw new Exception(getString(R.string.text_empty));

            mGroup = new TransferGroup(getIntent().getLongExtra(EXTRA_GROUP_ID, -1));

            try {
                getDatabase().reconstruct(mGroup);
            } catch (Exception e) {
                throw new Exception(getString(R.string.mesg_notValidTransfer));
            }

            return true;
        } catch (Exception e) {
            Toast.makeText(AddDevicesToTransferActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }

        return false;
    }

    public Snackbar createSnackbar(final int resId, final Object... objects)
    {
        return Snackbar.make(findViewById(R.id.container), getString(resId, objects), Snackbar.LENGTH_LONG);
    }

    public void doCommunicate(final NetworkDevice device, final DeviceConnection connection)
    {
        AddDeviceRunningTask task = new AddDeviceRunningTask(mGroup, device, connection);

        task.setAnchorListener(this)
                .setTitle(getString(R.string.mesg_communicating))
                .setContentIntent(this, getIntent())
                .run(this);

        attachRunningTask(task);
    }

    @Override
    public Intent getIntent()
    {
        return super.getIntent();
    }

    public void resetStatusViews()
    {
        mProgressBar.setMax(0);
        mProgressBar.setProgress(0);

        //mTextMain.setText(R.string.text_addDevicesToTransfer);
        mActionButton.setIconResource(R.drawable.ic_add_white_24dp);
        mLayoutStatusContainer.setVisibility(View.GONE);
        mActionButton.setOnClickListener(v -> startConnectionManagerActivity());
    }

    private void startConnectionManagerActivity()
    {
        startActivityForResult(new Intent(AddDevicesToTransferActivity.this, AddDeviceActivity.class),
                REQUEST_CODE_CHOOSE_DEVICE);
    }

    public void takeOnProcessMode()
    {
        mLayoutStatusContainer.setVisibility(View.VISIBLE);
        mActionButton.setIconResource(R.drawable.ic_close_white_24dp);
        mActionButton.setOnClickListener(v -> {
            if (mTask != null)
                mTask.getInterrupter().interrupt();
        });
    }

    @Override
    public void setTaskPosition(int ofTotal, int total)
    {
        if (isFinishing())
            return;

        runOnUiThread(() -> {
            mProgressTextLeft.setText(String.valueOf(ofTotal));
            mProgressTextRight.setText(String.valueOf(total));
        });

        mProgressBar.setProgress(ofTotal);
        mProgressBar.setMax(total);
    }

    @Override
    public void updateTaskPosition(int addToOfTotal, int addToTotal)
    {
        if (isFinishing())
            return;

        if (addToOfTotal != 0) {
            int newPosition = mProgressBar.getProgress() + addToOfTotal;
            runOnUiThread(() -> mProgressTextLeft.setText(String.valueOf(newPosition)));
            mProgressBar.setProgress(newPosition);
        }

        if (addToTotal != 0) {
            int newPosition = mProgressBar.getMax() + addToTotal;
            runOnUiThread(() -> mProgressTextRight.setText(String.valueOf(newPosition)));
            mProgressBar.setMax(newPosition);
        }
    }

    @Override
    public void updateTaskStatus(String text)
    {

    }
}

