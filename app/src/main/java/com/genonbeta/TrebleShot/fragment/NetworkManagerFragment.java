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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.ui.callback.IconProvider;
import com.genonbeta.TrebleShot.ui.callback.TitleProvider;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.HotspotManager;
import com.genonbeta.TrebleShot.util.InetAddresses;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * created by: veli
 * date: 11/04/18 20:53
 */
public class NetworkManagerFragment extends com.genonbeta.android.framework.app.Fragment implements IconProvider,
        TitleProvider
{
    public static final int REQUEST_LOCATION_PERMISSION = 1;
    public static final int REQUEST_LOCATION_PERMISSION_FOR_HOTSPOT = 2;

    public static final String WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED";

    private final IntentFilter mIntentFilter = new IntentFilter();
    private final StatusReceiver mStatusReceiver = new StatusReceiver();
    private ConnectionUtils mConnectionUtils;

    private View mContainerText1;
    private View mContainerText2;
    private View mContainerText3;
    private TextView mCodeText;
    private TextView mText1;
    private TextView mText2;
    private TextView mText3;
    private ImageView mImageView2;
    private ImageView mImageView3;
    private ImageView mCodeView;
    private Button mToggleButton;
    private Button mSecondButton;
    private MenuItem mHelpMenuItem;
    private ColorStateList mColorPassiveState;
    private HotspotManager mManager;
    private Type mActiveType;
    private ColorStateList mToggleButtonDefaultStateList;
    private ColorStateList mToggleButtonEnabledStateList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mConnectionUtils = new ConnectionUtils(requireContext());
        mManager = HotspotManager.newInstance(requireContext());
        mIntentFilter.addAction(BackgroundService.ACTION_PIN_USED);
        mIntentFilter.addAction(WIFI_AP_STATE_CHANGED);
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(BackgroundService.ACTION_PIN_USED);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        return getLayoutInflater().inflate(R.layout.layout_hotspot_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        mToggleButtonEnabledStateList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                AppUtils.getReference(requireContext(), R.attr.colorError)));
        mColorPassiveState = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), AppUtils.getReference(
                requireContext(), R.attr.colorPassive)));
        mCodeView = view.findViewById(R.id.layout_network_manager_qr_image);
        mCodeText = view.findViewById(R.id.layout_network_manager_qr_help_text);
        mToggleButton = view.findViewById(R.id.layout_network_manager_info_toggle_button);
        mSecondButton = view.findViewById(R.id.layout_network_manager_info_second_toggle_button);
        mContainerText1 = view.findViewById(R.id.layout_netowrk_manager_info_container_text1_container);
        mContainerText2 = view.findViewById(R.id.layout_network_manager_info_container_text2_container);
        mContainerText3 = view.findViewById(R.id.layout_network_manager_info_container_text3_container);
        mText1 = view.findViewById(R.id.layout_network_manager_info_container_text1);
        mText2 = view.findViewById(R.id.layout_network_manager_info_container_text2);
        mText3 = view.findViewById(R.id.layout_network_manager_info_container_text3);
        mImageView2 = view.findViewById(R.id.layout_network_manager_info_container_text2_icon);
        mImageView3 = view.findViewById(R.id.layout_network_manager_info_container_text3_icon);
        mToggleButtonDefaultStateList = mToggleButton.getBackgroundTintList();

        mToggleButton.setOnClickListener(this::toggle);
        mSecondButton.setOnClickListener(this::toggle);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.actions_hotspot_manager, menu);
        mHelpMenuItem = menu.findItem(R.id.show_help);

        showMenu();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.show_help && mManager.getConfiguration() != null) {
            String hotspotName = mManager.getConfiguration().SSID;
            String friendlyName = AppUtils.getFriendlySSID(hotspotName);

            new AlertDialog.Builder(requireActivity())
                    .setMessage(getString(R.string.mesg_hotspotCreatedInfo, hotspotName, friendlyName))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (REQUEST_LOCATION_PERMISSION_FOR_HOTSPOT == requestCode)
            toggleHotspot();
        else if (REQUEST_LOCATION_PERMISSION == requestCode)
            updateState();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        requireContext().registerReceiver(mStatusReceiver, mIntentFilter);
        updateState();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        requireContext().unregisterReceiver(mStatusReceiver);
    }

    @Override
    public int getIconRes()
    {
        return R.drawable.ic_qrcode_white_24dp;
    }

    @Override
    public CharSequence getDistinctiveTitle(Context context)
    {
        return context.getString(R.string.butn_generateQrCode);
    }

    @Nullable
    public WifiConfiguration getWifiConfiguration()
    {
        if (Build.VERSION.SDK_INT < 26)
            return mManager.getConfiguration();

        try {
            return AppUtils.getBgService(requireActivity()).getHotspotConfig();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void openWifiSettings()
    {
        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
    }

    private void toggleHotspot()
    {
        mConnectionUtils.toggleHotspot(requireActivity(), this, mManager, true,
                REQUEST_LOCATION_PERMISSION_FOR_HOTSPOT);
    }

    public void toggle(View v)
    {
        if (v.getId() == R.id.layout_network_manager_info_toggle_button) {
            switch (mActiveType) {
                case LocationPermissionNeeded:
                    mConnectionUtils.validateLocationPermission(getActivity(), REQUEST_LOCATION_PERMISSION);
                    break;
                case WiFi:
                case HotspotExternal:
                    openWifiSettings();
                    break;
                case Hotspot:
                case None:
                default:
                    toggleHotspot();
            }
        } else if (v.getId() == R.id.layout_network_manager_info_second_toggle_button) {
            switch (mActiveType) {
                case LocationPermissionNeeded:
                case WiFi:
                    toggleHotspot();
                    break;
                case HotspotExternal:
                case Hotspot:
                case None:
                default:
                    openWifiSettings();
            }
        }
    }

    private void showMenu()
    {
        if (mHelpMenuItem != null)
            mHelpMenuItem.setVisible(mManager.getConfiguration() != null && mManager.isEnabled());
    }

    private void updateState()
    {
        showMenu();
        try {
            updateViews();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateViews() throws JSONException
    {
        showMenu();
        JSONObject json = new JSONObject();
        WifiConfiguration config = getWifiConfiguration();
        WifiInfo connectionInfo = mConnectionUtils.getWifiManager().getConnectionInfo();

        if (mManager.isEnabled()) {
            if (config != null) {
                mActiveType = Type.Hotspot;
                String ssid = config.SSID;
                String bssid = config.BSSID;
                String key = config.preSharedKey;

                json.put(Keyword.NETWORK_SSID, ssid)
                        .put(Keyword.NETWORK_BSSID, bssid)
                        .put(Keyword.NETWORK_PASSWORD, key);

                mImageView2.setImageResource(R.drawable.ic_wifi_tethering_white_24dp);
                mImageView3.setImageResource(R.drawable.ic_vpn_key_white_24dp);
                mText1.setText(R.string.text_qrCodeAvailableHelp);
                mText2.setText(ssid);
                mText3.setText(key);
            } else {
                mActiveType = Type.HotspotExternal;
                mText1.setText(R.string.text_hotspotStartedExternallyNotice);
            }
            mToggleButton.setText(R.string.butn_stopHotspot);
            mSecondButton.setText(R.string.butn_wifiSettings);
        } else if (!mConnectionUtils.canReadWifiInfo() && mConnectionUtils.getWifiManager().isWifiEnabled()) {
            mActiveType = Type.LocationPermissionNeeded;
            mText1.setText(R.string.mesg_locationPermissionRequiredAny);
            mToggleButton.setText(R.string.butn_enable);
            mSecondButton.setText(R.string.text_startHotspot);
        } else if (mConnectionUtils.isConnectedToAnyNetwork()) {
            mActiveType = Type.WiFi;
            String hostAddress;

            try {
                hostAddress = InetAddress.getByAddress(InetAddresses.toByteArray(connectionInfo.getIpAddress()))
                        .getHostAddress();
            } catch (UnknownHostException e) {
                hostAddress = "0.0.0.0";
            }

            json.put(Keyword.NETWORK_ADDRESS_IP, hostAddress)
                    .put(Keyword.NETWORK_BSSID, connectionInfo.getBSSID());

            mImageView2.setImageResource(R.drawable.ic_wifi_white_24dp);
            mImageView3.setImageResource(R.drawable.ic_ip_white_24dp);
            mText1.setText(R.string.help_scanQRCode);
            mText2.setText(ConnectionUtils.getCleanNetworkName(connectionInfo.getSSID()));
            mText3.setText(hostAddress);
            mToggleButton.setText(R.string.butn_wifiSettings);
            mSecondButton.setText(R.string.text_startHotspot);
        } else {
            mActiveType = Type.None;
            mText1.setText(R.string.help_setUpNetwork);
            mToggleButton.setText(R.string.text_startHotspot);
            mSecondButton.setText(R.string.butn_wifiSettings);
        }

        switch (mActiveType) {
            case Hotspot:
            case WiFi:
            case HotspotExternal:
                mToggleButton.setBackgroundTintList(mToggleButtonEnabledStateList);
                break;
            default:
                mToggleButton.setBackgroundTintList(mToggleButtonDefaultStateList);
        }

        switch (mActiveType) {
            case LocationPermissionNeeded:
            case None:
            case HotspotExternal:
                mText2.setText(null);
                mText3.setText(null);
        }

        mContainerText1.setVisibility(mText1.length() > 0 ? View.VISIBLE : View.GONE);
        mContainerText2.setVisibility(mText2.length() > 0 ? View.VISIBLE : View.GONE);
        mContainerText3.setVisibility(mText3.length() > 0 ? View.VISIBLE : View.GONE);

        boolean showQRCode = json.length() > 0 && getContext() != null;

        try {
            if (showQRCode) {
                json.put(Keyword.NETWORK_PIN, AppUtils.generateNetworkPin(getContext()));

                MultiFormatWriter formatWriter = new MultiFormatWriter();
                BitMatrix bitMatrix = formatWriter.encode(json.toString(), BarcodeFormat.QR_CODE, 400,
                        400);
                BarcodeEncoder encoder = new BarcodeEncoder();
                Bitmap bitmap = encoder.createBitmap(bitMatrix);

                GlideApp.with(getContext())
                        .load(bitmap)
                        .into(mCodeView);
            } else
                mCodeView.setImageResource(R.drawable.ic_qrcode_white_128dp);

            mCodeText.setVisibility(showQRCode ? View.GONE : View.VISIBLE);
            ImageViewCompat.setImageTintList(mCodeView, showQRCode ? null : mColorPassiveState);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class StatusReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // FIXME: 25.03.2020 Doesn't get called when the hotspot state is changed
            if (WIFI_AP_STATE_CHANGED.equals(intent.getAction())
                    || BackgroundService.ACTION_PIN_USED.equals(intent.getAction())
                    || WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
                    || ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())
                    || BackgroundService.ACTION_PIN_USED.equals(intent.getAction()))
                updateState();
        }
    }

    private enum Type
    {
        None,
        WiFi,
        Hotspot,
        HotspotExternal,
        LocationPermissionNeeded
    }
}
