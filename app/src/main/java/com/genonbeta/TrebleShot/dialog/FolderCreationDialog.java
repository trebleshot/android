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
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.android.framework.io.DocumentFile;

/**
 * Created by: veli
 * Date: 5/30/17 12:18 PM
 */

public class FolderCreationDialog extends AbstractSingleTextInputDialog
{
    public FolderCreationDialog(final Context context, final DocumentFile currentFolder, final OnFolderCreatedListener createdListener)
    {
        super(context);

        setTitle(R.string.text_createFolder);

        setOnProceedClickListener(R.string.butn_create, new OnProceedClickListener()
        {
            @Override
            public boolean onProceedClick(AlertDialog dialog)
            {
                String fileName = getEditText().getText().toString();

                if (fileName.length() == 0)
                    return false;

                DocumentFile createdFile = currentFolder.createDirectory(fileName);

                if (createdFile == null) {
                    Toast.makeText(getContext(), R.string.mesg_folderCreateError, Toast.LENGTH_SHORT).show();
                    return false;
                }

                createdListener.onFolderCreated(createdFile);
                dialog.dismiss();

                return true;
            }
        });
    }

    public interface OnFolderCreatedListener
    {
        void onFolderCreated(DocumentFile directoryFile);
    }
}
