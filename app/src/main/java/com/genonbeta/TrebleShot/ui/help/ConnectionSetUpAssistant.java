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

package com.genonbeta.TrebleShot.ui.help;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.AddDeviceActivity;

public class ConnectionSetUpAssistant
{
    private Context mContext;

    public ConnectionSetUpAssistant(Activity activity)
    {
        mContext = activity;
    }

    public Context getContext()
    {
        return mContext;
    }

    public AlertDialog.Builder getDialogInstance()
    {
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.text_connectionWizard);
    }

    public void isThereQRCode()
    {
        getDialogInstance()
                .setMessage(R.string.ques_connectionWizardIsThereQRCode)
                .setNeutralButton(R.string.butn_cancel, null)
                .setPositiveButton(R.string.butn_yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // use barcode scanner
                        updateFragment(AddDeviceActivity.AvailableFragment.ScanQrCode);
                    }
                })
                .setNegativeButton(R.string.butn_no, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        useHotspot();
                    }
                })
                .show();
    }

    public void useNetwork()
    {
        getDialogInstance()
                .setMessage(R.string.ques_connectionWizardUseNetwork)
                .setNeutralButton(R.string.butn_cancel, null)
                .setPositiveButton(R.string.butn_yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // open network settings
                        updateFragment(AddDeviceActivity.AvailableFragment.UseExistingNetwork);
                    }
                })
                .setNegativeButton(R.string.butn_no, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        useKnownDevices();
                    }
                })
                .show();
    }

    public void useKnownDevices()
    {
        getDialogInstance()
                .setMessage(R.string.ques_connectionWizardUseKnownDevices)
                .setNeutralButton(R.string.butn_cancel, null)
                .setPositiveButton(R.string.butn_yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // open known devices settings
                        updateFragment(AddDeviceActivity.AvailableFragment.UseKnownDevice);
                    }
                })
                .setNegativeButton(R.string.butn_retry, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        isOtherDeviceReady();
                    }
                })
                .show();
    }

    public void useHotspot()
    {
        getDialogInstance()
                .setMessage(R.string.ques_connectionWizardUseHotspot)
                .setNeutralButton(R.string.butn_cancel, null)
                .setPositiveButton(R.string.butn_yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // open hotspot settings
                        updateFragment(AddDeviceActivity.AvailableFragment.CreateHotspot);
                    }
                })
                .setNegativeButton(R.string.butn_no, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        useNetwork();
                    }
                })
                .show();
    }

    public void isOtherDeviceReady()
    {
        getDialogInstance()
                .setMessage(R.string.ques_connectionWizardIsOtherDeviceReady)
                .setNeutralButton(R.string.butn_cancel, null)
                .setPositiveButton(R.string.butn_yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        isThereQRCode();
                    }
                })
                .setNegativeButton(R.string.butn_no, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        useHotspot();
                    }
                })
                .show();
    }

    public void startShowing()
    {
        isOtherDeviceReady();
    }

    public void updateFragment(AddDeviceActivity.AvailableFragment fragment)
    {
        getContext().sendBroadcast(new Intent(AddDeviceActivity.ACTION_CHANGE_FRAGMENT)
                .putExtra(AddDeviceActivity.EXTRA_FRAGMENT_ENUM, fragment.toString()));
    }
}
