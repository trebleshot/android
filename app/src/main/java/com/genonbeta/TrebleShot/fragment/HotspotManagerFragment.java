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
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import org.json.JSONObject;

/**
 * created by: veli
 * date: 11/04/18 20:53
 */
public class HotspotManagerFragment extends com.genonbeta.android.framework.app.Fragment implements IconProvider,
        TitleProvider
{
    public static final int REQUEST_LOCATION_PERMISSION_FOR_HOTSPOT = 643;

    public static final String WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED";

    private IntentFilter mIntentFilter = new IntentFilter();
    private StatusReceiver mStatusReceiver = new StatusReceiver();
    private ConnectionUtils mConnectionUtils;

    private View mContainerText1;
    private View mContainerText2;
    private View mContainerText3;
    private TextView mText1;
    private TextView mText2;
    private TextView mText3;
    private ImageView mCodeView;
    private Button mToggleButton;
    private MenuItem mHelpMenuItem;
    private ColorStateList mColorPassiveState;
    private HotspotManager mManager;
    private boolean mWaitForHotspot = false;
    private boolean mHotspotStartedExternally = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mConnectionUtils = new ConnectionUtils(requireContext());
        mManager = HotspotManager.newInstance(requireContext());
        mIntentFilter.addAction(BackgroundService.ACTION_PIN_USED);
        mIntentFilter.addAction(WIFI_AP_STATE_CHANGED);
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

        mColorPassiveState = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), AppUtils.getReference(
                requireContext(), R.attr.colorPassive)));
        mCodeView = view.findViewById(R.id.layout_hotspot_manager_qr_image);
        mToggleButton = view.findViewById(R.id.layout_hotspot_manager_info_toggle_button);
        mContainerText1 = view.findViewById(R.id.layout_hotspot_manager_info_container_text1_container);
        mContainerText2 = view.findViewById(R.id.layout_hotspot_manager_info_container_text2_container);
        mContainerText3 = view.findViewById(R.id.layout_hotspot_manager_info_container_text3_container);
        mText1 = view.findViewById(R.id.layout_hotspot_manager_info_container_text1);
        mText2 = view.findViewById(R.id.layout_hotspot_manager_info_container_text2);
        mText3 = view.findViewById(R.id.layout_hotspot_manager_info_container_text3);

        mToggleButton.setOnClickListener(v -> toggleHotspot());
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
    }

    @Override
    public void onResume()
    {
        super.onResume();

        requireContext().registerReceiver(mStatusReceiver, mIntentFilter);
        updateState();

        if (mWaitForHotspot)
            toggleHotspot();
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
        return R.drawable.ic_wifi_tethering_white_24dp;
    }

    @Override
    public CharSequence getDistinctiveTitle(Context context)
    {
        return context.getString(R.string.text_startHotspot);
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

    private void toggleHotspot()
    {
        if (mHotspotStartedExternally)
            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        else
            mConnectionUtils.toggleHotspot(requireActivity(), this, mManager, true,
                    REQUEST_LOCATION_PERMISSION_FOR_HOTSPOT);
    }

    private void showMenu()
    {
        if (mHelpMenuItem != null)
            mHelpMenuItem.setVisible(mManager.getConfiguration() != null && mManager.isEnabled());
    }

    private void updateState()
    {
        showMenu();
        boolean enabled = mManager.isEnabled();
        WifiConfiguration config = getWifiConfiguration();

        if (enabled && config != null)
            updateViews(config);
        else if (!enabled)
            updateViewsWithBlank();
        else {
            updateViewsAsStartedExternally();
        }
    }

    private void updateViewsWithBlank()
    {
        mHotspotStartedExternally = false;

        updateViews(null, getString(R.string.text_qrCodeHotspotDisabledHelp), null, null,
                R.string.text_startHotspot);
    }

    private void updateViewsAsStartedExternally()
    {
        mHotspotStartedExternally = true;

        updateViews(null, getString(R.string.text_hotspotStartedExternallyNotice),
                null, null, R.string.butn_stopHotspot);
    }

    // for hotspot
    private void updateViews(WifiConfiguration configuration)
    {
        mHotspotStartedExternally = false;

        try {
            String ssid = configuration.SSID;
            String bssid = configuration.BSSID;
            String key = configuration.preSharedKey;

            JSONObject object = new JSONObject()
                    .put(Keyword.NETWORK_SSID, ssid)
                    .put(Keyword.NETWORK_BSSID, bssid)
                    .put(Keyword.NETWORK_PASSWORD, key);

            updateViews(object, getString(R.string.text_qrCodeAvailableHelp), ssid, key, R.string.butn_stopHotspot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateViews(@Nullable JSONObject codeIndex, @Nullable String text1, @Nullable String text2,
                             @Nullable String text3, @StringRes int buttonText)
    {
        boolean showQRCode = codeIndex != null && codeIndex.length() > 0 && getContext() != null;

        try {
            if (showQRCode) {
                codeIndex.put(Keyword.NETWORK_PIN, AppUtils.generateNetworkPin(getContext()));

                MultiFormatWriter formatWriter = new MultiFormatWriter();
                BitMatrix bitMatrix = formatWriter.encode(codeIndex.toString(), BarcodeFormat.QR_CODE, 400,
                        400);
                BarcodeEncoder encoder = new BarcodeEncoder();
                Bitmap bitmap = encoder.createBitmap(bitMatrix);

                GlideApp.with(getContext())
                        .load(bitmap)
                        .into(mCodeView);
            } else
                mCodeView.setImageResource(R.drawable.ic_qrcode_white_128dp);

            ImageViewCompat.setImageTintList(mCodeView, showQRCode ? null : mColorPassiveState);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mContainerText1.setVisibility(text1 == null ? View.GONE : View.VISIBLE);
            mContainerText2.setVisibility(text2 == null ? View.GONE : View.VISIBLE);
            mContainerText3.setVisibility(text3 == null ? View.GONE : View.VISIBLE);

            mText1.setText(text1);
            mText2.setText(text2);
            mText3.setText(text3);
            mToggleButton.setText(buttonText);
        }
    }

    private class StatusReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // FIXME: 25.03.2020 Doesn't get called when the hotspot state is changed
            if (WIFI_AP_STATE_CHANGED.equals(intent.getAction())
                    || BackgroundService.ACTION_PIN_USED.equals(intent.getAction()))
                updateState();
        }
    }
}
