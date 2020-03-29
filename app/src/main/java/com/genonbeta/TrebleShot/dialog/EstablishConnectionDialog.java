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
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.ProgressDialog;
import com.genonbeta.TrebleShot.callback.OnConnectionSelectionListener;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableBgTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.task.AssessNetworkTask;
import com.genonbeta.TrebleShot.task.AssessNetworkTask.ConnectionResult;
import com.genonbeta.android.framework.util.Stoppable;
import com.genonbeta.android.framework.util.StoppableImpl;

import java.util.List;

public class EstablishConnectionDialog extends ProgressDialog
{
    EstablishConnectionDialog(Activity activity)
    {
        super(activity);
    }

    public static void show(Activity activity, Device device, @Nullable OnConnectionSelectionListener listener)
    {
        Stoppable stoppable = new StoppableImpl();

        EstablishConnectionDialog dialog = new EstablishConnectionDialog(activity);
        LocalTaskBinder binder = new LocalTaskBinder(activity, dialog, device, listener);
        AssessNetworkTask task = new AssessNetworkTask(device);

        task.setAnchor(binder);
        task.setStoppable(stoppable);

        dialog.setTitle(R.string.text_automaticNetworkConnectionOngoing);
        dialog.setOnDismissListener((dialog1 -> stoppable.interrupt()));
        dialog.setOnCancelListener(dialog1 -> stoppable.interrupt());
        dialog.show();

        BackgroundService.run(activity, task);
    }

    static class LocalTaskBinder implements AssessNetworkTask.CalculationResultListener
    {
        Activity activity;
        EstablishConnectionDialog dialog;
        Device device;
        OnConnectionSelectionListener listener;

        LocalTaskBinder(Activity activity, EstablishConnectionDialog dialog, Device device,
                        OnConnectionSelectionListener listener)
        {
            this.activity = activity;
            this.dialog = dialog;
            this.device = device;
            this.listener = listener;
        }

        @Override
        public void onTaskStateChanged(BaseAttachableBgTask task)
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
            return false;
        }

        @Override
        public void onCalculationResult(ConnectionResult[] connectionResults)
        {
            if (connectionResults.length <= 0)
                return;

            List<ConnectionResult> availableList = AssessNetworkTask.getAvailableList(connectionResults);
            if (availableList.size() <= 0) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.text_error)
                        .setMessage(R.string.text_automaticNetworkConnectionFailed)
                        .setNeutralButton(R.string.butn_close, null)
                        .setPositiveButton(R.string.butn_retry,
                                (dialog, which) -> EstablishConnectionDialog.show(activity, device, listener));
            } else if (listener == null) {
                new ConnectionTestDialog(activity, device, connectionResults).show();
            } else
                listener.onConnectionSelection(availableList.get(0).connection);
        }
    }
}