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
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.fragment.FileExplorerFragment;
import com.genonbeta.TrebleShot.ui.callback.PowerfulActionModeSupport;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.widget.PowerfulActionMode;

import java.io.FileNotFoundException;

public class FileExplorerActivity
        extends Activity
        implements PowerfulActionModeSupport
{
    public static final String EXTRA_FILE_PATH = "filePath";

    private PowerfulActionMode mActionMode;
    private FileExplorerFragment mFragmentFileExplorer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_file_explorer);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mActionMode = findViewById(R.id.activity_file_explorer_action_mode);
        mFragmentFileExplorer = (FileExplorerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.activity_file_explorer_fragment_files);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mActionMode.setOnSelectionTaskListener(new PowerfulActionMode.OnSelectionTaskListener()
        {
            @Override
            public void onSelectionTask(boolean started, PowerfulActionMode actionMode)
            {
                toolbar.setVisibility(!started ? View.VISIBLE : View.GONE);
            }
        });

        checkRequestedPath(getIntent());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home)
            finish();
        else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public void onBackPressed()
    {
        if (!mFragmentFileExplorer.onBackPressed()) {
            if (mActionMode.hasActive(mFragmentFileExplorer.getSelectionCallback()))
                mActionMode.finish(mFragmentFileExplorer.getSelectionCallback());
            else
                super.onBackPressed();
        }
    }

    public void checkRequestedPath(Intent intent)
    {
        if (intent == null)
            return;

        if (intent.hasExtra(EXTRA_FILE_PATH)) {
            Uri directoryUri = intent.getParcelableExtra(EXTRA_FILE_PATH);

            try {
                openFolder(FileUtils.fromUri(getApplicationContext(), directoryUri));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else
            openFolder(null);
    }

    @Override
    public PowerfulActionMode getPowerfulActionMode()
    {
        return mActionMode;
    }

    private void openFolder(@Nullable DocumentFile requestedFolder)
    {
        if (requestedFolder != null)
            mFragmentFileExplorer.requestPath(requestedFolder);
    }
}
