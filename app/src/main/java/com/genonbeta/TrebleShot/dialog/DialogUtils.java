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
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.util.AppUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * created by: veli
 * date: 7/3/19 7:53 PM
 */
public class DialogUtils
{
    public static void showGenericCheckBoxDialog(final Activity activity, @StringRes int title,
                                                 String content, @StringRes int positiveButton,
                                                 @StringRes int checkBox, final ClickListener positiveListener,
                                                 String... textArgs)
    {
        View view = LayoutInflater.from(activity).inflate(R.layout.abstract_layout_dialog_text_option,
                null);
        final TextView text1 = view.findViewById(R.id.text1);
        final CheckBox checkBox1 = view.findViewById(R.id.checkbox1);

        text1.setText(content);

        if (checkBox == 0)
            checkBox1.setVisibility(View.GONE);
        else
            checkBox1.setText(checkBox);

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

    public static void showRemoveDialog(final Activity activity, final TransferGroup group)
    {
        showGenericCheckBoxDialog(activity, R.string.ques_removeAll,
                activity.getString(R.string.text_removeTransferGroupSummary),
                R.string.butn_remove, R.string.text_alsoDeleteReceivedFiles,
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

    public static void showRemoveDialog(final Activity activity, final TransferObject object)
    {
        int checkBox = TransferObject.Type.INCOMING.equals(object.type)
                ? R.string.text_alsoDeleteReceivedFiles : 0;
        showGenericCheckBoxDialog(activity, R.string.ques_removeTransfer,
                activity.getString(R.string.text_removeTransferSummary, object.friendlyName),
                R.string.butn_remove, checkBox,
                new ClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which, CheckBox checkBox)
                    {
                        object.setDeleteOnRemoval(checkBox.isChecked());
                        AppUtils.getDatabase(activity).removeAsynchronous(activity, object);
                    }
                });
    }

    public static void showRemoveTransferObjectListDialog(final Activity activity, final List<? extends TransferObject> objects)
    {
        final List<TransferObject> copiedObjects = new ArrayList<>(objects);

        showGenericCheckBoxDialog(activity, R.string.ques_removeTransfer,
                activity.getResources().getQuantityString(R.plurals.text_removeQueueSummary, objects.size(), objects.size()),
                R.string.butn_remove, R.string.text_alsoDeleteReceivedFiles,
                new ClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which, CheckBox checkBox)
                    {
                        boolean isChecked = checkBox.isChecked();

                        for (TransferObject object : copiedObjects)
                            object.setDeleteOnRemoval(isChecked);

                        AppUtils.getDatabase(activity).removeAsynchronous(activity, copiedObjects);
                    }
                });
    }


    public static void showRemoveTransferGroupListDialog(final Activity activity, final List<? extends TransferGroup> groups)
    {
        final List<TransferGroup> copiedGroups = new ArrayList<>(groups);

        showGenericCheckBoxDialog(activity, R.string.ques_removeAll,
                activity.getString(R.string.text_removeSelected),
                R.string.butn_remove, R.string.text_alsoDeleteReceivedFiles,
                new ClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which, CheckBox checkBox)
                    {
                        boolean isChecked = checkBox.isChecked();

                        for (TransferGroup group : copiedGroups)
                            group.setDeleteFilesOnRemoval(isChecked);

                        AppUtils.getDatabase(activity).removeAsynchronous(activity, copiedGroups);
                    }
                });
    }

    public interface ClickListener
    {
        void onClick(DialogInterface dialog, int which, CheckBox checkBox);
    }
}
