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
import android.content.res.ColorStateList;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.fragment.TransferMemberListFragment;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.object.Transfer;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.task.AddDeviceTask;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class AddDevicesToTransferActivity extends Activity implements SnackbarPlacementProvider, AttachedTaskListener
{
    public static final String
            TAG = AddDevicesToTransferActivity.class.getSimpleName(),
            EXTRA_DEVICE = "extraDevice",
            EXTRA_TRANSFER = "extraTransfer",
            EXTRA_FLAGS = "extraFlags";

    public static final int
            REQUEST_CODE_CHOOSE_DEVICE = 0,
            FLAG_LAUNCH_DEVICE_CHOOSER = 1;

    private Transfer mTransfer = null;
    private ExtendedFloatingActionButton mActionButton;
    private ProgressBar mProgressBar;
    private ViewGroup mLayoutStatusContainer;
    private TextView mProgressTextCurrent;
    private TextView mProgressTextTotal;
    private int mColorActive;
    private int mColorNormal;
    private final IntentFilter mFilter = new IntentFilter(Kuick.ACTION_DATABASE_CHANGE);
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (Kuick.ACTION_DATABASE_CHANGE.equals(intent.getAction())) {
                Kuick.BroadcastData data = Kuick.toData(intent);
                if (Kuick.TABLE_TRANSFER.equals(data.tableName) && !checkGroupIntegrity())
                    finish();
            }
        }
    };

    public static void startInstance(Context context, Transfer transfer, boolean addingNewDevice)
    {
        context.startActivity(new Intent(context, AddDevicesToTransferActivity.class)
                .putExtra(EXTRA_TRANSFER, transfer)
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

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle memberFragmentArgs = new Bundle();
        memberFragmentArgs.putLong(TransferMemberListFragment.ARG_TRANSFER_ID, mTransfer.id);
        memberFragmentArgs.putBoolean(TransferMemberListFragment.ARG_USE_HORIZONTAL_VIEW, false);

        mColorActive = ContextCompat.getColor(this, AppUtils.getReference(this, R.attr.colorError));
        mColorNormal = ContextCompat.getColor(this, AppUtils.getReference(this, R.attr.colorAccent));
        mProgressBar = findViewById(R.id.progressBar);
        mProgressTextCurrent = findViewById(R.id.text1);
        mProgressTextTotal = findViewById(R.id.text2);
        mActionButton = findViewById(R.id.content_fab);
        mLayoutStatusContainer = findViewById(R.id.layoutStatusContainer);

        TransferMemberListFragment memberListFragment = (TransferMemberListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.membersListFragment);

        if (memberListFragment == null) {
            memberListFragment = (TransferMemberListFragment) getSupportFragmentManager().getFragmentFactory()
                    .instantiate(this.getClassLoader(), TransferMemberListFragment.class.getName());
            memberListFragment.setArguments(memberFragmentArgs);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.add(R.id.membersListFragment, memberListFragment);
            transaction.commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home || id == R.id.actions_add_devices_done) {
            interruptAllTasks(true);
            finish();
        } else if (id == R.id.actions_add_devices_help) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.text_help)
                    .setMessage(R.string.text_addDeviceHelp)
                    .setPositiveButton(R.string.butn_close, null)
                    .show();
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.actions_add_devices, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == android.app.Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE_DEVICE && data != null
                    && data.hasExtra(AddDeviceActivity.EXTRA_DEVICE)
                    && data.hasExtra(AddDeviceActivity.EXTRA_DEVICE_ADDRESS)) {
                Device device = data.getParcelableExtra(AddDeviceActivity.EXTRA_DEVICE);
                DeviceAddress connection = data.getParcelableExtra(AddDeviceActivity.EXTRA_DEVICE_ADDRESS);

                if (device != null && connection != null)
                    runUiTask(new AddDeviceTask(mTransfer, device, connection));
            }
        }
    }

    @Override
    protected void onAttachTasks(List<BaseAttachableAsyncTask> taskList)
    {
        super.onAttachTasks(taskList);

        boolean hasOngoing = false;
        for (BaseAttachableAsyncTask task : taskList)
            if (task instanceof AddDeviceTask) {
                ((AddDeviceTask) task).setAnchor(this);
                hasOngoing = true;
            }

        if (!hasOngoing)
            setNowAdding(false);
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
    public void onTaskStateChanged(BaseAttachableAsyncTask task)
    {
        if (task instanceof AddDeviceTask) {
            if (task.isFinished())
                setNowAdding(false);
            else {
                int progress = task.progress().getCurrent();
                int total = task.progress().getTotal();

                runOnUiThread(() -> {
                    mProgressTextCurrent.setText(String.valueOf(progress));
                    mProgressTextTotal.setText(String.valueOf(total));
                });

                mProgressBar.setProgress(progress);
                mProgressBar.setMax(total);
            }
        }
    }

    @Override
    public boolean onTaskMessage(TaskMessage message)
    {
        return false;
    }

    public boolean checkGroupIntegrity()
    {
        try {
            if (getIntent() == null || !getIntent().hasExtra(EXTRA_TRANSFER))
                throw new Exception(getString(R.string.text_empty));

            if (mTransfer == null)
                mTransfer = getIntent().getParcelableExtra(EXTRA_TRANSFER);

            try {
                if (mTransfer == null)
                    throw new Exception();

                getDatabase().reconstruct(mTransfer);
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

    public void setNowAdding(boolean adding)
    {
        mLayoutStatusContainer.setVisibility(adding ? View.VISIBLE : View.GONE);
        mActionButton.setIconResource(adding ? R.drawable.ic_close_white_24dp : R.drawable.ic_add_white_24dp);
        mActionButton.setText(adding ? R.string.butn_cancel : R.string.butn_addMore);
        mActionButton.setBackgroundTintList(ColorStateList.valueOf(adding ? mColorActive : mColorNormal));
        mActionButton.setOnClickListener(v -> {
            if (adding)
                interruptAllTasks(true);
            else
                startConnectionManagerActivity();
        });
    }

    private void startConnectionManagerActivity()
    {
        startActivityForResult(new Intent(this, AddDeviceActivity.class), REQUEST_CODE_CHOOSE_DEVICE);
    }
}

