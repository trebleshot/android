package com.genonbeta.TrebleShot.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.util.AppUtils;

/**
 * created by: veli
 * date: 7/3/19 7:53 PM
 */
public class DialogUtils
{
    public static void showGenericCheckBoxDialog(final Activity activity, @StringRes int title,
                                                 @StringRes int text, @StringRes int positiveButton,
                                                 final ClickListener positiveListener)
    {
        View view = LayoutInflater.from(activity).inflate(R.layout.abstract_layout_dialog_text_option,
                null);
        final TextView text1 = view.findViewById(R.id.text1);
        final CheckBox checkBox1 = view.findViewById(R.id.checkbox1);

        text1.setText(text);
        checkBox1.setText(R.string.text_alsoDeleteFiles);

        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(view)
                .setNegativeButton(R.string.butn_cancel, null)
                .setPositiveButton(positiveButton, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        positiveListener.onClick(dialog, which, checkBox1);
                    }
                })
                .show();
    }

    public static void showRemoveGroupDialog(final Activity activity, final TransferGroup group)
    {
        showGenericCheckBoxDialog(activity, R.string.ques_removeAll,
                R.string.text_removeTransferGroupSummary, R.string.butn_remove,
                new ClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which, CheckBox checkBox)
                    {
                        group.setDeleteFilesOnRemoval(checkBox.isChecked());
                        AppUtils.getDatabase(activity).removeAsynchronous(activity, group);
                    }
                });
    }

    public interface ClickListener {
        void onClick(DialogInterface dialog, int which, CheckBox checkBox);
    }
}
