package com.genonbeta.TrebleShot.receiver;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.view.WindowManager;

import com.genonbeta.TrebleShot.R;

public class DialogEventReceiver extends BroadcastReceiver
{
    public final static String ACTION_DIALOG = "com.genonbeta.TrebleShot.action.makeDialog";

    public final static String EXTRA_TITLE = "title";
    public final static String EXTRA_MESSAGE = "message";
    public final static String EXTRA_POSITIVE_INTENT = "positive";
    public final static String EXTRA_NEGATIVE_INTENT = "negative";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (ACTION_DIALOG.equals(intent.getAction()) && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP)
            showDialog(context, intent.getStringExtra(EXTRA_TITLE), intent.getStringExtra(EXTRA_MESSAGE), (PendingIntent) intent.getParcelableExtra(EXTRA_POSITIVE_INTENT), (PendingIntent) intent.getParcelableExtra(EXTRA_NEGATIVE_INTENT));
    }

    public void showDialog(Context context, String title, String message, final PendingIntent accept, final PendingIntent reject)
    {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

        if (title != null)
            dialogBuilder.setTitle(title);

        if (message != null)
            dialogBuilder.setMessage(message);

        if (accept != null)
            dialogBuilder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface p1, int p2)
                        {
                            try {
                                accept.send();
                            } catch (PendingIntent.CanceledException e) {
                                e.printStackTrace();
                            }
                        }
                    }
            );

        if (reject != null)
            dialogBuilder.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface p1, int p2)
                        {
                            try {
                                reject.send();
                            } catch (PendingIntent.CanceledException e) {
                                e.printStackTrace();
                            }
                        }
                    }
            );
        else
            dialogBuilder.setNegativeButton(R.string.butn_close, null);

        Dialog dialog = dialogBuilder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }
}
