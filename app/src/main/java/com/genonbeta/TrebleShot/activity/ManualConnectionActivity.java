/*
 * Copyright (C) 2020 Veli Tasalı
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

package com.genonbeta.TrebleShot.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.transition.TransitionManager;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.object.DeviceRoute;
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class ManualConnectionActivity extends Activity implements DeviceIntroductionTask.ResultListener
{
    public static final String
            TAG = ManualConnectionActivity.class.getSimpleName(),
            EXTRA_DEVICE = "extraDevice",
            EXTRA_DEVICE_ADDRESS = "extraDeviceAddress";

    private final CheckHostnameListener hostnameListener = new CheckHostnameListener();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_address_connection);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.confirm_button).setOnClickListener((v) -> {
            AppCompatEditText editText = getEditText();
            Editable editable = editText.getText();

            if (editable == null || editable.length() <= 0)
                editText.setError(getString(R.string.mesg_enterValidHostAddress));
            else {
                CheckAddressAsyncTask asyncTask = new CheckAddressAsyncTask(hostnameListener);
                asyncTask.execute(editable.toString());
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        getEditText().requestFocus();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home)
            onBackPressed();
        else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    protected void onAttachTasks(List<BaseAttachableAsyncTask> taskList)
    {
        super.onAttachTasks(taskList);
        for (BaseAttachableAsyncTask task : taskList)
            if (task instanceof DeviceIntroductionTask)
                ((DeviceIntroductionTask) task).setAnchor(this);
    }

    @Override
    public void onTaskStateChange(BaseAttachableAsyncTask task,
                                  com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask.State state)
    {
        boolean running = task instanceof DeviceIntroductionTask && !task.isFinished();
        setShowProgress(running);
        getButton().setEnabled(!running);
    }

    @Override
    public boolean onTaskMessage(TaskMessage message)
    {
        Log.d(TAG, message.getMessage());
        runOnUiThread(() -> getEditText().setError(message.getMessage()));
        return true;
    }

    @Override
    public void onDeviceReached(DeviceRoute deviceRoute)
    {
        setResult(RESULT_OK, new Intent()
                .putExtra(EXTRA_DEVICE, deviceRoute.device)
                .putExtra(EXTRA_DEVICE_ADDRESS, deviceRoute.address));
        finish();
    }

    private Button getButton()
    {
        return findViewById(R.id.confirm_button);
    }

    private AppCompatEditText getEditText()
    {
        return findViewById(R.id.editText);
    }

    private void setShowProgress(boolean show)
    {
        findViewById(R.id.progressBar).setVisibility(show ? View.VISIBLE : View.GONE);
        TransitionManager.beginDelayedTransition(findViewById(R.id.layout_main));
    }

    public static class CheckAddressAsyncTask extends AsyncTask<String, Void, InetAddress>
    {
        private final CheckHostnameListener listener;

        public CheckAddressAsyncTask(CheckHostnameListener listener)
        {
            this.listener = listener;
        }

        @Override
        protected InetAddress doInBackground(String... address)
        {
            try {
                if (address.length > 0)
                    return InetAddress.getByName(address[0]);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(InetAddress address)
        {
            super.onPostExecute(address);

            if (address == null)
                listener.onHostnameError();
            else
                listener.onConnect(address);
        }
    }

    public class CheckHostnameListener
    {
        public void onConnect(InetAddress address)
        {
            runUiTask(new DeviceIntroductionTask(address, 0), ManualConnectionActivity.this);
        }

        void onHostnameError()
        {
            getEditText().setError(getString(R.string.mesg_unknownHostError));
        }
    }
}