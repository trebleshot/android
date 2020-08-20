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
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.util.Stoppable;
import com.genonbeta.android.framework.util.StoppableImpl;

import java.io.File;

public class ShareAppDialog extends AlertDialog.Builder
{
    public ShareAppDialog(@NonNull final Context context)
    {
        super(context);

        setMessage(R.string.ques_shareAsApkOrLink);

        setNegativeButton(R.string.butn_cancel, null);
        setNeutralButton(R.string.butn_asApk, (dialogInterface, i) -> shareAsApk(context));
        setPositiveButton(R.string.butn_asLink, (dialogInterface, i) -> shareAsLink(context));
    }

    private void shareAsApk(@NonNull final Context context)
    {
        try {
            Stoppable interrupter = new StoppableImpl();

            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(context.getApplicationInfo().packageName, 0);
            String fileName = packageInfo.applicationInfo.loadLabel(pm) + "_" + packageInfo.versionName;

            DocumentFile storageDirectory = FileUtils.getApplicationDirectory(context.getApplicationContext());
            DocumentFile codeFile = DocumentFile.fromFile(new File(context.getApplicationInfo().sourceDir));
            DocumentFile cloneFile = storageDirectory.createFile(codeFile.getType(), fileName);

            if (cloneFile.exists())
                cloneFile.delete();

            FileUtils.copy(context, codeFile, cloneFile, interrupter);

            try {
                Intent sendIntent = new Intent(Intent.ACTION_SEND)
                        .putExtra(Intent.EXTRA_STREAM, FileUtils.getSecureUri(context, cloneFile))
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .setType(cloneFile.getType());

                context.startActivity(Intent.createChooser(sendIntent, context.getString(
                        R.string.text_fileShareAppChoose)));
            } catch (IllegalArgumentException e) {
                Toast.makeText(context, R.string.mesg_providerNotAllowedError, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shareAsLink(@NonNull final Context context)
    {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                String textToShare = context.getString(R.string.text_linkTrebleshot, AppConfig.URI_GOOGLE_PLAY);

                Intent sendIntent = new Intent(Intent.ACTION_SEND)
                        .putExtra(Intent.EXTRA_TEXT, textToShare)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .setType("text/plain");

                context.startActivity(Intent.createChooser(sendIntent, context.getString(
                        R.string.text_fileShareAppChoose)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
