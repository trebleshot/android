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

import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.util.AppUtils;

public class ProfileEditorDialog extends AlertDialog.Builder
{
    private AlertDialog mDialog;

    public ProfileEditorDialog(@NonNull final Activity activity)
    {
        super(activity);

        final View view = LayoutInflater.from(activity).inflate(R.layout.layout_profile_editor, null, false);
        final ImageView image = view.findViewById(R.id.layout_profile_picture_image_default);
        final ImageView editImage = view.findViewById(R.id.layout_profile_picture_image_preferred);
        final EditText editText = view.findViewById(R.id.editText);
        final String deviceName = AppUtils.getLocalDeviceName(getContext());

        editText.getText().clear();
        editText.getText().append(deviceName);
        activity.loadProfilePictureInto(deviceName, image);
        editText.requestFocus();

        editImage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                activity.requestProfilePictureChange();
                saveNickname(activity, editText);
                closeIfPossible();
            }
        });

        setView(view);


        setNegativeButton(R.string.butn_remove, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                activity.deleteFile("profilePicture");
                activity.notifyUserProfileChanged();
            }
        });

        setPositiveButton(R.string.butn_save, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
               saveNickname(activity, editText);
            }
        });

        setNeutralButton(R.string.butn_close, null);
    }

    protected void closeIfPossible()
    {
        if (mDialog != null) {
            if (mDialog.isShowing())
                mDialog.dismiss();
            else
                mDialog = null;
        }
    }

    @Override
    public AlertDialog show()
    {
        return mDialog = super.show();
    }

    public void saveNickname(Activity activity, EditText editText) {
        AppUtils.getDefaultPreferences(getContext()).edit()
                .putString("device_name", editText.getText().toString())
                .apply();

        activity.notifyUserProfileChanged();
    }
}
