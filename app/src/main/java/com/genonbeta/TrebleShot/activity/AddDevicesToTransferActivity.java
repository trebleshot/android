package com.genonbeta.TrebleShot.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.ConnectionChooserDialog;
import com.genonbeta.TrebleShot.fragment.TransferAssigneeListFragment;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.genonbeta.android.framework.util.Interrupter;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class AddDevicesToTransferActivity extends Activity
        implements SnackbarSupport
{
    public static final String TAG = AddDevicesToTransferActivity.class.getSimpleName();

    public static final int REQUEST_CODE_CHOOSE_DEVICE = 0;
    public static final int WORKER_TASK_CONNECT_SERVER = 2;

    public static final String EXTRA_DEVICE_ID = "extraDeviceId";
    public static final String EXTRA_GROUP_ID = "extraGroupId";

    private TransferGroup mGroup = null;
    private Interrupter mInterrupter = new Interrupter();
    private Button mActionButton;
    private ProgressBar mProgressBar;
    private ViewGroup mLayoutStatusContainer;
    private TextView mProgressTextLeft;
    private TextView mProgressTextRight;
    private TextView mTextMain;
    private TextView mTextInfo;
    private TransferAssigneeListFragment mAssigneeFragment;
    private IntentFilter mFilter = new IntentFilter(AccessDatabase.ACTION_DATABASE_CHANGE);
    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction()))
                if (intent.hasExtra(AccessDatabase.EXTRA_TABLE_NAME)
                        && AccessDatabase.TABLE_TRANSFERGROUP.equals(intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME)))
                    if (!checkGroupIntegrity())
                        finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_devices_to_transfer);

        if (!checkGroupIntegrity())
            return;

        Bundle assigneeFragmentArgs = new Bundle();
        assigneeFragmentArgs.putLong(TransferAssigneeListFragment.ARG_GROUP_ID, mGroup.groupId);
        assigneeFragmentArgs.putBoolean(TransferAssigneeListFragment.ARG_USE_HORIZONTAL_VIEW, true);

        mProgressBar = findViewById(R.id.progressBar);
        mProgressTextLeft = findViewById(R.id.text1);
        mProgressTextRight = findViewById(R.id.text2);
        mTextMain = findViewById(R.id.textMain);
        mTextInfo = findViewById(R.id.textInfo);
        mActionButton = findViewById(R.id.actionButton);
        mLayoutStatusContainer = findViewById(R.id.layoutStatusContainer);
        mAssigneeFragment = (TransferAssigneeListFragment) getSupportFragmentManager().findFragmentById(R.id.assigneListFragment);

        findViewById(R.id.returnButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });

        if (mAssigneeFragment == null) {
            mAssigneeFragment = (TransferAssigneeListFragment) Fragment
                    .instantiate(this, TransferAssigneeListFragment.class.getName(), assigneeFragmentArgs);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.add(R.id.assigneListFragment, mAssigneeFragment);
            transaction.commit();
        }

        resetStatusViews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == android.app.Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE_DEVICE
                    && data != null
                    && data.hasExtra(ConnectionManagerActivity.EXTRA_DEVICE_ID)
                    && data.hasExtra(ConnectionManagerActivity.EXTRA_CONNECTION_ADAPTER)) {
                String deviceId = data.getStringExtra(ConnectionManagerActivity.EXTRA_DEVICE_ID);
                String connectionAdapter = data.getStringExtra(ConnectionManagerActivity.EXTRA_CONNECTION_ADAPTER);

                try {
                    NetworkDevice networkDevice = new NetworkDevice(deviceId);
                    NetworkDevice.Connection connection = new NetworkDevice.Connection(deviceId, connectionAdapter);

                    getDatabase().reconstruct(networkDevice);
                    getDatabase().reconstruct(connection);

                    doCommunicate(networkDevice, connection);
                } catch (Exception e) {
                    Toast.makeText(AddDevicesToTransferActivity.this, R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        getDefaultInterrupter().interrupt(false);
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

    protected void doCommunicate(final NetworkDevice device, final NetworkDevice.Connection connection)
    {
        takeOnProcessMode();

        runOnWorkerService(new WorkerService.RunningTask(TAG, WORKER_TASK_CONNECT_SERVER)
        {
            @Override
            public void onRun()
            {
                final WorkerService.RunningTask thisTask = this;
                updateText(thisTask, getString(R.string.mesg_communicating));

                CommunicationBridge.connect(getDatabase(), true, new CommunicationBridge.Client.ConnectionHandler()
                {
                    @Override
                    public void onConnect(CommunicationBridge.Client client)
                    {
                        client.setDevice(device);

                        try {
                            boolean doPublish = false;
                            final JSONObject jsonRequest = new JSONObject();
                            final TransferGroup.Assignee assignee = new TransferGroup.Assignee(mGroup, device, connection);
                            final ArrayList<TransferObject> pendingRegistry = new ArrayList<>();

                            final ArrayList<TransferObject> existingRegistry = new ArrayList<>(getDatabase()
                                    .castQuery(new SQLQuery.Select(AccessDatabase.DIVIS_TRANSFER)
                                            .setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
                                                            + AccessDatabase.FIELD_TRANSFER_TYPE + "=?",
                                                    String.valueOf(mGroup.groupId),
                                                    TransferObject.Type.OUTGOING.toString()), TransferObject.class));
                            final SQLiteDatabase.ProgressUpdater progressUpdater = new SQLiteDatabase.ProgressUpdater()
                            {
                                @Override
                                public void onProgressChange(int total, int current)
                                {
                                    updateProgress(total, current);
                                }

                                @Override
                                public boolean onProgressState()
                                {
                                    return !getDefaultInterrupter().interrupted();
                                }
                            };

                            try {
                                // Checks if the current assignee is already on the list, if so do publish not insert
                                getDatabase().reconstruct(new TransferGroup.Assignee(assignee.groupId, assignee.deviceId));
                                doPublish = true;
                            } catch (Exception e) {
                            }

                            if (device instanceof NetworkDeviceListAdapter.HotspotNetwork
                                    && ((NetworkDeviceListAdapter.HotspotNetwork) device).qrConnection)
                                jsonRequest.put(Keyword.FLAG_TRANSFER_QR_CONNECTION, true);

                            jsonRequest.put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER);
                            jsonRequest.put(Keyword.TRANSFER_GROUP_ID, mGroup.groupId);

                            if (existingRegistry.size() == 0)
                                throw new Exception("Empty share holder id: " + mGroup.groupId);

                            JSONArray filesArray = new JSONArray();

                            for (TransferObject transferObject : existingRegistry) {
                                TransferObject copyObject = new TransferObject(AccessDatabase.convertValues(transferObject.getValues()));

                                if (getDefaultInterrupter().interrupted())
                                    throw new InterruptedException("Interrupted by user");

                                copyObject.deviceId = assignee.deviceId; // We will clone the file index with new deviceId
                                copyObject.flag = TransferObject.Flag.PENDING;
                                copyObject.accessPort = 0;
                                copyObject.skippedBytes = 0;
                                JSONObject thisJson = new JSONObject();

                                try {
                                    thisJson.put(Keyword.INDEX_FILE_NAME, copyObject.friendlyName);
                                    thisJson.put(Keyword.INDEX_FILE_SIZE, copyObject.fileSize);
                                    thisJson.put(Keyword.TRANSFER_REQUEST_ID, copyObject.requestId);
                                    thisJson.put(Keyword.INDEX_FILE_MIME, copyObject.fileMimeType);

                                    if (copyObject.directory != null)
                                        thisJson.put(Keyword.INDEX_DIRECTORY, copyObject.directory);

                                    filesArray.put(thisJson);
                                    pendingRegistry.add(copyObject);
                                } catch (Exception e) {
                                    Log.e(TAG, "Sender error on fileUri: " + e.getClass().getName() + " : " + copyObject.friendlyName);
                                }
                            }

                            // so that if the user rejects it won't be removed from the sender
                            jsonRequest.put(Keyword.FILES_INDEX, filesArray.toString());

                            getDefaultInterrupter().addCloser(new Interrupter.Closer()
                            {
                                @Override
                                public void onClose(boolean userAction)
                                {
                                    getDatabase().remove(assignee);
                                }
                            });

                            final CoolSocket.ActiveConnection activeConnection = client.communicate(device, connection);

                            getDefaultInterrupter().addCloser(new Interrupter.Closer()
                            {
                                @Override
                                public void onClose(boolean userAction)
                                {
                                    try {
                                        activeConnection.getSocket().close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                            activeConnection.reply(jsonRequest.toString());

                            CoolSocket.ActiveConnection.Response response = activeConnection.receive();
                            activeConnection.getSocket().close();

                            JSONObject clientResponse = new JSONObject(response.response);

                            if (clientResponse.has(Keyword.RESULT) && clientResponse.getBoolean(Keyword.RESULT)) {
                                updateText(thisTask, getString(R.string.mesg_organizingFiles));

                                if (doPublish)
                                    getDatabase().publish(assignee);
                                else
                                    getDatabase().insert(assignee);

                                if (doPublish)
                                    getDatabase().publish(pendingRegistry, progressUpdater);
                                else
                                    getDatabase().insert(pendingRegistry, progressUpdater);

                                setResult(RESULT_OK, new Intent()
                                        .putExtra(EXTRA_DEVICE_ID, assignee.deviceId)
                                        .putExtra(EXTRA_GROUP_ID, assignee.groupId));

                                finish();
                            } else {
                                if (clientResponse.has(Keyword.ERROR) && clientResponse.getString(Keyword.ERROR).equals(Keyword.ERROR_NOT_ALLOWED))
                                    createSnackbar(R.string.mesg_notAllowed)
                                            .setAction(R.string.ques_why, new View.OnClickListener()
                                            {
                                                @Override
                                                public void onClick(View v)
                                                {
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(AddDevicesToTransferActivity.this);

                                                    builder.setMessage(getString(R.string.text_notAllowedHelp,
                                                            device.nickname,
                                                            AppUtils.getLocalDeviceName(AddDevicesToTransferActivity.this)));

                                                    builder.setNegativeButton(R.string.butn_close, null);
                                                    builder.show();
                                                }
                                            }).show();
                                else
                                    createSnackbar(R.string.mesg_somethingWentWrong).show();
                            }
                        } catch (Exception e) {
                            if (!(e instanceof InterruptedException)) {
                                e.printStackTrace();
                                createSnackbar(R.string.mesg_fileSendError, getString(R.string.text_connectionProblem))
                                        .show();
                            }
                        } finally {
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    resetStatusViews();
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    @Override
    public Intent getIntent()
    {
        return super.getIntent();
    }

    public Interrupter getDefaultInterrupter()
    {
        return mInterrupter;
    }

    public void runOnWorkerService(WorkerService.RunningTask runningTask)
    {
        WorkerService.run(AddDevicesToTransferActivity.this, runningTask.setInterrupter(getDefaultInterrupter()));
    }

    protected void resetStatusViews()
    {
        mProgressBar.setMax(0);
        mProgressBar.setProgress(0);

        mTextMain.setText(R.string.text_addDevicesToTransfer);
        mActionButton.setText(R.string.butn_addDevices);
        mTextInfo.setVisibility(View.VISIBLE);
        mLayoutStatusContainer.setVisibility(View.GONE);
        mActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startConnectionManagerActivity();
            }
        });

        getDefaultInterrupter().reset(true);
    }

    protected void showChooserDialog(final NetworkDevice device)
    {
        device.isRestricted = false;
        getDatabase().publish(device);

        new ConnectionChooserDialog(AddDevicesToTransferActivity.this, device, new ConnectionChooserDialog.OnDeviceSelectedListener()
        {
            @Override
            public void onDeviceSelected(final NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> availableInterfaces)
            {
                doCommunicate(device, connection);
            }
        }, true).show();
    }

    private void startConnectionManagerActivity()
    {
        startActivityForResult(new Intent(AddDevicesToTransferActivity.this, ConnectionManagerActivity.class)
                .putExtra(ConnectionManagerActivity.EXTRA_ACTIVITY_SUBTITLE, getString(R.string.text_addDevicesToTransfer)), REQUEST_CODE_CHOOSE_DEVICE);
    }

    public void takeOnProcessMode()
    {
        getDefaultInterrupter().reset(true);

        mTextInfo.setVisibility(View.GONE);
        mLayoutStatusContainer.setVisibility(View.VISIBLE);
        mActionButton.setText(R.string.butn_cancel);
        mActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                getDefaultInterrupter().interrupt();
            }
        });
    }

    public void updateProgress(final int total, final int current)
    {
        if (isFinishing())
            return;

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mProgressTextLeft.setText(String.valueOf(current));
                mProgressTextRight.setText(String.valueOf(total));
            }
        });

        mProgressBar.setProgress(current);
        mProgressBar.setMax(total);
    }

    public void updateText(WorkerService.RunningTask runningTask, final String text)
    {
        if (isFinishing())
            return;

        runningTask.publishStatusText(text);

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mTextMain.setText(text);
            }
        });
    }

    public static void startInstance(Context context, long groupId)
    {
        context.startActivity(new Intent(context, AddDevicesToTransferActivity.class)
                .putExtra(EXTRA_GROUP_ID, groupId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}

