package com.genonbeta.TrebleShot.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.database.AccessDatabase;
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
import java.util.List;
import java.util.Locale;

public class ShareActivity extends Activity
        implements SnackbarSupport, Activity.OnPreloadArgumentWatcher
{
    public static final String TAG = "ShareActivity";

    public static final int WORKER_TASK_LOAD_ITEMS = 1;

    public static final String ACTION_SEND = "genonbeta.intent.action.TREBLESHOT_SEND";
    public static final String ACTION_SEND_MULTIPLE = "genonbeta.intent.action.TREBLESHOT_SEND_MULTIPLE";

    public static final String EXTRA_FILENAME_LIST = "extraFileNames";
    public static final String EXTRA_DEVICE_ID = "extraDeviceId";
    public static final String EXTRA_GROUP_ID = "extraGroupId";

    private Interrupter mInterrupter = new Interrupter();
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
        setContentView(R.layout.activity_share);

        String action = getIntent() != null ? getIntent().getAction() : null;

        if (ACTION_SEND.equals(action)
                || ACTION_SEND_MULTIPLE.equals(action)
                || Intent.ACTION_SEND.equals(action)
                || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            if (getIntent().hasExtra(Intent.EXTRA_TEXT)) {
                startActivity(new Intent(ShareActivity.this, TextEditorActivity.class)
                        .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                        .putExtra(TextEditorActivity.EXTRA_TEXT_INDEX, getIntent().getStringExtra(Intent.EXTRA_TEXT)));
                finish();
            } else {
                ArrayList<Uri> fileUris = new ArrayList<>();
                ArrayList<CharSequence> fileNames = null;

                if (ACTION_SEND_MULTIPLE.equals(action)
                        || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                    List<Uri> pendingFileUris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
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
        } else {
            Toast.makeText(this, R.string.mesg_formatNotSupported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        getDefaultInterrupter().interrupt(false);
    }

    protected void createFolderStructure(DocumentFile file, String folderName, List<SelectableStream> pendingObjects)
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

    protected void organizeFiles(final List<Uri> fileUris, final List<CharSequence> fileNames)
    {
        runOnWorkerService(new WorkerService.RunningTask(TAG, WORKER_TASK_LOAD_ITEMS)
        {
            @Override
            public void onRun()
            {
                final WorkerService.RunningTask thisTask = this;
                mProgressBar.setMax(fileUris.size());

                updateText(thisTask, getString(R.string.mesg_organizingFiles));

                final List<SelectableStream> measuredObjects = new ArrayList<>();
                final List<TransferObject> pendingObjects = new ArrayList<>();
                final TransferGroup groupInstance = new TransferGroup(AppUtils.getUniqueNumber());

                for (int position = 0; position < fileUris.size(); position++) {
                    if (getDefaultInterrupter().interrupted())
                        break;

                    updateProgress(mProgressBar.getMax(), mProgressBar.getProgress() + 1);
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
                            selectableStream.getSelectableTitle(),
                            selectableStream.getDocumentFile().getUri().toString(),
                            selectableStream.getDocumentFile().getType(),
                            selectableStream.getDocumentFile().length(), TransferObject.Type.OUTGOING);

                    if (selectableStream.mDirectory != null)
                        transferObject.directory = selectableStream.mDirectory;

                    pendingObjects.add(transferObject);
                }

                updateText(thisTask, getString(R.string.mesg_completing));

                getDatabase().insert(pendingObjects, new SQLiteDatabase.ProgressUpdater()
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
                });

                if (getDefaultInterrupter().interrupted()) {
                    getDatabase().remove(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                            .setWhere(String.format("%s = ?", AccessDatabase.FIELD_TRANSFER_GROUPID),
                                    String.valueOf(groupInstance.groupId)));

                    finish();
                } else {
                    getDatabase().insert(groupInstance);

                    ViewTransferActivity.startInstance(ShareActivity.this, groupInstance.groupId);
                    AddDevicesToTransferActivity.startInstance(ShareActivity.this, groupInstance.groupId);

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

    public void runOnWorkerService(WorkerService.RunningTask runningTask)
    {
        getDefaultInterrupter().reset(true);
        WorkerService.run(ShareActivity.this, runningTask.setInterrupter(getDefaultInterrupter()));
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

