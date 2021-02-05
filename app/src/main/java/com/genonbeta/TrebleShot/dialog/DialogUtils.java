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

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.dataobject.Transfer;
import com.genonbeta.TrebleShot.dataobject.TransferItem;
import com.genonbeta.TrebleShot.util.AppUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * created by: veli
 * date: 7/3/19 7:53 PM
 */
public class DialogUtils
{
    public static void showGenericCheckBoxDialog(final Activity activity, @StringRes int title, String content,
                                                 @StringRes int positiveButton, @StringRes int checkBox,
                                                 final ClickListener positiveListener, String... textArgs)
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
                .setPositiveButton(positiveButton, (dialog, which) -> positiveListener.onClick(dialog, which,
                        checkBox1))
                .show();
    }

    public static void showRemoveDialog(final Activity activity, final Transfer transfer)
    {
        showGenericCheckBoxDialog(activity, R.string.ques_removeAll,
                activity.getString(R.string.text_removeTransferGroupSummary),
                R.string.butn_remove, R.string.text_alsoDeleteReceivedFiles,
                (dialog, which, checkBox) -> {
                    transfer.deleteFilesOnRemoval = checkBox.isChecked();
                    AppUtils.getKuick(activity).removeAsynchronous(activity, transfer, null);
                });
    }

    public static void showRemoveDialog(final Activity activity, final TransferItem object)
    {
        int checkBox = TransferItem.Type.INCOMING.equals(object.type) ? R.string.text_alsoDeleteReceivedFiles : 0;
        showGenericCheckBoxDialog(activity, R.string.ques_removeTransfer, activity.getString(
                R.string.text_removeTransferSummary, object.name),
                R.string.butn_remove, checkBox, (dialog, which, checkBox1) -> {
                    object.setDeleteOnRemoval(checkBox1.isChecked());
                    AppUtils.getKuick(activity).removeAsynchronous(activity, object, null);
                });
    }

    public static void showRemoveTransferObjectListDialog(final Activity activity,
                                                          final List<? extends TransferItem> objects)
    {
        final List<TransferItem> copiedObjects = new ArrayList<>(objects);

        showGenericCheckBoxDialog(activity, R.string.ques_removeTransfer,
                activity.getResources().getQuantityString(R.plurals.text_removeQueueSummary, objects.size(), objects.size()),
                R.string.butn_remove, R.string.text_alsoDeleteReceivedFiles,
                (dialog, which, checkBox) -> {
                    boolean isChecked = checkBox.isChecked();

                    for (TransferItem object : copiedObjects)
                        object.setDeleteOnRemoval(isChecked);

                    AppUtils.getKuick(activity).removeAsynchronous(activity, copiedObjects, null);
                });
    }


    public static void showRemoveTransferGroupListDialog(final Activity activity,
                                                         List<? extends Transfer> groups)
    {
        final List<Transfer> copiedTransfers = new ArrayList<>(groups);

        showGenericCheckBoxDialog(activity, R.string.ques_removeAll, activity.getString(R.string.text_removeSelected),
                R.string.butn_remove, R.string.text_alsoDeleteReceivedFiles,
                (dialog, which, checkBox) -> {
                    boolean isChecked = checkBox.isChecked();

                    for (Transfer transfer : copiedTransfers)
                        transfer.deleteFilesOnRemoval = isChecked;

                    AppUtils.getKuick(activity).removeAsynchronous(activity, copiedTransfers, null);
                });
    }

    public interface ClickListener
    {
        void onClick(DialogInterface dialog, int which, CheckBox checkBox);
    }
}
