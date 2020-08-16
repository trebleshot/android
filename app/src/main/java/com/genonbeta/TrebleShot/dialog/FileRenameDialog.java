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

package com.genonbeta.TrebleShot.dialog;

import android.app.Activity;
import com.genonbeta.TrebleShot.App;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.task.RenameMultipleFilesTask;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.android.framework.io.DocumentFile;

import java.util.ArrayList;
import java.util.List;

/**
 * created by: Veli
 * date: 26.02.2018 08:53
 */

public class FileRenameDialog extends AbstractSingleTextInputDialog
{
    public static final String TAG = FileRenameDialog.class.getSimpleName();


    public FileRenameDialog(Activity activity, List<? extends FileListAdapter.FileHolder> list)
    {
        super(activity);

        List<FileListAdapter.FileHolder> itemList = new ArrayList<>(list);
        boolean multiple = itemList.size() > 1;
        setTitle(multiple ? R.string.text_renameMultipleItems : R.string.text_rename);
        getEditText().setText(multiple ? "%d" : itemList.get(0).fileName);

        setOnProceedClickListener(R.string.butn_rename, dialog -> {
            final String renameTo = getEditText().getText().toString();

            if (multiple)
                try {
                    String.format(renameTo, itemList.size());
                    App.from(activity).run(new RenameMultipleFilesTask(itemList, renameTo));
                } catch (Exception e) {
                    getEditText().setError(activity.getString(R.string.text_errorIncludePrintfPlaceholder));
                    return false;
                }
            else if (itemList.size() == 1) {
                FileListAdapter.FileHolder fileHolder = itemList.get(0);
                List<DocumentFile> scannerList = new ArrayList<>();
                RenameMultipleFilesTask.renameFile(AppUtils.getKuick(activity), fileHolder, renameTo, scannerList);
                RenameMultipleFilesTask.notifyFileChanges(getContext(), scannerList);
            }


            return true;
        });
    }
}
