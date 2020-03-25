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

package com.genonbeta.TrebleShot.fragment;

import android.Manifest;
import android.app.Service;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.TextEditorActivity;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter.InfoHolder;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter.NetworkDescription;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask;
import com.genonbeta.TrebleShot.ui.UIConnectionUtils;
import com.genonbeta.TrebleShot.ui.callback.IconProvider;
import com.genonbeta.TrebleShot.ui.callback.TitleProvider;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.android.framework.app.Fragment;
import com.genonbeta.android.framework.util.Stoppable;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import static com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter.NetworkSuggestion;

/**
 * created by: veli
 * date: 12/04/18 17:21
 */
public class BarcodeConnectFragment extends Fragment implements TitleProvider, IconProvider
{
    public static final String TAG = "BarcodeConnectFragment";

    public static final int REQUEST_PERMISSION_CAMERA = 1;
    public static final int REQUEST_PERMISSION_LOCATION = 2;
    public static final int REQUEST_TURN_WIFI_ON = 4;

    private DecoratedBarcodeView mBarcodeView;
    private UIConnectionUtils mConnectionUtils;
    private ViewGroup mConductContainer;
    private TextView mConductText;
    private ImageView mConductImage;
    private ImageView mTextModeIndicator;
    private Button mConductButton;
    private Button mTaskInterruptButton;
    private View mTaskContainer;
    private IntentFilter mIntentFilter = new IntentFilter();
    private boolean mPermissionRequestedCamera = false;
    private boolean mPermissionRequestedLocation = false;
    private boolean mShowAsText = false;
    //private String mPreviousScanResult = null;

    private UIConnectionUtils.RequestWatcher mPermissionWatcher = new UIConnectionUtils.RequestWatcher()
    {
        @Override
        public void onResultReturned(boolean result, boolean shouldWait)
        {
            if (isResumed()) // isResumed
                updateState();
            else
                mBarcodeView.pauseAndWait();

            // We don't want to keep this when the result is ok
            // or not asked to wait
            //if (!shouldWait || result)
            //    mPreviousScanResult = null;
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver()
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
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mConnectionUtils = new UIConnectionUtils(ConnectionUtils.getInstance(getContext()), this);

        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.layout_barcode_connect, container, false);

        mConductContainer = view.findViewById(R.id.layout_barcode_connect_conduct_container);
        mTextModeIndicator = view.findViewById(R.id.layout_barcode_connect_mode_text_indicator);
        mConductButton = view.findViewById(R.id.layout_barcode_connect_conduct_button);
        mBarcodeView = view.findViewById(R.id.layout_barcode_connect_barcode_view);
        mConductText = view.findViewById(R.id.layout_barcode_connect_conduct_text);
        mConductImage = view.findViewById(R.id.layout_barcode_connect_conduct_image);
        mTaskContainer = view.findViewById(R.id.container_task);
        mTaskInterruptButton = view.findViewById(R.id.task_interrupter_button);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.actions_barcode_scanner, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.show_help)
            new AlertDialog.Builder(requireActivity())
                    .setMessage(R.string.text_scanQRCodeHelp)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        else if (id == R.id.change_mode) {
            mShowAsText = !mShowAsText;
            mTextModeIndicator.setVisibility(mShowAsText ? View.VISIBLE : View.GONE);
            item.setIcon(mShowAsText ? R.drawable.ic_qrcode_white_24dp : R.drawable.ic_short_text_white_24dp);

            createSnackbar(mShowAsText ? R.string.mesg_qrScannerTextMode : R.string.mesg_qrScannerDefaultMode)
                    .show();

            updateState();
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

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
    }

    @Override
    public void onResume()
    {
        super.onResume();

        requireContext().registerReceiver(mReceiver, mIntentFilter);
        updateState();

        //if (mPreviousScanResult != null)
        //    handleBarcode(mPreviousScanResult);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        requireContext().unregisterReceiver(mReceiver);
        mBarcodeView.pauseAndWait();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (permissions.length > 0)
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.CAMERA.equals(permissions[i]) &&
                        grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    updateState();
                    mPermissionRequestedCamera = false;
                }
            }
    }

    protected void handleBarcode(final String code)
    {
        final DialogInterface.OnDismissListener dismissListener = dialog -> updateState();

        try {
            //mPreviousScanResult = code; // Fail-safe
            if (mShowAsText)
                throw new JSONException("Showing as text.");

            JSONObject jsonObject = new JSONObject(code);
            final int accessPin = jsonObject.has(Keyword.NETWORK_PIN) ? jsonObject.getInt(Keyword.NETWORK_PIN) : -1;

            if (jsonObject.has(Keyword.NETWORK_SSID)) {
                String ssid = jsonObject.getString(Keyword.NETWORK_SSID);
                String bssid = null;
                String password = null;
                InfoHolder informer;

                if (jsonObject.has(Keyword.NETWORK_BSSID))
                    bssid = jsonObject.getString(Keyword.NETWORK_BSSID);

                if (jsonObject.has(Keyword.NETWORK_PASSWORD))
                    password = jsonObject.getString(Keyword.NETWORK_PASSWORD);

                if (Build.VERSION.SDK_INT >= 29) {
                    // TODO: 1/1/20 Ensure using both wpa2 and wpa3 does not cause an issue
                    WifiNetworkSuggestion.Builder builder = new WifiNetworkSuggestion.Builder()
                            .setSsid(ssid);

                    if (password != null)
                        builder.setWpa2Passphrase(password)
                                .setWpa3Passphrase(password);

                    if (bssid != null)
                        builder.setBssid(MacAddress.fromString(bssid));

                    informer = new InfoHolder(new NetworkSuggestion(ssid, builder.build()));
                } else
                    informer = new InfoHolder(new NetworkDescription(ssid, bssid, password));

                makeAcquaintance(informer, accessPin);
            } else if (jsonObject.has(Keyword.NETWORK_ADDRESS_IP)) {
                final String bssid = jsonObject.getString(Keyword.NETWORK_BSSID);
                final String ipAddress = jsonObject.getString(Keyword.NETWORK_ADDRESS_IP);

                WifiInfo wifiInfo = mConnectionUtils.getConnectionUtils().getWifiManager().getConnectionInfo();
                Runnable runnable = () -> {
                    try {
                        makeAcquaintance(new InfoHolder(InetAddress.getByName(ipAddress)), accessPin);
                    } catch (UnknownHostException e) {
                        new AlertDialog.Builder(requireActivity())
                                .setMessage(R.string.mesg_unknownHostError)
                                .setNeutralButton(R.string.butn_close, null)
                                .show();
                    }
                };

                if (wifiInfo != null && wifiInfo.getBSSID() != null && wifiInfo.getBSSID().equals(bssid)) {
                    runnable.run();
                } else {
                    mBarcodeView.pauseAndWait();

                    new AlertDialog.Builder(requireActivity())
                            .setMessage(R.string.mesg_errorNotSameNetwork)
                            .setNegativeButton(R.string.butn_close, null)
                            .setPositiveButton(R.string.butn_skip, (dialog, which) -> runnable.run())
                            .setOnDismissListener(dismissListener)
                            .show();
                }
            } else {
                throw new JSONException("Failed to attain known variables.");
            }
        } catch (JSONException e) {
            e.printStackTrace();

            mBarcodeView.pauseAndWait();

            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.text_unrecognizedQrCode)
                    .setMessage(code)
                    .setNegativeButton(R.string.butn_close, null)
                    .setPositiveButton(R.string.butn_show, (dialog, which) -> {
                        TextStreamObject textObject = new TextStreamObject(AppUtils.getUniqueNumber(), code);
                        AppUtils.getKuick(getContext()).publish(textObject);
                        AppUtils.getKuick(getContext()).broadcast();

                        Toast.makeText(getContext(), R.string.mesg_textStreamSaved, Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(getContext(), TextEditorActivity.class)
                                .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                                .putExtra(TextEditorActivity.EXTRA_CLIPBOARD_ID, textObject.id));
                    })
                    .setNeutralButton(R.string.butn_copyToClipboard, (dialog, which) -> {
                        if (getContext() != null) {
                            ClipboardManager manager = (ClipboardManager) getContext().getSystemService(
                                    Service.CLIPBOARD_SERVICE);
                            if (manager != null) {
                                manager.setPrimaryClip(ClipData.newPlainText("copiedText", code));
                                Toast.makeText(getContext(), R.string.mesg_textCopiedToClipboard, Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setOnDismissListener(dismissListener)
                    .show();
        }
    }

    @Override
    public int getIconRes()
    {
        return R.drawable.ic_qrcode_white_24dp;
    }

    @Override
    public CharSequence getDistinctiveTitle(Context context)
    {
        return context.getString(R.string.text_scanQrCode);
    }

    protected void makeAcquaintance(InfoHolder infoHolder, int pin)
    {
        BackgroundService.run(requireActivity(), new DeviceIntroductionTask(infoHolder, pin));
    }

    // TODO: 21.03.2020 Reimplement this
    public void updateState(boolean connecting, final Stoppable stoppable)
    {
        if (!isAdded()) {
            mBarcodeView.pauseAndWait();
            return;
        }

        if (connecting) {
            // Keep showing barcode view
            mBarcodeView.pauseAndWait();
            setConductItemsShowing(false);
        } else {
            mBarcodeView.resume();
            updateState();
        }

        mTaskContainer.setVisibility(connecting ? View.VISIBLE : View.GONE);
        mTaskInterruptButton.setOnClickListener(connecting ? (View.OnClickListener) v -> stoppable.interrupt() : null);
    }

    public void updateState()
    {
        if (!isAdded())
            return;

        final boolean wifiEnabled = mConnectionUtils.getConnectionUtils().getWifiManager().isWifiEnabled();
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        // With Android Oreo, to gather Wi-Fi information, minimal access to location is needed
        final boolean hasLocationPermission = Build.VERSION.SDK_INT < 23
                || mConnectionUtils.getConnectionUtils().canAccessLocation();
        final boolean state = (wifiEnabled || mShowAsText) && hasCameraPermission && hasLocationPermission;

        if (!state) {
            mBarcodeView.pauseAndWait();

            if (!hasCameraPermission) {
                mConductImage.setImageResource(R.drawable.ic_camera_white_144dp);
                mConductText.setText(R.string.text_cameraPermissionRequired);
                mConductButton.setText(R.string.butn_ask);

                mConductButton.setOnClickListener(v -> ActivityCompat.requestPermissions(requireActivity(), new String[]{
                        Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA));

                if (!mPermissionRequestedCamera)
                    ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA},
                            REQUEST_PERMISSION_CAMERA);

                mPermissionRequestedCamera = true;
            } else if (!hasLocationPermission) {
                mConductImage.setImageResource(R.drawable.ic_perm_device_information_white_144dp);
                mConductText.setText(R.string.mesg_locationPermissionRequiredAny);
                mConductButton.setText(R.string.butn_enable);

                mConductButton.setOnClickListener(v -> mConnectionUtils.validateLocationPermission(getActivity(),
                        REQUEST_PERMISSION_LOCATION, mPermissionWatcher));

                if (!mPermissionRequestedLocation)
                    ActivityCompat.requestPermissions(requireActivity(), new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_CAMERA);

                mPermissionRequestedLocation = true;
            } else {
                mConductImage.setImageResource(R.drawable.ic_signal_wifi_off_white_144dp);
                mConductText.setText(R.string.text_scanQRWifiRequired);
                mConductButton.setText(R.string.butn_enable);

                mConductButton.setOnClickListener(v -> mConnectionUtils.turnOnWiFi(getActivity(), REQUEST_TURN_WIFI_ON,
                        mPermissionWatcher));
            }
        } else {
            mBarcodeView.resume();
            mConductText.setText(R.string.text_scanQRCodeHelp);
        }

        setConductItemsShowing(!state);
        mBarcodeView.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    protected void setConductItemsShowing(boolean showing)
    {
        mConductContainer.setVisibility(showing ? View.VISIBLE : View.GONE);
    }
}
