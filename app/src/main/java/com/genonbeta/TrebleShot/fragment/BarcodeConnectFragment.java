package com.genonbeta.TrebleShot.fragment;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ConnectionManagerActivity;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.ui.UIConnectionUtils;
import com.genonbeta.TrebleShot.ui.UITask;
import com.genonbeta.TrebleShot.ui.callback.IconSupport;
import com.genonbeta.TrebleShot.ui.callback.NetworkDeviceSelectedListener;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.genonbeta.android.framework.util.Interrupter;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * created by: veli
 * date: 12/04/18 17:21
 */
public class BarcodeConnectFragment
        extends com.genonbeta.android.framework.app.Fragment
        implements TitleSupport, UITask, IconSupport, ConnectionManagerActivity.DeviceSelectionSupport
{
    public static final String TAG = "BarcodeConnectFragment";

    public static final int REQUEST_PERMISSION_CAMERA = 1;
    public static final int REQUEST_PERMISSION_LOCATION = 1;
    public static final int REQUEST_TURN_WIFI_ON = 2;

    private DecoratedBarcodeView mBarcodeView;
    private UIConnectionUtils mConnectionUtils;
    private ViewGroup mConductContainer;
    private TextView mConductText;
    private ImageView mConductImage;
    private Button mConductButton;
    private View mTaskContainer;
    private AppCompatButton mTaskInterruptButton;
    private IntentFilter mIntentFilter = new IntentFilter();
    private NetworkDeviceSelectedListener mDeviceSelectedListener;
    private boolean mPermissionRequested = false;
    private String mPreviousScanResult = null;

    private UIConnectionUtils.RequestWatcher mPermissionWatcher = new UIConnectionUtils.RequestWatcher()
    {
        @Override
        public void onResultReturned(boolean result, boolean shouldWait)
        {
            if (result)
                updateState();
            else
                mBarcodeView.pauseAndWait();

            // We don't want to keep this when the result is ok
            // or not asked to wait
            if (!shouldWait || result)
                mPreviousScanResult = null;
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
                    || ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction()))
                updateState();
        }
    };

    private NetworkDeviceLoader.OnDeviceRegisteredListener mRegisteredListener = new NetworkDeviceLoader.OnDeviceRegisteredListener()
    {
        @Override
        public void onDeviceRegistered(AccessDatabase database, final NetworkDevice device, final NetworkDevice.Connection connection)
        {
            if (mDeviceSelectedListener != null)
                mDeviceSelectedListener.onNetworkDeviceSelected(device, connection);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mConnectionUtils = new UIConnectionUtils(ConnectionUtils.getInstance(getContext()), this);

        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.layout_barcode_connect, container, false);

        mConductContainer = view.findViewById(R.id.layout_barcode_connect_conduct_container);
        mConductButton = view.findViewById(R.id.layout_barcode_connect_conduct_button);
        mBarcodeView = view.findViewById(R.id.layout_barcode_connect_barcode_view);
        mConductText = view.findViewById(R.id.layout_barcode_connect_conduct_text);
        mConductImage = view.findViewById(R.id.layout_barcode_connect_conduct_image);
        mTaskContainer = view.findViewById(R.id.container_task);
        mTaskInterruptButton = view.findViewById(R.id.task_interrupter_button);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.actions_barcode_scanner, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.show_help)
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.text_scanQRCodeHelp)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        else
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

        getContext().registerReceiver(mReceiver, mIntentFilter);
        updateState();

        if (mPreviousScanResult != null)
            handleBarcode(mPreviousScanResult);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        getContext().unregisterReceiver(mReceiver);
        mBarcodeView.pauseAndWait();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (permissions.length > 0)
            for (int permIterator = 0; permIterator < permissions.length; permIterator++) {
                if (Manifest.permission.CAMERA.equals(permissions[permIterator]) &&
                        grantResults[permIterator] == PackageManager.PERMISSION_GRANTED) {
                    updateState();
                    mPermissionRequested = false;
                }
            }
    }

    protected void handleBarcode(String code) {
        try {
            JSONObject jsonObject = new JSONObject(code);
            NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork = new NetworkDeviceListAdapter.HotspotNetwork();

            mPreviousScanResult = code; // Fail-safe

            final int accessPin = jsonObject.has(Keyword.NETWORK_PIN)
                    ? jsonObject.getInt(Keyword.NETWORK_PIN)
                    : -1;

            if (jsonObject.has(Keyword.NETWORK_NAME)) {
                hotspotNetwork.SSID = jsonObject.getString(Keyword.NETWORK_NAME);
                hotspotNetwork.qrConnection = true;

                boolean passProtected = jsonObject.has(Keyword.NETWORK_PASSWORD);

                if (passProtected) {
                    hotspotNetwork.password = jsonObject.getString(Keyword.NETWORK_PASSWORD);
                    hotspotNetwork.keyManagement = jsonObject.getInt(Keyword.NETWORK_KEYMGMT);
                }

                makeAcquaintance(hotspotNetwork, accessPin);
            } else if (jsonObject.has(Keyword.NETWORK_ADDRESS_IP)) {
                if (Build.VERSION.SDK_INT < 26 // With Android Oreo to gather Wi-Fi information, minimal access to location is needed
                        || mConnectionUtils.validateLocationPermission(getActivity(), REQUEST_PERMISSION_LOCATION, mPermissionWatcher)) {
                    final String bssid = jsonObject.getString(Keyword.NETWORK_ADDRESS_BSSID);
                    final String ipAddress = jsonObject.getString(Keyword.NETWORK_ADDRESS_IP);

                    WifiInfo wifiInfo = mConnectionUtils.getConnectionUtils().getWifiManager().getConnectionInfo();

                    if (wifiInfo != null
                            && wifiInfo.getBSSID() != null
                            && wifiInfo.getBSSID().equals(bssid))
                        makeAcquaintance(ipAddress, accessPin);
                    else {
                        mBarcodeView.pauseAndWait();

                        new AlertDialog.Builder(getActivity())
                                .setMessage(R.string.mesg_errorNotSameNetwork)
                                .setNegativeButton(R.string.butn_close, null)
                                .setPositiveButton(R.string.butn_skip, new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        makeAcquaintance(ipAddress, accessPin);
                                    }
                                })
                                .setOnDismissListener(new DialogInterface.OnDismissListener()
                                {
                                    @Override
                                    public void onDismiss(DialogInterface dialog)
                                    {
                                        updateState();
                                    }
                                })
                                .show();
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();

            mBarcodeView.pauseAndWait();

            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.mesg_somethingWentWrong)
                    .setPositiveButton(R.string.butn_close, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener()
                    {
                        @Override
                        public void onDismiss(DialogInterface dialog)
                        {
                            updateState();
                        }
                    })
                    .show();
        }
    }

    @Override
    public int getIconRes()
    {
        return R.drawable.ic_qrcode_white_24dp;
    }

    @Override
    public CharSequence getTitle(Context context)
    {
        return context.getString(R.string.text_scanQrCode);
    }

    protected void makeAcquaintance(Object object, int accessPin)
    {
        mConnectionUtils.makeAcquaintance(getActivity(), AppUtils.getDatabase(getContext()), BarcodeConnectFragment.this, object, accessPin, mRegisteredListener);
    }

    public void setDeviceSelectedListener(NetworkDeviceSelectedListener listener)
    {
        mDeviceSelectedListener = listener;
    }

    public void updateState(boolean isConnecting, final Interrupter interrupter)
    {
        if (isConnecting) {
            // Keep showing barcode view
            mBarcodeView.pauseAndWait();
            setConductItemsShowing(false);
        } else {
            mBarcodeView.resume();
            updateState();
        }

        mTaskContainer.setVisibility(isConnecting ? View.VISIBLE : View.GONE);

        mTaskInterruptButton.setOnClickListener(isConnecting ? new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                interrupter.interrupt();
            }
        } : null);
    }

    public void updateState()
    {
        final boolean wifiEnabled = mConnectionUtils.getConnectionUtils().getWifiManager().isWifiEnabled();
        final boolean hasPermissions = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        final boolean state = wifiEnabled && hasPermissions;

        if (!state) {
            mBarcodeView.pauseAndWait();

            if (!hasPermissions) {
                mConductImage.setImageResource(R.drawable.ic_camera_white_144dp);
                mConductText.setText(R.string.text_cameraPermissionRequired);
                mConductButton.setText(R.string.butn_ask);

                mConductButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
                    }
                });

                if (!mPermissionRequested)
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);

                mPermissionRequested = true;
            } else {
                mConductImage.setImageResource(R.drawable.ic_signal_wifi_off_white_144dp);
                mConductText.setText(R.string.text_scanQRWifiRequired);
                mConductButton.setText(R.string.butn_enable);

                mConductButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        mConnectionUtils.turnOnWiFi(getActivity(), REQUEST_TURN_WIFI_ON, mPermissionWatcher);
                    }
                });
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

    @Override
    public void updateTaskStarted(Interrupter interrupter)
    {
        updateState(true, interrupter);
    }

    @Override
    public void updateTaskStopped()
    {
        updateState(false, null);
    }
}
