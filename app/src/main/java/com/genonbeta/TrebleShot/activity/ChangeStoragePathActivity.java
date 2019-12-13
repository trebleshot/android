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
import android.widget.Toast;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.android.framework.io.DocumentFile;

/**
 * Created by: veli
 * Date: 5/30/17 6:57 PM
 */

public class ChangeStoragePathActivity extends Activity
{
    public final static int REQUEST_CHOOSE_FOLDER = 1;

    @Override
    protected void onStart()
    {
        super.onStart();

        DocumentFile currentSavePath = FileUtils.getApplicationDirectory(getApplicationContext());

        startActivityForResult(new Intent(this, FilePickerActivity.class)
                .setAction(FilePickerActivity.ACTION_CHOOSE_DIRECTORY)
                .putExtra(FilePickerActivity.EXTRA_START_PATH, currentSavePath.getUri().toString())
                .putExtra(FilePickerActivity.EXTRA_ACTIVITY_TITLE, getString(R.string.text_storagePath)), REQUEST_CHOOSE_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null) {
            if (resultCode == Activity.RESULT_OK) {
                switch (requestCode) {
                    case REQUEST_CHOOSE_FOLDER:
                        if (data.hasExtra(FilePickerActivity.EXTRA_CHOSEN_PATH)) {
                            getDefaultPreferences()
                                    .edit()
                                    .putString("storage_path", data.getParcelableExtra(FilePickerActivity.EXTRA_CHOSEN_PATH).toString())
                                    .apply();

                            Toast.makeText(this, "\uD83D\uDC4D", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        }

        finish();
    }
}
