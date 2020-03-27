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
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.ProgressDialog;
import com.genonbeta.TrebleShot.callback.OnDeviceSelectedListener;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.task.AssessNetworkTask;
import com.genonbeta.android.framework.util.Stoppable;
import com.genonbeta.android.framework.util.StoppableImpl;

public class EstablishConnectionDialog extends ProgressDialog
{
    private AssessNetworkTask mTask;

    public EstablishConnectionDialog(final Activity activity, final Device device,
                                     @Nullable final OnDeviceSelectedListener listener)
    {
        super(activity);

        final Stoppable stoppable = new StoppableImpl();

        setTitle(R.string.text_automaticNetworkConnectionOngoing);
        setCancelable(false);
        setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        setButton(ProgressDialog.BUTTON_NEGATIVE, getContext().getString(R.string.butn_cancel), (dialogInterface, i) -> stoppable.interrupt());

        mTask = new AssessNetworkTask(this, device, listener);
        mTask.setStoppable(stoppable);
    }

    @Override
    public void show()
    {
        super.show();
        BackgroundService.run(getOwnerActivity(), mTask);
    }

    public static class ConnectionResult
    {
        public DeviceConnection connection;
        public long pingTime = 0; // nano
        public boolean successful = false;

        public ConnectionResult(DeviceConnection connection)
        {
            this.connection = connection;
        }
    }
}