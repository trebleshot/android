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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.io.ParcelableFile;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.task.OrganizeSharingRunningTask;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.object.Selectable;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ShareActivity extends Activity implements SnackbarSupport, Activity.OnPreloadArgumentWatcher,
        WorkerService.OnAttachListener
{
    public static final String TAG = "ShareActivity";

    public static final String
            ACTION_SEND = "genonbeta.intent.action.TREBLESHOT_SEND",
            ACTION_SEND_MULTIPLE = "genonbeta.intent.action.TREBLESHOT_SEND_MULTIPLE",
            EXTRA_FILENAME_LIST = "extraFileNames",
            EXTRA_DEVICE_ID = "extraDeviceId",
            EXTRA_GROUP_ID = "extraGroupId",
            EXTRA_FILE_CONTAINER = "extraFileContainer",
            EXTRA_FLAGS = "extraFlags";

    public static final int
            FLAG_WEBSHARE = 1,
            FLAG_LAUNCH_DEVICE_ADDING = 2;

    private Bundle mPreLoadingBundle = new Bundle();
    private ProgressBar mProgressBar;
    private TextView mProgressTextLeft;
    private TextView mProgressTextRight;
    private TextView mTextMain;
    private List<Uri> mFileUris;
    private ArrayList<FileContainer> mFileContainers;
    private List<CharSequence> mFileNames;
    private OrganizeSharingRunningTask mTask;

    public static void createFolderStructure(DocumentFile file, String folderName,
                                             List<SelectableStream> pendingObjects, OrganizeSharingRunningTask task)
    {
        DocumentFile[] files = file.listFiles();

        if (files != null) {

            if (task.getAnchorListener() != null)
                task.getAnchorListener().getProgressBar()
                        .setMax(task.getAnchorListener().getProgressBar().getMax() + files.length);

            for (DocumentFile thisFile : files) {
                if (task.getAnchorListener() != null)
                    task.getAnchorListener().getProgressBar()
                            .setProgress(task.getAnchorListener().getProgressBar().getProgress() + 1);

                if (task.getInterrupter().interrupted())
                    break;

                if (thisFile.isDirectory()) {
                    createFolderStructure(thisFile, (folderName != null ? folderName + File.separator
                            : null) + thisFile.getName(), pendingObjects, task);
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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        String action = getIntent() != null ? getIntent().getAction() : null;

        if (ACTION_SEND.equals(action) || ACTION_SEND_MULTIPLE.equals(action) || Intent.ACTION_SEND.equals(action)
                || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            if (getIntent().hasExtra(Intent.EXTRA_TEXT)) {
                startActivity(new Intent(ShareActivity.this, TextEditorActivity.class)
                        .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                        .putExtra(TextEditorActivity.EXTRA_TEXT_INDEX, getIntent().getStringExtra(Intent.EXTRA_TEXT)));
                finish();
            } else {
                ArrayList<Uri> fileUris = new ArrayList<>();
                ArrayList<CharSequence> fileNames = null;

                if (ACTION_SEND_MULTIPLE.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                    List<Uri> pendingFileUris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    fileNames = getIntent().hasExtra(EXTRA_FILENAME_LIST) ? getIntent().getCharSequenceArrayListExtra(
                            EXTRA_FILENAME_LIST) : null;

                    fileUris.addAll(pendingFileUris);
                } else {
                    fileUris.add(getIntent().getParcelableExtra(Intent.EXTRA_STREAM));

                    if (getIntent().hasExtra(EXTRA_FILENAME_LIST)) {
                        fileNames = new ArrayList<>();
                        String fileName = getIntent().getStringExtra(EXTRA_FILENAME_LIST);

                        fileNames.add(fileName);
                    }
                }

                if (fileUris.size() == 0) {
                    Toast.makeText(this, R.string.mesg_nothingToShare, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    mProgressBar = findViewById(R.id.progressBar);
                    mProgressTextLeft = findViewById(R.id.text1);
                    mProgressTextRight = findViewById(R.id.text2);
                    mTextMain = findViewById(R.id.textMain);

                    if (getIntent().hasExtra(EXTRA_FILE_CONTAINER))
                        mFileContainers = getIntent().getParcelableArrayListExtra(EXTRA_FILE_CONTAINER);

                    findViewById(R.id.cancelButton).setOnClickListener(v -> {
                        if (mTask != null)
                            mTask.getInterrupter().interrupt(true);
                    });

                    mFileUris = fileUris;
                    mFileNames = fileNames;

                    checkForTasks();
                }
            }
        } else {
            Toast.makeText(this, R.string.mesg_formatNotSupported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onAttachedToTask(WorkerService.RunningTask task)
    {

    }

    @Override
    protected void onPreviousRunningTask(@Nullable WorkerService.RunningTask task)
    {
        super.onPreviousRunningTask(task);

        if (task instanceof OrganizeSharingRunningTask) {
            mTask = ((OrganizeSharingRunningTask) task);
            mTask.setAnchorListener(this);
        } else {
            mTask = new OrganizeSharingRunningTask(mFileUris, mFileNames, getIntent(), mFileContainers);

            mTask.setTitle(getString(R.string.mesg_organizingFiles))
                    .setAnchorListener(this)
                    .setContentIntent(this, getIntent())
                    .run(this);

            attachRunningTask(mTask);
        }
    }

    public Snackbar createSnackbar(int resId, Object... objects)
    {
        return Snackbar.make(getWindow().getDecorView(), getString(resId, objects), Snackbar.LENGTH_LONG);
    }

    public ProgressBar getProgressBar()
    {
        return mProgressBar;
    }

    @Override
    public Bundle passPreLoadingArguments()
    {
        return mPreLoadingBundle;
    }

    public void updateProgress(final int total, final int current)
    {
        if (isFinishing())
            return;

        runOnUiThread(() -> {
            mProgressTextLeft.setText(String.valueOf(current));
            mProgressTextRight.setText(String.valueOf(total));
        });

        mProgressBar.setProgress(current);
        mProgressBar.setMax(total);
    }

    public void updateText(WorkerService.RunningTask runningTask, final String text)
    {
        if (isFinishing())
            return;

        runningTask.publishStatusText(text);

        runOnUiThread(() -> mTextMain.setText(text));
    }

    public static class SelectableStream implements Selectable
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

    public static class FileContainer implements Serializable, Parcelable
    {
        public static final Creator<FileContainer> CREATOR = new Creator<FileContainer>()
        {
            @Override
            public FileContainer createFromParcel(Parcel source)
            {
                return new FileContainer(source);
            }

            @Override
            public FileContainer[] newArray(int size)
            {
                return new FileContainer[size];
            }
        };

        public String targetUri;
        public List<ParcelableFile> files;

        public FileContainer(Parcel in) {
            files = new ArrayList<>();
            targetUri = in.readString();

            in.readTypedList(files, ParcelableFile.CREATOR);
        }

        public FileContainer(String targetUri, List<ParcelableFile> files)
        {
            this.targetUri = targetUri;
            this.files = files;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            ParcelableFile[] fileArray = new ParcelableFile[files.size()];

            files.toArray(fileArray);
            dest.writeString(targetUri);
            dest.writeParcelableArray(fileArray, flags);
        }
    }
}

