package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ConnectionManagerActivity;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.ui.UIConnectionUtils;
import com.genonbeta.TrebleShot.ui.callback.IconSupport;
import com.genonbeta.TrebleShot.ui.callback.NetworkDeviceSelectedListener;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.NetworkUtils;
import com.genonbeta.android.framework.app.Fragment;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;

public class NetworkManagerFragment
        extends Fragment
        implements TitleSupport, IconSupport, ConnectionManagerActivity.DeviceSelectionSupport
{
    public static final int REQUEST_LOCATION_PERMISSION = 643;

    private IntentFilter mIntentFilter = new IntentFilter();
    private StatusReceiver mStatusReceiver = new StatusReceiver();
    private UIConnectionUtils mConnectionUtils;

    private View mContainerText1;
    private View mContainerText2;
    private View mContainerText3;
    private ImageView mTextIcon1;
    private ImageView mTextIcon2;
    private ImageView mTextIcon3;
    private AppCompatTextView mText1;
    private TextView mText2;
    private TextView mText3;
    private ImageView mCodeView;
    private NetworkDeviceSelectedListener mDeviceSelectedListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = getLayoutInflater().inflate(R.layout.layout_network_manager, container, false);

        mCodeView = view.findViewById(R.id.layout_network_manager_qr_image);
        mContainerText1 = view.findViewById(R.id.layout_network_manager_info_container_text1_container);
        mContainerText2 = view.findViewById(R.id.layout_network_manager_info_container_text2_container);
        mContainerText3 = view.findViewById(R.id.layout_network_manager_info_container_text3_container);
        mTextIcon1 = view.findViewById(R.id.layout_network_manager_info_container_text1_icon);
        mTextIcon2 = view.findViewById(R.id.layout_network_manager_info_container_text2_icon);
        mTextIcon3 = view.findViewById(R.id.layout_network_manager_info_container_text3_icon);
        mText1 = view.findViewById(R.id.layout_network_manager_info_container_text1);
        mText2 = view.findViewById(R.id.layout_network_manager_info_container_text2);
        mText3 = view.findViewById(R.id.layout_network_manager_info_container_text3);

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (REQUEST_LOCATION_PERMISSION == requestCode)
            getUIConnectionUtils().showConnectionOptions(getActivity(), REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        getContext().registerReceiver(mStatusReceiver, mIntentFilter);
        updateState();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        getContext().unregisterReceiver(mStatusReceiver);
    }

    public ConnectionUtils getConnectionUtils()
    {
        return getUIConnectionUtils().getConnectionUtils();
    }

    @Override
    public int getIconRes()
    {
        return R.drawable.ic_wifi_white_24dp;
    }


    public UIConnectionUtils getUIConnectionUtils()
    {
        if (mConnectionUtils == null)
            mConnectionUtils = new UIConnectionUtils(ConnectionUtils.getInstance(getContext()), this);

        return mConnectionUtils;
    }

    @Override
    public CharSequence getTitle(Context context)
    {
        return context.getString(R.string.text_network);
    }

    public void setDeviceSelectedListener(NetworkDeviceSelectedListener listener)
    {
        mDeviceSelectedListener = listener;
    }

    public void updateViewsWithBlank()
    {
        updateViews(null, getString(R.string.text_qrCodeHotspotDisabledHelp), null, null);
    }

    // for connection addressing purpose
    public void updateViews(String networkName, String ipAddress, String bssid)
    {
        mTextIcon2.setImageResource(R.drawable.ic_wifi_white_24dp);
        mTextIcon3.setImageResource(R.drawable.ic_device_hub_white_24dp);

        try {
            JSONObject object = new JSONObject()
                    .put(Keyword.NETWORK_ADDRESS_IP, ipAddress)
                    .put(Keyword.NETWORK_ADDRESS_BSSID, bssid);

            updateViews(object, getString(R.string.text_easyDiscoveryHelp), networkName, ipAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateViews(@Nullable JSONObject codeIndex, @Nullable String text1, @Nullable String text2, @Nullable String text3)
    {
        String codeString = codeIndex == null ? null : codeIndex.toString();
        boolean showQRCode = codeString != null && getContext() != null;

        try {
            if (codeIndex != null) {
                int networkPin = AppUtils.getUniqueNumber();

                codeIndex.put(Keyword.NETWORK_PIN, networkPin);

                AppUtils.getDefaultPreferences(getContext()).edit()
                        .putInt(Keyword.NETWORK_PIN, networkPin)
                        .apply();
            }

            MultiFormatWriter formatWriter = new MultiFormatWriter();

            if (showQRCode) {
                BitMatrix bitMatrix = formatWriter.encode(codeString, BarcodeFormat.QR_CODE, 400, 400);
                BarcodeEncoder encoder = new BarcodeEncoder();

                Bitmap bitmap = encoder.createBitmap(bitMatrix);

                GlideApp.with(getContext())
                        .load(bitmap)
                        .into(mCodeView);
            } else
                mCodeView.setImageResource(R.drawable.ic_qrcode_white_128dp);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mContainerText1.setVisibility(text1 == null ? View.GONE : View.VISIBLE);
            mContainerText2.setVisibility(text2 == null ? View.GONE : View.VISIBLE);
            mContainerText3.setVisibility(text3 == null ? View.GONE : View.VISIBLE);

            mText1.setText(text1);
            mText2.setText(text2);
            mText3.setText(text3);
        }
    }

    public void updateState()
    {
        WifiInfo connectionInfo = getConnectionUtils().getWifiManager().getConnectionInfo();

        if (!getConnectionUtils().isConnectedToAnyNetwork())
            updateViewsWithBlank();
        else {
            updateViews(ConnectionUtils.getCleanNetworkName(connectionInfo.getSSID()),
                    NetworkUtils.convertInet4Address(connectionInfo.getIpAddress()),
                    connectionInfo.getBSSID());
        }
    }

    private class StatusReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
                    || ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction()))
                updateState();
        }
    }
}
