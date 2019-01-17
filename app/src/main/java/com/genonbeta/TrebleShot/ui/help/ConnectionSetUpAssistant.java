package com.genonbeta.TrebleShot.ui.help;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ConnectionManagerActivity;

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
                        updateFragment(ConnectionManagerActivity.AvailableFragment.ScanQrCode);
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
                        updateFragment(ConnectionManagerActivity.AvailableFragment.UseExistingNetwork);
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
                        updateFragment(ConnectionManagerActivity.AvailableFragment.UseKnownDevice);
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
                        updateFragment(ConnectionManagerActivity.AvailableFragment.CreateHotspot);
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

    public void updateFragment(ConnectionManagerActivity.AvailableFragment fragment)
    {
        getContext().sendBroadcast(new Intent(ConnectionManagerActivity.ACTION_CHANGE_FRAGMENT)
                .putExtra(ConnectionManagerActivity.EXTRA_FRAGMENT_ENUM, fragment.toString()));
    }
}
