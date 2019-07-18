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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.android.framework.io.DocumentFile;

import java.io.IOException;

/**
 * created by: Veli
 * date: 10.11.2017 14:59
 */

public class TransferInfoDialog extends AlertDialog.Builder
{
    public TransferInfoDialog(@NonNull final Activity activity, final TransferGroup group,
                              final TransferObject transferObject)
    {
        super(activity);

        DocumentFile attemptedFile = null;
        boolean isIncoming = TransferObject.Type.INCOMING.equals(transferObject.type);

        try {
            // If it is incoming than get the received or cache file
            // If not then try to reach to the source file that is being send
            attemptedFile = isIncoming
                    ? FileUtils.getIncomingPseudoFile(getContext(), transferObject, group, false)
                    : FileUtils.fromUri(getContext(), Uri.parse(transferObject.file));
        } catch (Exception e) {
            e.printStackTrace();
        }

        final DocumentFile pseudoFile = attemptedFile;
        boolean fileExists = pseudoFile != null && pseudoFile.canRead();

        @SuppressLint("InflateParams")
        View rootView = LayoutInflater.from(activity).inflate(R.layout.layout_transfer_info, null);

        TextView nameText = rootView.findViewById(R.id.transfer_info_file_name);
        TextView sizeText = rootView.findViewById(R.id.transfer_info_file_size);
        TextView typeText = rootView.findViewById(R.id.transfer_info_file_mime);
        TextView flagText = rootView.findViewById(R.id.transfer_info_file_status);

        View incomingDetailsLayout = rootView.findViewById(R.id.transfer_info_incoming_details_layout);
        TextView receivedSizeText = rootView.findViewById(R.id.transfer_info_received_size);
        TextView locationText = rootView.findViewById(R.id.transfer_info_pseudo_location);

        setTitle(R.string.text_transactionDetails);
        setView(rootView);

        nameText.setText(transferObject.name);
        sizeText.setText(FileUtils.sizeExpression(transferObject.size, false));
        typeText.setText(transferObject.mimeType);

        receivedSizeText.setText(fileExists
                ? FileUtils.sizeExpression(pseudoFile.length(), false)
                : getContext().getString(R.string.text_unknown));

        locationText.setText(fileExists
                ? FileUtils.getReadableUri(pseudoFile.getUri())
                : getContext().getString(R.string.text_unknown));

        setPositiveButton(R.string.butn_close, null);
        setNegativeButton(R.string.butn_remove, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                DialogUtils.showRemoveDialog(activity, transferObject);
            }
        });

        if (isIncoming) {
            flagText.setText(TextUtils.getTransactionFlagString(transferObject.getFlag()));
            incomingDetailsLayout.setVisibility(View.VISIBLE);

            if (TransferObject.Flag.INTERRUPTED.equals(transferObject.getFlag())
                    || TransferObject.Flag.IN_PROGRESS.equals(transferObject.getFlag())) {
                setNeutralButton(R.string.butn_retry, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        transferObject.setFlag(TransferObject.Flag.PENDING);
                        AppUtils.getDatabase(activity).publish(transferObject);
                    }
                });
            } else if (fileExists) {
                if (TransferObject.Flag.REMOVED.equals(transferObject.getFlag()) && pseudoFile.getParentFile() != null) {
                    setNeutralButton(R.string.butn_saveAnyway, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            AlertDialog.Builder saveAnyway = new AlertDialog.Builder(getContext());

                            saveAnyway.setTitle(R.string.ques_saveAnyway);
                            saveAnyway.setMessage(R.string.text_saveAnywaySummary);
                            saveAnyway.setNegativeButton(R.string.butn_cancel, null);
                            saveAnyway.setPositiveButton(R.string.butn_proceed, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    try {
                                        DocumentFile savedFile = FileUtils.saveReceivedFile(pseudoFile.getParentFile(), pseudoFile, transferObject);
                                        transferObject.file = savedFile.getName();
                                        transferObject.setFlag(TransferObject.Flag.DONE);

                                        AppUtils.getDatabase(activity).update(transferObject);

                                        Toast.makeText(getContext(), R.string.mesg_fileSaved, Toast.LENGTH_SHORT).show();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(getContext(), R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                            saveAnyway.show();
                        }
                    });
                } else if (TransferObject.Flag.DONE.equals(transferObject.getFlag())) {
                    setNeutralButton(R.string.butn_open, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            try {
                                FileUtils.openUri(getContext(), pseudoFile);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        } else if (TransferObject.Type.OUTGOING.equals(transferObject.type) && fileExists) {
            try {
                final Intent startIntent = FileUtils.getOpenIntent(getContext(), attemptedFile);

                setNeutralButton(R.string.butn_open, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        try {
                            getContext().startActivity(startIntent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                // Do nothing
            }
        }
    }
}
