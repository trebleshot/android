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
import com.genonbeta.TrebleShot.object.FileShortcutObject;
import com.genonbeta.TrebleShot.object.WritablePathObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.android.framework.io.DocumentFile;

import java.util.ArrayList;
import java.util.List;

/**
 * created by: Veli
 * date: 26.02.2018 08:53
 */

public class FileRenameDialog<T extends FileListAdapter.GenericFileHolder> extends AbstractSingleTextInputDialog
{
    public static final String TAG = FileRenameDialog.class.getSimpleName();
    public static final int JOB_RENAME_FILES = 0;

    private List<T> mItemList = new ArrayList<>();

    public FileRenameDialog(final Context context, List<T> itemList, final OnFileRenameListener renameListener)
    {
        super(context);

        mItemList.addAll(itemList);

        setTitle(mItemList.size() > 1
                ? R.string.text_renameMultipleItems
                : R.string.text_rename);

        getEditText().setText(mItemList.size() > 1
                ? "%d"
                : mItemList.get(0).fileName);

        setOnProceedClickListener(R.string.butn_rename, dialog -> {
            final String renameTo = getEditText().getText().toString();

            if (getItemList().size() == 1
                    && renameFile(getItemList().get(0), renameTo, renameListener)) {
                if (renameListener != null)
                    renameListener.onFileRenameCompleted(getContext());
                return true;
            }

            try {
                String.format(renameTo, getItemList().size());
            } catch (Exception e) {
                return false;
            }

            new WorkerService.RunningTask()
            {
                @Override
                protected void onRun()
                {
                    int fileId = 0;

                    for (T fileHolder : getItemList()) {
                        publishStatusText(fileHolder.friendlyName);

                        String ext = FileUtils.getFileFormat(fileHolder.file.getName());
                        ext = ext != null ? String.format(".%s", ext) : "";

                        renameFile(fileHolder, String.format("%s%s", String.format(renameTo, fileId), ext),
                                renameListener);
                        fileId++;
                    }

                    if (renameListener != null)
                        renameListener.onFileRenameCompleted(getService());
                }
            }.setTitle(context.getString(R.string.text_renameMultipleItems))
                    .setIconRes(R.drawable.ic_compare_arrows_white_24dp_static)
                    .run(context);

            return true;
        });
    }

    public List<T> getItemList()
    {
        return mItemList;
    }

    public boolean renameFile(T holder, String renameTo, OnFileRenameListener renameListener)
    {
        try {
            if (holder instanceof FileListAdapter.ShortcutDirectoryHolder) {
                FileShortcutObject object = ((FileListAdapter.ShortcutDirectoryHolder) holder).getShortcutObject();

                if (object != null) {
                    object.title = renameTo;
                    AppUtils.getDatabase(getContext()).publish(object);
                    AppUtils.getDatabase(getContext()).broadcast();
                }
            } else if (holder instanceof FileListAdapter.WritablePathHolder) {
                WritablePathObject object = ((FileListAdapter.WritablePathHolder) holder).pathObject;

                if (object != null) {
                    object.title = renameTo;
                    AppUtils.getDatabase(getContext()).publish(object);
                    AppUtils.getDatabase(getContext()).broadcast();
                }
            } else if (holder.file.canWrite() && holder.file.renameTo(renameTo)) {
                if (renameListener != null)
                    renameListener.onFileRename(holder.file, renameTo);

                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public interface OnFileRenameListener
    {
        void onFileRename(DocumentFile file, String displayName);

        void onFileRenameCompleted(Context context);
    }
}
