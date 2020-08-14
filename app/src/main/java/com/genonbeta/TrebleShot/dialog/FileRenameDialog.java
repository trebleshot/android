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

import android.content.Context;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
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

    private List<FileListAdapter.FileHolder> mItemList = new ArrayList<>();

    public FileRenameDialog(Context context, List<? extends FileListAdapter.FileHolder> itemList,
                            final OnFileRenameListener renameListener)
    {
        super(context);

        mItemList.addAll(itemList);

        setTitle(mItemList.size() > 1 ? R.string.text_renameMultipleItems : R.string.text_rename);
        getEditText().setText(mItemList.size() > 1 ? "%d" : mItemList.get(0).fileName);

        setOnProceedClickListener(R.string.butn_rename, dialog -> {
            final String renameTo = getEditText().getText().toString();

            if (mItemList.size() > 1) {
            }

            try {
                String.format(renameTo, mItemList.size());
            } catch (Exception e) {
                return false;
            }

            return true;
        });
    }

    public interface OnFileRenameListener
    {
        void onFileRename(DocumentFile file, String displayName);

        void onFileRenameCompleted(Context context);
    }
}
