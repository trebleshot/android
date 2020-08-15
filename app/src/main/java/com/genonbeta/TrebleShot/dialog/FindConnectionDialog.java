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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.TrebleShot.App;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.ProgressDialog;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.task.FindWorkingNetworkTask;
import com.genonbeta.TrebleShot.util.DeviceLoader;

public class FindConnectionDialog extends ProgressDialog
{
    FindConnectionDialog(Activity activity)
    {
        super(activity);
    }

    public static void show(Activity activity, Device device, @Nullable DeviceLoader.OnDeviceResolvedListener listener)
    {
        FindConnectionDialog dialog = new FindConnectionDialog(activity);
        LocalTaskBinder binder = new LocalTaskBinder(activity, dialog, device, listener);
        FindWorkingNetworkTask task = new FindWorkingNetworkTask(device);

        task.setAnchor(binder);
        task.addCloser(userAction -> task.removeAnchor());

        dialog.setTitle(R.string.text_automaticNetworkConnectionOngoing);
        dialog.setOnDismissListener(dialog1 -> task.interrupt());
        dialog.setOnCancelListener(dialog1 -> task.interrupt());
        dialog.show();

        App.run(activity, task);
    }

    static class LocalTaskBinder implements FindWorkingNetworkTask.CalculationResultListener
    {
        Activity activity;
        FindConnectionDialog dialog;
        Device device;
        DeviceLoader.OnDeviceResolvedListener listener;

        LocalTaskBinder(Activity activity, FindConnectionDialog dialog, Device device,
                        DeviceLoader.OnDeviceResolvedListener listener)
        {
            this.activity = activity;
            this.dialog = dialog;
            this.device = device;
            this.listener = listener;
        }

        @Override
        public void onTaskStateChanged(BaseAttachableAsyncTask task)
        {
            if (task.isFinished()) {
                dialog.dismiss();
            } else {
                dialog.setMax(task.progress().getTotal());
                dialog.setProgress(task.progress().getTotal());
            }
        }

        @Override
        public boolean onTaskMessage(TaskMessage message)
        {
            activity.runOnUiThread(() -> message.toDialogBuilder(activity).show());
            return true;
        }

        @Override
        public void onCalculationResult(Device device, @Nullable DeviceAddress address)
        {
            if (address == null) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.text_error)
                        .setMessage(R.string.text_automaticNetworkConnectionFailed)
                        .setNeutralButton(R.string.butn_close, null)
                        .setPositiveButton(R.string.butn_retry,
                                (dialog, which) -> FindConnectionDialog.show(activity, device, listener));
            } else
                listener.onDeviceResolved(device, address);
        }
    }
}