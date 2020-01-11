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
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.util.AppUtils;

import static com.genonbeta.TrebleShot.activity.HomeActivity.REQUEST_PERMISSION_ALL;

/**
 * created by: Veli
 * date: 18.11.2017 20:16
 */

public class RationalePermissionRequest extends AlertDialog.Builder
{
    public PermissionRequest mPermissionQueue;

    public RationalePermissionRequest(final Activity activity, @NonNull PermissionRequest permission,
                                      boolean killActivityOtherwise)
    {
        super(activity);

        mPermissionQueue = permission;

        setCancelable(false);
        setTitle(permission.title);
        setMessage(permission.message);

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, mPermissionQueue.permission))
            setNeutralButton(R.string.butn_settings, (dialogInterface, i) -> AppUtils.startApplicationDetails(activity));

        setPositiveButton(R.string.butn_ask, (dialogInterface, i) -> ActivityCompat.requestPermissions(activity,
                new String[]{mPermissionQueue.permission}, REQUEST_PERMISSION_ALL));

        if (killActivityOtherwise)
            setNegativeButton(R.string.butn_reject, (dialogInterface, i) -> activity.finish());
        else
            setNegativeButton(R.string.butn_close, null);
    }

    public static AlertDialog requestIfNecessary(Activity activity, PermissionRequest permissionQueue,
                                                 boolean killActivityOtherwise)
    {
        return ActivityCompat.checkSelfPermission(activity, permissionQueue.permission)
                == PackageManager.PERMISSION_GRANTED ? null : new RationalePermissionRequest(activity, permissionQueue,
                killActivityOtherwise).show();
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