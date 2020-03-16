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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.task.OrganizeSharingRunningTask;
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class ShareActivity extends Activity implements SnackbarPlacementProvider, Activity.OnPreloadArgumentWatcher,
        BackgroundService.AttachedTaskListener
{
    public static final String TAG = "ShareActivity";

    public static final String
            ACTION_SEND = "genonbeta.intent.action.TREBLESHOT_SEND",
            ACTION_SEND_MULTIPLE = "genonbeta.intent.action.TREBLESHOT_SEND_MULTIPLE",
            EXTRA_DEVICE_ID = "extraDeviceId";

    private Bundle mPreLoadingBundle = new Bundle();
    private ProgressBar mProgressBar;
    private TextView mProgressTextLeft;
    private TextView mProgressTextRight;
    private TextView mTextMain;
    private List<Uri> mFileUris;
    private OrganizeSharingRunningTask mTask;

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
                List<Uri> fileUris = new ArrayList<>();

                if (ACTION_SEND_MULTIPLE.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                    List<Uri> pendingFileUris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    if (pendingFileUris != null)
                        fileUris.addAll(pendingFileUris);
                } else {
                    fileUris.add(getIntent().getParcelableExtra(Intent.EXTRA_STREAM));
                }

                if (fileUris.size() == 0) {
                    Toast.makeText(this, R.string.mesg_nothingToShare, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    mProgressBar = findViewById(R.id.progressBar);
                    mProgressTextLeft = findViewById(R.id.text1);
                    mProgressTextRight = findViewById(R.id.text2);
                    mTextMain = findViewById(R.id.textMain);

                    findViewById(R.id.cancelButton).setOnClickListener(v -> {
                        if (mTask != null)
                            mTask.interrupt(true);
                    });

                    mFileUris = fileUris;

                    checkForTasks();
                }
            }
        } else {
            Toast.makeText(this, R.string.mesg_formatNotSupported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onAttachedToTask(BackgroundService.BaseAttachableRunningTask task)
    {

    }

    @Override
    protected void onPreviousRunningTask(@Nullable BackgroundService.BaseAttachableRunningTask task)
    {
        super.onPreviousRunningTask(task);

        if (task instanceof OrganizeSharingRunningTask) {
            mTask = ((OrganizeSharingRunningTask) task);
            mTask.setAnchorListener(this);
        } else {
            mTask = new OrganizeSharingRunningTask(mFileUris);

            Log.d(TAG, "onPreviousRunningTask: Created new task");

            mTask.setAnchorListener(this)
                    .setTitle(getString(R.string.mesg_organizingFiles))
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

    @Override
    public void setTaskPosition(int ofTotal, int total)
    {
        if (isFinishing())
            return;

        runOnUiThread(() -> {
            mProgressTextLeft.setText(String.valueOf(ofTotal));
            mProgressTextRight.setText(String.valueOf(total));
        });

        mProgressBar.setProgress(total);
        mProgressBar.setMax(total);
    }

    @Override
    public void updateTaskPosition(int addToOfTotal, int addToTotal)
    {
        if (isFinishing())
            return;

        if (addToOfTotal != 0) {
            int newPosition = getProgressBar().getProgress() + addToOfTotal;
            runOnUiThread(() -> mProgressTextLeft.setText(String.valueOf(newPosition)));
            mProgressBar.setProgress(newPosition);
        }

        if (addToTotal != 0) {
            int newPosition = getProgressBar().getMax() + addToTotal;
            runOnUiThread(() -> mProgressTextRight.setText(String.valueOf(newPosition)));
            mProgressBar.setMax(newPosition);
        }
    }

    @Override
    public void updateTaskStatus(String text)
    {
        if (isFinishing())
            return;

        runOnUiThread(() -> mTextMain.setText(text));
    }
}

