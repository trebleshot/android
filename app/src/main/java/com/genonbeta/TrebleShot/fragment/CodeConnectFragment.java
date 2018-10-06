package com.genonbeta.TrebleShot.fragment;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.adapter.SmartFragmentPagerAdapter;
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
import com.journeyapps.barcodescanner.BarcodeView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * created by: veli
 * date: 12/04/18 17:21
 */
public class CodeConnectFragment
        extends com.genonbeta.android.framework.app.Fragment
        implements TitleSupport, UITask, IconSupport, SmartFragmentPagerAdapter.ShowingChangeListener
{
    public static final String TAG = "CodeConnectFragment";

    public static final int REQUEST_PERMISSION_CAMERA = 1;

    private SmartFragmentPagerAdapter.TabSelectionOracle mTabSelectionOracle = new SmartFragmentPagerAdapter.TabSelectionOracle(this);
    private BarcodeView mBarcodeView;
    private UIConnectionUtils mConnectionUtils;
    private TextView mConductText;
    private ImageView mConductImage;
    private View mConductContainer;
    private View mTaskContainer;
    private AppCompatButton mTaskInterruptButton;
    private IntentFilter mIntentFilter = new IntentFilter();
    private NetworkDeviceSelectedListener mDeviceSelectedListener;
    private boolean mPermissionRequested = false;

    private Snackbar.Callback mWaitedSnackbarCallback = new Snackbar.Callback()
    {
        @Override
        public void onDismissed(Snackbar transientBottomBar, int event)
        {
            super.onDismissed(transientBottomBar, event);
            updateState();
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.layout_code_connect, container, false);

        mBarcodeView = view.findViewById(R.id.layout_code_connect_barcode_view);
        mConductText = view.findViewById(R.id.layout_code_connect_conduct_text);
        mConductImage = view.findViewById(R.id.layout_code_connect_conduct_image);
        mConductContainer = view.findViewById(R.id.container_conduct);
        mTaskContainer = view.findViewById(R.id.container_task);
        mTaskInterruptButton = view.findViewById(R.id.task_interrupter_button);

        return view;
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
                try {
                    JSONObject jsonObject = new JSONObject(result.getResult().getText());
                    NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork = new NetworkDeviceListAdapter.HotspotNetwork();

                    int accessPin = jsonObject.has(Keyword.NETWORK_PIN)
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

                        mConnectionUtils.makeAcquaintance(getContext(), AppUtils.getDatabase(getContext()), CodeConnectFragment.this, hotspotNetwork, accessPin, mRegisteredListener);
                    } else if (jsonObject.has(Keyword.NETWORK_ADDRESS_IP)) {
                        String bssid = jsonObject.getString(Keyword.NETWORK_ADDRESS_BSSID);
                        String ipAddress = jsonObject.getString(Keyword.NETWORK_ADDRESS_IP);

                        WifiInfo wifiInfo = mConnectionUtils.getConnectionUtils().getWifiManager().getConnectionInfo();

                        if (wifiInfo != null
                                && wifiInfo.getBSSID() != null
                                && wifiInfo.getBSSID().equals(bssid))
                            mConnectionUtils.makeAcquaintance(getContext(), AppUtils.getDatabase(getContext()), CodeConnectFragment.this, ipAddress, accessPin, mRegisteredListener);
                        else {
                            mBarcodeView.pauseAndWait();

                            createSnackbar(R.string.mesg_errorNotSameNetwork)
                                    .addCallback(mWaitedSnackbarCallback)
                                    .show();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();

                    createSnackbar(R.string.mesg_somethingWentWrong)
                            .show();
                }
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

        mTabSelectionOracle.cycle(true);
        getContext().registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        mTabSelectionOracle.cycle(false);
        getContext().unregisterReceiver(mReceiver);
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

    @Override
    public void onNotifyShowingChange()
    {
        if (mTabSelectionOracle.isResuming())
            updateState();
        else
            mBarcodeView.pauseAndWait();
    }

    @Override
    public int getIconRes()
    {
        return R.drawable.ic_qrcode_white_24dp;
    }


    @Override
    public SmartFragmentPagerAdapter.TabSelectionOracle getTabSelectionOracle()
    {
        return mTabSelectionOracle;
    }

    @Override
    public CharSequence getTitle(Context context)
    {
        return context.getString(R.string.text_connect);
    }

    public void setDeviceSelectedListener(NetworkDeviceSelectedListener listener)
    {
        mDeviceSelectedListener = listener;
    }

    public void updateState()
    {
        final boolean wifiEnabled = mConnectionUtils.getConnectionUtils().getWifiManager().isWifiEnabled();

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            mConductImage.setImageResource(R.drawable.ic_camera_white_144dp);
            mConductText.setText(R.string.text_cameraPermissionRequired);

            if (!mPermissionRequested)
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);

            mPermissionRequested = true;
        } else {
            mConductImage.setImageResource(wifiEnabled
                    ? R.drawable.ic_crop_free_white_144dp
                    : R.drawable.ic_signal_wifi_off_white_144dp);

            mConductText.setText(wifiEnabled
                    ? R.string.text_scanQRCodeHelp
                    : R.string.text_scanQRWifiRequired);

            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    if (wifiEnabled)
                        mBarcodeView.resume();
                    else
                        mBarcodeView.pauseAndWait();
                }
            }, 300);
        }
    }

    @Override
    public void updateTaskStarted(final Interrupter interrupter)
    {
        mBarcodeView.pauseAndWait();
        mConductContainer.setVisibility(View.GONE);
        mTaskContainer.setVisibility(View.VISIBLE);

        mTaskInterruptButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                interrupter.interrupt();
            }
        });
    }

    @Override
    public void updateTaskStopped()
    {
        mBarcodeView.resume();
        mConductContainer.setVisibility(View.VISIBLE);
        mTaskContainer.setVisibility(View.GONE);

        mTaskInterruptButton.setOnClickListener(null);
    }
}
