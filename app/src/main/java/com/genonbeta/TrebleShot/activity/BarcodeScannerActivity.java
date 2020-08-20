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

package com.genonbeta.TrebleShot.activity;

import android.Manifest;
import android.app.Service;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.DeviceListAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.DeviceRoute;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.Connections;
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class BarcodeScannerActivity extends Activity implements DeviceIntroductionTask.ResultListener,
        SnackbarPlacementProvider
{
    public static final String EXTRA_DEVICE = "extraDevice";

    public static final String EXTRA_DEVICE_ADDRESS = "extraDeviceAddress";

    public static final int REQUEST_PERMISSION_CAMERA = 1;

    public static final int REQUEST_PERMISSION_LOCATION = 2;

    private final DialogInterface.OnDismissListener mDismissListener = dialog -> updateState();
    private DecoratedBarcodeView mBarcodeView;
    private Connections mConnections;
    private ViewGroup mConductContainer;
    private TextView mConductText;
    private ImageView mConductImage;
    private ImageView mTextModeIndicator;
    private Button mConductButton;
    private Button mTaskInterruptButton;
    private View mTaskContainer;
    private final IntentFilter mIntentFilter = new IntentFilter();
    private boolean mPermissionRequestedCamera = false;
    private boolean mPermissionRequestedLocation = false;
    private boolean mShowAsText = false;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
                    || ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())
                    || LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction()))
                updateState();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setResult(RESULT_CANCELED);

        mConnections = new Connections(this);
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);

        mConductContainer = findViewById(R.id.layout_barcode_connect_conduct_container);
        mTextModeIndicator = findViewById(R.id.layout_barcode_connect_mode_text_indicator);
        mConductButton = findViewById(R.id.layout_barcode_connect_conduct_button);
        mBarcodeView = findViewById(R.id.layout_barcode_connect_barcode_view);
        mConductText = findViewById(R.id.layout_barcode_connect_conduct_text);
        mConductImage = findViewById(R.id.layout_barcode_connect_conduct_image);
        mTaskContainer = findViewById(R.id.container_task);
        mTaskInterruptButton = findViewById(R.id.task_interrupter_button);

        mBarcodeView.decodeContinuous(new BarcodeCallback()
        {
            @Override
            public void barcodeResult(BarcodeResult result)
            {
                handleBarcode(result.getResult().getText());
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints)
            {

            }
        });

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
        updateState();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(mReceiver);
        mBarcodeView.pauseAndWait();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mBarcodeView.pause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (permissions.length > 0) {
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.CAMERA.equals(permissions[i]) &&
                        grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    updateState();
                    mPermissionRequestedCamera = false;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.actions_barcode_scanner, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home)
            onBackPressed();
        else if (id == R.id.show_help)
            new AlertDialog.Builder(this)
                    .setMessage(R.string.help_scanQRCode)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        else if (id == R.id.change_mode) {
            mShowAsText = !mShowAsText;
            mTextModeIndicator.setVisibility(mShowAsText ? View.VISIBLE : View.GONE);
            item.setIcon(mShowAsText ? R.drawable.ic_qrcode_white_24dp : R.drawable.ic_short_text_white_24dp);

            createSnackbar(mShowAsText ? R.string.mesg_qrScannerTextMode : R.string.mesg_qrScannerDefaultMode).show();
            updateState();
        } else
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

        updateState(hasTaskOf(DeviceIntroductionTask.class));
    }

    @Override
    public Snackbar createSnackbar(int resId, Object... objects)
    {
        return Snackbar.make(mTaskInterruptButton, getString(resId, objects), Snackbar.LENGTH_LONG);
    }

    protected void handleBarcode(final String code)
    {
        try {
            if (mShowAsText)
                throw new Exception("Showing as text.");

            String[] values = code.split(";");
            String type = values[0];

            // empty-strings cause trouble and are harder to manage.
            for (int i = 0; i < values.length; i++)
                if (values[i].length() == 0)
                    values[i] = null;

            switch (type) {
                case Keyword.QR_CODE_TYPE_HOTSPOT: {
                    int pin = Integer.parseInt(values[1]);
                    String ssid = values[2];
                    String bssid = values[3];
                    String password = values[4];
                    run(new DeviceListAdapter.NetworkDescription(ssid, bssid, password), pin);
                }
                break;
                case Keyword.QR_CODE_TYPE_WIFI:
                    int pin = Integer.parseInt(values[1]);
                    String ssid = values[2];
                    String bssid = values[3];
                    String ip = values[4];
                    run(InetAddress.getByName(ip), bssid, pin);
                    break;
                default:
                    throw new Exception("Request is unknown");
            }
        } catch (UnknownHostException e) {
            showDialog(new AlertDialog.Builder(this)
                    .setMessage(R.string.mesg_unknownHostError)
                    .setNeutralButton(R.string.butn_close, null));
        } catch (Exception e) {
            e.printStackTrace();
            showDialog(new AlertDialog.Builder(this)
                    .setTitle(R.string.text_unrecognizedQrCode)
                    .setMessage(code)
                    .setNegativeButton(R.string.butn_close, null)
                    .setPositiveButton(R.string.butn_show, (dialog, which) -> {
                        TextStreamObject textObject = new TextStreamObject(AppUtils.getUniqueNumber(), code);
                        getDatabase().publish(textObject);
                        getDatabase().broadcast();

                        Toast.makeText(this, R.string.mesg_textStreamSaved, Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(this, TextEditorActivity.class)
                                .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                                .putExtra(TextEditorActivity.EXTRA_CLIPBOARD_ID, textObject.id));
                    })
                    .setNeutralButton(R.string.butn_copyToClipboard, (dialog, which) -> {
                        ClipboardManager manager = (ClipboardManager) getApplicationContext().getSystemService(
                                Service.CLIPBOARD_SERVICE);
                        if (manager != null) {
                            manager.setPrimaryClip(ClipData.newPlainText("copiedText", code));
                            Toast.makeText(this, R.string.mesg_textCopiedToClipboard, Toast.LENGTH_SHORT).show();
                        }
                    }));
        }
    }

    private void run(DeviceListAdapter.NetworkDescription description, int pin)
    {
        run(new DeviceIntroductionTask(description, pin));
    }

    private void run(InetAddress address, String bssid, int pin)
    {
        Runnable runnable = () -> run(new DeviceIntroductionTask(address, pin));
        WifiInfo wifiInfo = mConnections.getWifiManager().getConnectionInfo();

        if (wifiInfo != null && wifiInfo.getBSSID() != null && wifiInfo.getBSSID().equals(bssid)) {
            runnable.run();
        } else {
            showDialog(new AlertDialog.Builder(this)
                    .setMessage(R.string.mesg_errorNotSameNetwork)
                    .setNegativeButton(R.string.butn_cancel, null)
                    .setPositiveButton(R.string.butn_gotIt, (dialog, which) -> runnable.run())
                    .setOnDismissListener(mDismissListener));
        }
    }

    private void run(DeviceIntroductionTask task)
    {
        runUiTask(task, this);
    }

    private void showDialog(AlertDialog.Builder builder)
    {
        mBarcodeView.pauseAndWait();
        builder.setOnDismissListener(mDismissListener).show();
    }

    public void updateState(boolean connecting)
    {
        if (connecting) {
            // Keep showing barcode view
            mBarcodeView.pauseAndWait();
            setConductItemsShowing(false);
        } else {
            mBarcodeView.resume();
            updateState();
        }

        mTaskContainer.setVisibility(connecting ? View.VISIBLE : View.GONE);
        mTaskInterruptButton.setOnClickListener(connecting ? v -> {
            List<DeviceIntroductionTask> tasks = getTaskListOf(DeviceIntroductionTask.class);
            for (DeviceIntroductionTask task : tasks)
                task.interrupt(true);
        } : null);
    }

    public void updateState()
    {
        boolean wifiEnabled = mConnections.getWifiManager().isWifiEnabled();
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        // With Android Oreo, to gather Wi-Fi information, minimal access to location is needed
        boolean hasLocationPermission = Build.VERSION.SDK_INT < 23 || mConnections.canAccessLocation();
        boolean state = hasCameraPermission && (mShowAsText || (wifiEnabled && hasLocationPermission));

        if (hasTaskOf(DeviceIntroductionTask.class))
            return;

        if (!state) {
            mBarcodeView.pauseAndWait();

            if (!hasCameraPermission) {
                mConductImage.setImageResource(R.drawable.ic_camera_white_144dp);
                mConductText.setText(R.string.text_cameraPermissionRequired);
                mConductButton.setText(R.string.butn_ask);

                mConductButton.setOnClickListener(v -> ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA));

                if (!mPermissionRequestedCamera)
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                            REQUEST_PERMISSION_CAMERA);

                mPermissionRequestedCamera = true;
            } else if (!hasLocationPermission) {
                mConductImage.setImageResource(R.drawable.ic_perm_device_information_white_144dp);
                mConductText.setText(R.string.mesg_locationPermissionRequiredAny);
                mConductButton.setText(R.string.butn_enable);

                mConductButton.setOnClickListener(v -> mConnections.validateLocationPermission(this,
                        REQUEST_PERMISSION_LOCATION));

                if (!mPermissionRequestedLocation)
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_CAMERA);

                mPermissionRequestedLocation = true;
            } else {
                mConductImage.setImageResource(R.drawable.ic_signal_wifi_off_white_144dp);
                mConductText.setText(R.string.text_scanQRWifiRequired);
                mConductButton.setText(R.string.butn_enable);
                mConductButton.setOnClickListener(v -> mConnections.turnOnWiFi(this, this));
            }
        } else {
            mBarcodeView.resume();
            mConductText.setText(R.string.help_scanQRCode);
        }

        setConductItemsShowing(!state);
        mBarcodeView.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    protected void setConductItemsShowing(boolean showing)
    {
        mConductContainer.setVisibility(showing ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDeviceReached(DeviceRoute deviceRoute)
    {
        setResult(android.app.Activity.RESULT_OK, new Intent()
                .putExtra(BarcodeScannerActivity.EXTRA_DEVICE, deviceRoute.device)
                .putExtra(BarcodeScannerActivity.EXTRA_DEVICE_ADDRESS, deviceRoute.address));
        finish();
    }

    @Override
    public void onTaskStateChange(BaseAttachableAsyncTask task, AsyncTask.State state)
    {
        if (task instanceof DeviceIntroductionTask) {
            switch (state) {
                case Starting:
                    updateState(true);
                    break;
                case Finished:
                    updateState(false);
            }
        }
        updateState(!task.isFinished());
    }

    @Override
    public boolean onTaskMessage(TaskMessage message)
    {
        if (message.sizeOfActions() > 1)
            runOnUiThread(() -> message.toDialogBuilder(this).show());
        else if (message.sizeOfActions() <= 1)
            runOnUiThread(() -> message.toSnackbar(mTaskInterruptButton).show());
        else
            return false;

        return true;
    }
}
