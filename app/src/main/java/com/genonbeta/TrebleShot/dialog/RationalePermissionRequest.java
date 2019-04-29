package com.genonbeta.TrebleShot.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;

import static com.genonbeta.TrebleShot.activity.HomeActivity.REQUEST_PERMISSION_ALL;

/**
 * created by: Veli
 * date: 18.11.2017 20:16
 */

public class RationalePermissionRequest extends AlertDialog.Builder
{
    public PermissionRequest mPermissionQueue;

    public RationalePermissionRequest(final Activity activity,
                                      @NonNull PermissionRequest permission,
                                      boolean killActivityOtherwise)
    {
        super(activity);

        mPermissionQueue = permission;

        setCancelable(false);
        setTitle(permission.title);
        setMessage(permission.message);

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, mPermissionQueue.permission))
            setNeutralButton(R.string.butn_settings, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    Intent intent = new Intent()
                            .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts("package", activity.getPackageName(), null));

                    activity.startActivity(intent);
                }
            });

        setPositiveButton(R.string.butn_ask, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                ActivityCompat.requestPermissions(activity, new String[]{mPermissionQueue.permission}, REQUEST_PERMISSION_ALL);
            }
        });

        if (killActivityOtherwise)
            setNegativeButton(R.string.butn_reject, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    activity.finish();
                }
            });
        else
            setNegativeButton(R.string.butn_close, null);
    }

    public static AlertDialog requestIfNecessary(Activity activity,
                                                 PermissionRequest permissionQueue,
                                                 boolean killActivityOtherwise)
    {
        return ActivityCompat.checkSelfPermission(activity, permissionQueue.permission) == PackageManager.PERMISSION_GRANTED
                ? null
                : new RationalePermissionRequest(activity, permissionQueue, killActivityOtherwise).show();
    }

    public static class PermissionRequest
    {
        public String permission;
        public String title;
        public String message;
        public boolean required;

        public PermissionRequest(String permission, String title, String message)
        {
            this(permission, title, message, true);
        }

        public PermissionRequest(String permission, String title, String message, boolean required)
        {
            this.permission = permission;
            this.title = title;
            this.message = message;
            this.required = required;
        }

        public PermissionRequest(Context context, String permission, int titleRes, int messageRes)
        {
            this(permission, context.getString(titleRes), context.getString(messageRes));
        }
    }
}