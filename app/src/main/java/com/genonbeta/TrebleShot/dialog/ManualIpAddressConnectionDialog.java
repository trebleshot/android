package com.genonbeta.TrebleShot.dialog;

import android.content.Context;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.ui.UIConnectionUtils;

/**
 * created by: veli
 * date: 4/4/19 7:27 PM
 */
public class ManualIpAddressConnectionDialog extends AbstractSingleTextInputDialog
{
    public ManualIpAddressConnectionDialog(Context context, UIConnectionUtils utils)
    {
        super(context);

        setTitle(R.string.butn_enterIpAddress);

    }
}
