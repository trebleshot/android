package com.genonbeta.TrebleShot.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.object.Selectable;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.genonbeta.android.framework.util.Interrupter;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Locale;

public class ShareActivity extends Activity
        implements SnackbarSupport, Activity.OnPreloadArgumentWatcher
{
    public static final String TAG = "ShareActivity";

    public static final int WORKER_TASK_LOAD_ITEMS = 1;
    public static final int WORKER_TASK_CONNECT_SERVER = 2;

    public static final int REQUEST_CODE_EDIT_BOX = 1;

    public static final String ACTION_SEND = "genonbeta.intent.action.TREBLESHOT_SEND";
    public static final String ACTION_SEND_MULTIPLE = "genonbeta.intent.action.TREBLESHOT_SEND_MULTIPLE";

    public static final String EXTRA_FILENAME_LIST = "extraFileNames";
    public static final String EXTRA_DEVICE_ID = "extraDeviceId";
    public static final String EXTRA_GROUP_ID = "extraGroupId";

    private Interrupter mInterrupter = new Interrupter();
    private WorkerService mWorkerService;
    private WorkerConnection mWorkerConnection = new WorkerConnection();
    private Bundle mPreLoadingBundle = new Bundle();
    private Button mCancelButton;
    private ProgressBar mProgressBar;
    private TextView mProgressTextLeft;
    private TextView mProgressTextRight;
    private TextView mTextMain;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        bindService(new Intent(this, WorkerService.class), mWorkerConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        getDefaultInterrupter().interrupt(false);
        unbindService(mWorkerConnection);
    }

    protected void createFolderStructure(DocumentFile file, String folderName, ArrayList<SelectableStream> pendingObjects)
    {
        DocumentFile[] files = file.listFiles();

        if (files != null) {
            mProgressBar.setMax(mProgressBar.getMax() + files.length);

            for (DocumentFile thisFile : files) {
                mProgressBar.setProgress(mProgressBar.getProgress() + 1);

                if (getDefaultInterrupter().interrupted())
                    break;

                if (thisFile.isDirectory()) {
                    createFolderStructure(thisFile, (folderName != null ? folderName + File.separator : null) + thisFile.getName(), pendingObjects);
                    continue;
                }

                try {
                    pendingObjects.add(new SelectableStream(thisFile, folderName));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Snackbar createSnackbar(int resId, Object... objects)
    {
        return Snackbar.make(getWindow().getDecorView(), getString(resId, objects), Snackbar.LENGTH_LONG);
    }

    public Interrupter getDefaultInterrupter()
    {
        return mInterrupter;
    }

    private void initialize()
    {
        String action = getIntent() != null ? getIntent().getAction() : null;

        if (action == null) {
            Toast.makeText(this, R.string.mesg_formatNotSupported, Toast.LENGTH_SHORT).show();
            finish();
        } else if (ACTION_SEND.equals(action)
                || ACTION_SEND_MULTIPLE.equals(action)
                || Intent.ACTION_SEND.equals(action)
                || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> fileUris = new ArrayList<>();
            ArrayList<CharSequence> fileNames = null;

            if (ACTION_SEND_MULTIPLE.equals(action)
                    || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                ArrayList<Uri> pendingFileUris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                fileNames = getIntent().hasExtra(EXTRA_FILENAME_LIST) ? getIntent().getCharSequenceArrayListExtra(EXTRA_FILENAME_LIST) : null;

                fileUris.addAll(pendingFileUris);
            } else {
                fileUris.add((Uri) getIntent().getParcelableExtra(Intent.EXTRA_STREAM));

                if (getIntent().hasExtra(EXTRA_FILENAME_LIST)) {
                    fileNames = new ArrayList<>();
                    String fileName = getIntent().getStringExtra(EXTRA_FILENAME_LIST);

                    fileNames.add(fileName);
                }
            }


            if (fileUris.size() == 0) {
                Toast.makeText(this, R.string.text_listEmpty, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                setContentView(R.layout.activity_share);

                mProgressBar = findViewById(R.id.progressBar);
                mProgressTextLeft = findViewById(R.id.text1);
                mProgressTextRight = findViewById(R.id.text2);
                mTextMain = findViewById(R.id.textMain);
                mCancelButton = findViewById(R.id.cancelButton);

                mCancelButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        getDefaultInterrupter().interrupt(true);
                    }
                });

                organizeFiles(fileUris, fileNames);
            }
        }
    }

    protected void organizeFiles(final ArrayList<Uri> fileUris, final ArrayList<CharSequence> fileNames)
    {
        runOnWorkerService(new WorkerService.RunningTask(TAG, WORKER_TASK_LOAD_ITEMS)
        {
            @Override
            public void onRun()
            {
                mProgressBar.setMax(fileUris.size());
                mTextMain.setText(R.string.mesg_organizingFiles);
                publishStatusText(getString(R.string.mesg_organizingFiles));

                final NetworkDevice localDevice = AppUtils.getLocalDevice(ShareActivity.this);
                final ArrayList<SelectableStream> measuredObjects = new ArrayList<>();
                final ArrayList<TransferObject> pendingObjects = new ArrayList<>();
                final TransferGroup groupInstance = new TransferGroup(AppUtils.getUniqueNumber());
                final TransferGroup.Assignee assignee = new TransferGroup.Assignee(groupInstance.groupId, localDevice.deviceId, Keyword.Local.NETWORK_INTERFACE_UNKNOWN);

                for (int position = 0; position < fileUris.size(); position++) {
                    if (getDefaultInterrupter().interrupted())
                        break;

                    mProgressBar.setProgress(mProgressBar.getProgress() + 1);

                    publishStatusText(String.format(Locale.getDefault(), "%s - %d", getString(R.string.mesg_organizingFiles), pendingObjects.size()));

                    Uri fileUri = fileUris.get(position);
                    String fileName = fileNames != null ? String.valueOf(fileNames.get(position)) : null;

                    try {
                        SelectableStream selectableStream = new SelectableStream(ShareActivity.this, fileUri, null);

                        if (selectableStream.getDocumentFile().isDirectory())
                            createFolderStructure(selectableStream.getDocumentFile(), selectableStream.getDocumentFile().getName(), measuredObjects);
                        else {
                            if (fileName != null)
                                selectableStream.setFriendlyName(fileName);

                            measuredObjects.add(selectableStream);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }

                for (SelectableStream selectableStream : measuredObjects) {
                    if (getDefaultInterrupter().interrupted())
                        break;

                    long requestId = AppUtils.getUniqueNumber();

                    TransferObject transferObject = new TransferObject(requestId,
                            groupInstance.groupId,
                            assignee.deviceId,
                            selectableStream.getSelectableTitle(),
                            selectableStream.getDocumentFile().getUri().toString(),
                            selectableStream.getDocumentFile().getType(),
                            selectableStream.getDocumentFile().length(), TransferObject.Type.OUTGOING);

                    if (selectableStream.mDirectory != null)
                        transferObject.directory = selectableStream.mDirectory;

                    pendingObjects.add(transferObject);
                }

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mTextMain.setText(R.string.mesg_completing);
                    }
                });

                mProgressBar.setMax(pendingObjects.size());

                getDatabase().insert(pendingObjects, new SQLiteDatabase.ProgressUpdater()
                {
                    @Override
                    public void onProgressChange(int total, int current)
                    {
                        mProgressBar.setProgress(current);
                    }

                    @Override
                    public boolean onProgressState()
                    {
                        return !getDefaultInterrupter().interrupted();
                    }
                });

                if (getDefaultInterrupter().interrupted()) {
                    getDatabase().remove(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                            .setWhere(String.format("%s = ?", AccessDatabase.FIELD_TRANSFER_GROUPID),
                                    String.valueOf(groupInstance.groupId)));

                    finish();
                } else {
                    getDatabase().insert(groupInstance);
                    getDatabase().insert(assignee);
                    getDatabase().publish(localDevice);

                    ViewTransferActivity.startInstance(ShareActivity.this, groupInstance.groupId);
                    AddDevicesToTransferActivity.startInstance(ShareActivity.this, groupInstance.groupId, true);

                    finish();
                }
            }
        });
    }

    @Override
    public Bundle passPreLoadingArguments()
    {
        return mPreLoadingBundle;
    }

    public boolean runOnWorkerService(WorkerService.RunningTask runningTask)
    {
        if (mWorkerService == null)
            return false;

        mWorkerService.run(runningTask.setInterrupter(getDefaultInterrupter()));

        return true;
    }

    private class WorkerConnection implements ServiceConnection
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            mWorkerService = ((WorkerService.LocalBinder) service).getService();
            initialize();
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            finish();
        }
    }

    private class SelectableStream implements Selectable
    {
        private String mDirectory;
        private String mFriendlyName;
        private DocumentFile mFile;
        private boolean mSelected = true;

        public SelectableStream(DocumentFile documentFile, String directory)
        {
            mFile = documentFile;
            mDirectory = directory;
            mFriendlyName = mFile.getName();
        }

        public SelectableStream(Context context, Uri uri, String directory) throws FileNotFoundException
        {
            this(FileUtils.fromUri(context, uri), directory);
        }

        public String getDirectory()
        {
            return mDirectory;
        }

        public DocumentFile getDocumentFile()
        {
            return mFile;
        }

        @Override
        public String getSelectableTitle()
        {
            return mFriendlyName;
        }

        @Override
        public boolean isSelectableSelected()
        {
            return mSelected;
        }

        public void setFriendlyName(String friendlyName)
        {
            mFriendlyName = friendlyName;
        }

        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            mSelected = selected;
            return true;
        }
    }
}

