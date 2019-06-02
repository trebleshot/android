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
                AppUtils.getDefaultPreferences(getContext()).edit()
                        .putString("device_name", editText.getText().toString())
                        .apply();

                activity.notifyUserProfileChanged();
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
}
