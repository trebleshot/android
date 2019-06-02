package com.genonbeta.TrebleShot.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.util.Interrupter;

import java.io.File;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class ShareAppDialog extends AlertDialog.Builder
{
    public ShareAppDialog(@NonNull final Context context)
    {
        super(context);

        setMessage(R.string.ques_shareAsApkOrLink);

        setNegativeButton(R.string.butn_cancel, null);
        setNeutralButton(R.string.butn_asApk, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                shareAsApk(context);
            }
        });

        setPositiveButton(R.string.butn_asLink, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                shareAsLink(context);
            }
        });
    }

    private void shareAsApk(@NonNull final Context context)
    {
        new Handler(Looper.myLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    Interrupter interrupter = new Interrupter();

                    PackageManager pm = context.getPackageManager();
                    PackageInfo packageInfo = pm.getPackageInfo(context.getApplicationInfo().packageName, 0);

                    String fileName = packageInfo.applicationInfo.loadLabel(pm) + "_" + packageInfo.versionName + ".apk";

                    DocumentFile storageDirectory = FileUtils.getApplicationDirectory(context.getApplicationContext());
                    DocumentFile codeFile = DocumentFile.fromFile(new File(context.getApplicationInfo().sourceDir));
                    DocumentFile cloneFile = storageDirectory.createFile(null, FileUtils.getUniqueFileName(storageDirectory, fileName, true));

                    FileUtils.copy(context, codeFile, cloneFile, interrupter);

                    try {
                        Intent sendIntent = new Intent(Intent.ACTION_SEND)
                                .putExtra(ShareActivity.EXTRA_FILENAME_LIST, fileName)
                                .putExtra(Intent.EXTRA_STREAM, FileUtils.getSecureUri(context, cloneFile))
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                .setType(cloneFile.getType());

                        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.text_fileShareAppChoose)));
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(context, R.string.mesg_providerNotAllowedError, Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void shareAsLink(@NonNull final Context context)
    {
        new Handler(Looper.myLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    String textToShare = context.getString(R.string.text_linkTrebleshot,
                            AppConfig.URI_GOOGLE_PLAY);

                    Intent sendIntent = new Intent(Intent.ACTION_SEND)
                            .putExtra(Intent.EXTRA_TEXT, textToShare)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            .setType("text/plain");

                    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.text_fileShareAppChoose)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
