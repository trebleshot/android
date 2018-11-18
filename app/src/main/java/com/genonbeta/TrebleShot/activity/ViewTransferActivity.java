package com.genonbeta.TrebleShot.activity;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter;
import com.genonbeta.TrebleShot.adapter.TransferAssigneeListAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.PauseMultipleTransferDialog;
import com.genonbeta.TrebleShot.dialog.TransferInfoDialog;
import com.genonbeta.TrebleShot.fragment.TransferListFragment;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.ui.callback.PowerfulActionModeSupport;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.android.framework.io.StreamInfo;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.genonbeta.android.framework.widget.PowerfulActionMode;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by: veli
 * Date: 5/23/17 1:43 PM
 */

public class ViewTransferActivity
        extends Activity
        implements PowerfulActionModeSupport
{
    public static final String TAG = ViewTransferActivity.class.getSimpleName();

    public static final String ACTION_LIST_TRANSFERS = "com.genonbeta.TrebleShot.action.LIST_TRANSFERS";
    public static final String EXTRA_GROUP_ID = "extraGroupId";

    public static final int REQUEST_ADD_DEVICES = 5045;

    private OnBackPressedListener mBackPressedListener;
    private TransferGroup mGroup;
    private TransferObject mTransferObject;

    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction()) && intent.hasExtra(AccessDatabase.EXTRA_TABLE_NAME)) {
                if (AccessDatabase.TABLE_TRANSFERGROUP.equals(intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME)))
                    reconstructGroup();
                else if (intent.hasExtra(AccessDatabase.EXTRA_CHANGE_TYPE)
                        && AccessDatabase.TABLE_TRANSFER.equals(intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME))
                        && (AccessDatabase.TYPE_INSERT.equals(intent.getStringExtra(AccessDatabase.EXTRA_CHANGE_TYPE)) || AccessDatabase.TYPE_REMOVE.equals(intent.getStringExtra(AccessDatabase.EXTRA_CHANGE_TYPE)))) {
                    updateCalculations();
                }
            } else if (CommunicationService.ACTION_TASK_STATUS_CHANGE.equals(intent.getAction())
                    && intent.hasExtra(CommunicationService.EXTRA_GROUP_ID)
                    && intent.hasExtra(CommunicationService.EXTRA_DEVICE_ID)) {
                long groupId = intent.getLongExtra(CommunicationService.EXTRA_GROUP_ID, -1);

                if (groupId == mGroup.groupId) {
                    String deviceId = intent.getStringExtra(CommunicationService.EXTRA_DEVICE_ID);
                    int taskChange = intent.getIntExtra(CommunicationService.EXTRA_TASK_CHANGE_TYPE, -1);

                    synchronized (mActiveProcesses) {
                        if (taskChange == CommunicationService.TASK_STATUS_ONGOING)
                                mActiveProcesses.add(deviceId);
                        else
                            mActiveProcesses.remove(deviceId);
                    }

                    showMenus();
                }
            } else if (CommunicationService.ACTION_TASK_RUNNING_LIST_CHANGE.equals(intent.getAction())) {
                long[] groupIds = intent.getLongArrayExtra(CommunicationService.EXTRA_TASK_LIST_RUNNING);
                ArrayList<String> deviceIds = intent.getStringArrayListExtra(CommunicationService.EXTRA_DEVICE_LIST_RUNNING);

                if (groupIds != null && deviceIds != null
                        && groupIds.length == deviceIds.size()) {
                    int iterator = 0;

                    synchronized (mActiveProcesses) {
                        mActiveProcesses.clear();

                        for (long groupId : groupIds) {
                            String deviceId = deviceIds.get(iterator++);

                            if (groupId == mGroup.groupId)
                                mActiveProcesses.add(deviceId);
                        }

                        showMenus();
                    }
                }
            }
        }
    };

    final private List<String> mActiveProcesses = new ArrayList<>();
    final private TransferGroup.Index mTransactionIndex = new TransferGroup.Index();

    private PowerfulActionMode mMode;
    private MenuItem mStartMenu;
    private MenuItem mRetryMenu;
    private MenuItem mShowFiles;
    private MenuItem mAddDevice;
    private CrunchLatestDataTask mDataCruncher;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_view_transfer);

        mMode = findViewById(R.id.activity_transaction_action_mode);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (Intent.ACTION_VIEW.equals(getIntent().getAction()) && getIntent().getData() != null) {
            try {
                StreamInfo streamInfo = StreamInfo.getStreamInfo(this, getIntent().getData());

                Log.d(TAG, "Requested file is: " + streamInfo.friendlyName);

                CursorItem fileIndex = getDatabase().getFirstFromTable(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                        .setWhere(AccessDatabase.FIELD_TRANSFER_FILE + "=?", streamInfo.friendlyName));

                if (fileIndex == null)
                    throw new Exception("File is not found in the database");

                TransferObject transferObject = new TransferObject(fileIndex);
                TransferGroup transferGroup = new TransferGroup(transferObject.groupId);

                getDatabase().reconstruct(transferObject);

                mGroup = transferGroup;
                mTransferObject = transferObject;

                if (getIntent().getExtras() != null)
                    getIntent().getExtras().clear();

                getIntent()
                        .setAction(ACTION_LIST_TRANSFERS)
                        .putExtra(EXTRA_GROUP_ID, mGroup.groupId);

                new TransferInfoDialog(ViewTransferActivity.this, transferObject)
                        .show();

                Log.d(TAG, "Created instance from an file intent. Original has been cleaned " +
                        "and changed to open intent");
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.mesg_notValidTransfer, Toast.LENGTH_SHORT).show();
            }
        } else if (ACTION_LIST_TRANSFERS.equals(getIntent().getAction()) && getIntent().hasExtra(EXTRA_GROUP_ID)) {
            TransferGroup group = new TransferGroup(getIntent().getLongExtra(EXTRA_GROUP_ID, -1));

            try {
                getDatabase().reconstruct(group);
                mGroup = group;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mGroup == null)
            finish();
        else {
            Bundle transactionFragmentArgs = new Bundle();
            transactionFragmentArgs.putLong(TransferFileExplorerFragment.ARG_GROUP_ID, mGroup.groupId);
            transactionFragmentArgs.putString(TransferFileExplorerFragment.ARG_PATH,
                    mTransferObject == null || mTransferObject.directory == null
                            ? null : mTransferObject.directory);

            TransferFileExplorerFragment fragment = (TransferFileExplorerFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.activity_transaction_content_frame);

            if (fragment == null) {
                fragment = (TransferFileExplorerFragment) Fragment
                        .instantiate(ViewTransferActivity.this, TransferFileExplorerFragment.class.getName(), transactionFragmentArgs);

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                transaction.add(R.id.activity_transaction_content_frame, fragment);
                transaction.commit();
            }

            attachListeners(fragment);

            mMode.setOnSelectionTaskListener(new PowerfulActionMode.OnSelectionTaskListener()
            {
                @Override
                public void onSelectionTask(boolean started, PowerfulActionMode actionMode)
                {
                    toolbar.setVisibility(!started ? View.VISIBLE : View.GONE);
                }
            });
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        IntentFilter filter = new IntentFilter();

        filter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);
        filter.addAction(CommunicationService.ACTION_TASK_STATUS_CHANGE);
        filter.addAction(CommunicationService.ACTION_TASK_RUNNING_LIST_CHANGE);

        registerReceiver(mReceiver, filter);
        reconstructGroup();

        requestTaskStateUpdate();
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

        mStartMenu = menu.findItem(R.id.actions_transfer_resume);
        mRetryMenu = menu.findItem(R.id.actions_transfer_retry_all);
        mShowFiles = menu.findItem(R.id.actions_transfer_show_files);
        mAddDevice = menu.findItem(R.id.actions_transfer_add_device);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        showMenus();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        } else if (id == R.id.actions_transfer_resume) {
            toggleTask();
        } else if (id == R.id.actions_transfer_remove) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);

            dialog.setTitle(R.string.ques_removeAll);
            dialog.setMessage(R.string.text_removeCertainPendingTransfersSummary);
            dialog.setNegativeButton(R.string.butn_cancel, null);
            dialog.setPositiveButton(R.string.butn_removeAll, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    getDatabase().remove(mGroup);
                }
            });

            dialog.show();
        } else if (id == R.id.actions_transfer_retry_all) {
            ContentValues contentValues = new ContentValues();

            contentValues.put(AccessDatabase.FIELD_TRANSFER_FLAG, TransferObject.Flag.PENDING.toString());

            getDatabase().update(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                    .setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
                                    + AccessDatabase.FIELD_TRANSFER_FLAG + "=? AND "
                                    + AccessDatabase.FIELD_TRANSFER_TYPE + "=?",
                            String.valueOf(mGroup.groupId),
                            TransferObject.Flag.INTERRUPTED.toString(),
                            TransferObject.Type.INCOMING.toString()), contentValues);

            createSnackbar(R.string.mesg_retryAllInfo)
                    .show();
        } else if (id == R.id.actions_transfer_show_files) {
            startActivity(new Intent(this, FileExplorerActivity.class)
                    .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, FileUtils.getSavePath(this, getDefaultPreferences(), mGroup).getUri()));
        } else if (id == R.id.actions_transfer_add_device) {
            startActivityForResult(new Intent(this, AddDevicesToTransferActivity.class)
                    .putExtra(ShareActivity.EXTRA_GROUP_ID, mGroup.groupId), REQUEST_ADD_DEVICES);
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
        mBackPressedListener = initiatedItem instanceof OnBackPressedListener
                ? (OnBackPressedListener) initiatedItem
                : null;
    }

    private Snackbar createSnackbar(int resId, Object... objects)
    {
        return Snackbar.make(findViewById(R.id.activity_transaction_content_frame), getString(resId, objects), Snackbar.LENGTH_LONG);
    }

    @Nullable
    public TransferGroup getGroup()
    {
        return mGroup;
    }

    public TransferGroup.Index getIndex()
    {
        return mTransactionIndex;
    }

    @Override
    public PowerfulActionMode getPowerfulActionMode()
    {
        return mMode;
    }

    public void reconstructGroup()
    {
        try {
            if (mGroup != null)
                getDatabase().reconstruct(mGroup);
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    private void requestTaskStateUpdate()
    {
        if (mGroup != null)
            AppUtils.startForegroundService(this, new Intent(this, CommunicationService.class)
                    .setAction(CommunicationService.ACTION_REQUEST_TASK_RUNNING_LIST_CHANGE));
    }

    private void showMenus()
    {
        boolean hasIncoming = getIndex().incomingCount > 0;
        boolean hasOutgoing = getIndex().outgoingCount > 0;
        boolean hasRunning = mActiveProcesses.size() > 0;

        if (mStartMenu == null || mRetryMenu == null || mShowFiles == null)
            return;

        mStartMenu.setTitle(hasRunning ? R.string.butn_pause : R.string.butn_start);

        // Only show when there
        mAddDevice.setVisible(hasOutgoing);
        mStartMenu.setVisible(hasIncoming || hasRunning);
        mRetryMenu.setVisible(hasIncoming);
        mShowFiles.setVisible(hasIncoming);

        setTitle(getResources().getQuantityString(R.plurals.text_files,
                getIndex().incomingCount + getIndex().outgoingCount,
                getIndex().incomingCount + getIndex().outgoingCount));
    }

    private void toggleTask()
    {
        if (mActiveProcesses.size() > 0) {
            if (mActiveProcesses.size() == 1)
                TransferUtils.pauseTransfer(this, mGroup.groupId, mActiveProcesses.get(0));
            else
                new PauseMultipleTransferDialog(ViewTransferActivity.this, mGroup, mActiveProcesses)
                        .show();
        } else {
            SQLQuery.Select select = new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
                    .setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(mGroup.groupId));

            ArrayList<TransferAssigneeListAdapter.ShowingAssignee> assignees = getDatabase()
                    .castQuery(select, TransferAssigneeListAdapter.ShowingAssignee.class, new SQLiteDatabase.CastQueryListener<TransferAssigneeListAdapter.ShowingAssignee>()
            {
                @Override
                public void onObjectReconstructed(SQLiteDatabase db, CursorItem item, TransferAssigneeListAdapter.ShowingAssignee object)
                {
                    object.device = new NetworkDevice(object.deviceId);
                    object.connection = new NetworkDevice.Connection(object);

                    try {
                        db.reconstruct(object.device);
                        db.reconstruct(object.connection);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            if (assignees.size() == 0) {
                createSnackbar(R.string.mesg_noReceiverOrSender)
                        .show();
                return;
            }

            final TransferAssigneeListAdapter.ShowingAssignee assignee = assignees.get(0);

            try {
                getDatabase().reconstruct(new NetworkDevice.Connection(assignee));
                TransferUtils.startTransfer(this, mGroup, assignee);
            } catch (Exception e) {
                e.printStackTrace();

                createSnackbar(R.string.mesg_transferConnectionNotSetUpFix)
                        .setAction(R.string.butn_setUp, new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                TransferUtils.changeConnection(ViewTransferActivity.this, getDatabase(), mGroup, assignee.device, new TransferUtils.ConnectionUpdatedListener()
                                {
                                    @Override
                                    public void onConnectionUpdated(NetworkDevice.Connection connection, TransferGroup.Assignee assignee)
                                    {
                                        createSnackbar(R.string.mesg_connectionUpdated, TextUtils.getAdapterName(getApplicationContext(), connection))
                                                .show();
                                    }
                                });
                            }
                        }).show();
            }
        }
    }

    public synchronized void updateCalculations()
    {
        if (mDataCruncher == null || !mDataCruncher.requestRestart()) {
            mDataCruncher = new CrunchLatestDataTask(new CrunchLatestDataTask.PostExecuteListener()
            {
                @Override
                public void onPostExecute()
                {
                    showMenus();
                    findViewById(R.id.activity_transaction_no_devices_warning)
                            .setVisibility(mTransactionIndex.assigneeCount > 0 ? View.GONE : View.VISIBLE);

                    if (mTransactionIndex.assigneeCount == 0)

                    if (mTransferObject != null) {
                        new TransferInfoDialog(ViewTransferActivity.this, mTransferObject).show();
                        mTransferObject = null;
                    }
                }
            });

            mDataCruncher.execute(this);
        }
    }

    public static void startInstance(Context context, long groupId)
    {
        context.startActivity(new Intent(context, ViewTransferActivity.class)
                .setAction(ACTION_LIST_TRANSFERS)
                .putExtra(EXTRA_GROUP_ID, groupId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private static class TransferPathResolverRecyclerAdapter extends PathResolverRecyclerAdapter<String>
    {
        public TransferPathResolverRecyclerAdapter(Context context)
        {
            super(context);
        }

        public void goTo(String[] paths)
        {
            getList().clear();

            StringBuilder mergedPath = new StringBuilder();

            getList().add(new Holder.Index<>(getContext().getString(R.string.text_home), R.drawable.ic_home_white_24dp, (String) null));

            if (paths != null)
                for (String path : paths) {
                    if (path.length() == 0)
                        continue;

                    if (mergedPath.length() > 0)
                        mergedPath.append(File.separator);

                    mergedPath.append(path);

                    getList().add(new Holder.Index<>(path, mergedPath.toString()));
                }
        }
    }

    public static class TransferFileExplorerFragment
            extends TransferListFragment
            implements TitleSupport, SnackbarSupport
    {
        private RecyclerView mPathView;
        private TransferPathResolverRecyclerAdapter mPathAdapter;

        @Override
        protected RecyclerView onListView(View mainContainer, ViewGroup listViewContainer)
        {
            View adaptedView = getLayoutInflater().inflate(R.layout.layout_transfer_explorer, null, false);
            listViewContainer.addView(adaptedView);

            mPathView = adaptedView.findViewById(R.id.layout_transfer_explorer_recycler);
            mPathAdapter = new TransferPathResolverRecyclerAdapter(getContext());

            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
            layoutManager.setStackFromEnd(true);

            mPathView.setHasFixedSize(true);
            mPathView.setLayoutManager(layoutManager);
            mPathView.setAdapter(mPathAdapter);

            mPathAdapter.setOnClickListener(new PathResolverRecyclerAdapter.OnClickListener<String>()
            {
                @Override
                public void onClick(PathResolverRecyclerAdapter.Holder<String> holder)
                {
                    goPath(getAdapter().getGroupId(), holder.index.object);
                }
            });

            return super.onListView(mainContainer, (ViewGroup) adaptedView.findViewById(R.id.layout_transfer_explorer_fragment_content));
        }

        @Override
        protected void onListRefreshed()
        {
            super.onListRefreshed();

            String path = getAdapter().getPath();

            mPathAdapter.goTo(path == null ? null : path.split(File.separator));
            mPathAdapter.notifyDataSetChanged();

            if (mPathAdapter.getItemCount() > 0)
                mPathView.smoothScrollToPosition(mPathAdapter.getItemCount() - 1);
        }

        @Override
        public CharSequence getTitle(Context context)
        {
            return context.getString(R.string.text_files);
        }
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

                for (ViewTransferActivity activity : activities) {
                    if (activity.getGroup() != null)
                        activity.getDatabase()
                                .calculateTransactionSize(activity.getGroup().groupId, activity.getIndex());
                }
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
