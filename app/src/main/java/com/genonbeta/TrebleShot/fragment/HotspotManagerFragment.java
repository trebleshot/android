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
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.receiver.NetworkStatusReceiver;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.ui.UIConnectionUtils;
import com.genonbeta.TrebleShot.ui.callback.IconSupport;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.HotspotUtils;
import com.genonbeta.TrebleShot.util.NetworkUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

/**
 * created by: veli
 * date: 11/04/18 20:53
 */
public class HotspotManagerFragment
        extends com.genonbeta.android.framework.app.Fragment
        implements TitleSupport, IconSupport
{
    public static final int REQUEST_LOCATION_PERMISSION_FOR_HOTSPOT = 643;

    private IntentFilter mIntentFilter = new IntentFilter();
    private StatusReceiver mStatusReceiver = new StatusReceiver();
    private UIConnectionUtils mConnectionUtils;

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
    private boolean mWaitForHotspot = false;
    private boolean mWaitForWiFi = false;
    private boolean mHotspotStartedExternally = false;

    private UIConnectionUtils.RequestWatcher mHotspotWatcher = new UIConnectionUtils.RequestWatcher()
    {
        @Override
        public void onResultReturned(boolean result, boolean shouldWait)
        {
            mWaitForHotspot = shouldWait;
        }
    };

    private UIConnectionUtils.RequestWatcher mWiFiWatcher = new UIConnectionUtils.RequestWatcher()
    {
        @Override
        public void onResultReturned(boolean result, boolean shouldWait)
        {
            mWaitForWiFi = shouldWait;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mIntentFilter.addAction(CommunicationService.ACTION_HOTSPOT_STATUS);
        mIntentFilter.addAction(NetworkStatusReceiver.WIFI_AP_STATE_CHANGED);

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = getLayoutInflater().inflate(R.layout.layout_hotspot_manager, container, false);

        mColorPassiveState = ColorStateList.valueOf(ContextCompat.getColor(getContext(), AppUtils.getReference(getContext(), R.attr.colorPassive)));
        mCodeView = view.findViewById(R.id.layout_hotspot_manager_qr_image);
        mToggleButton = view.findViewById(R.id.layout_hotspot_manager_info_toggle_button);
        mContainerText1 = view.findViewById(R.id.layout_hotspot_manager_info_container_text1_container);
        mContainerText2 = view.findViewById(R.id.layout_hotspot_manager_info_container_text2_container);
        mContainerText3 = view.findViewById(R.id.layout_hotspot_manager_info_container_text3_container);
        mText1 = view.findViewById(R.id.layout_hotspot_manager_info_container_text1);
        mText2 = view.findViewById(R.id.layout_hotspot_manager_info_container_text2);
        mText3 = view.findViewById(R.id.layout_hotspot_manager_info_container_text3);

        mToggleButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                toggleHotspot();
            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.actions_hotspot_manager, menu);
        mHelpMenuItem = menu.findItem(R.id.show_help);

        showMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.show_help && getConnectionUtils().getHotspotUtils().getConfiguration() != null) {
            String hotspotName = getConnectionUtils().getHotspotUtils().getConfiguration().SSID;
            String friendlyName = AppUtils.getFriendlySSID(hotspotName);

            new AlertDialog.Builder(getActivity())
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

        getContext().registerReceiver(mStatusReceiver, mIntentFilter);
        updateState();

        if (mWaitForHotspot)
            toggleHotspot();
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
        return R.drawable.ic_wifi_tethering_white_24dp;
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
        return context.getString(R.string.text_startHotspot);
    }

    private void toggleHotspot()
    {
        if (mHotspotStartedExternally)
            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        else
            getUIConnectionUtils().toggleHotspot(true, getActivity(), REQUEST_LOCATION_PERMISSION_FOR_HOTSPOT, mHotspotWatcher);
    }

    private void updateViewsWithBlank()
    {
        mHotspotStartedExternally = false;

        updateViews(null,
                getString(R.string.text_qrCodeHotspotDisabledHelp),
                null,
                null,
                R.string.text_startHotspot);
    }

    private void updateViewsStartedExternally()
    {
        mHotspotStartedExternally = true;

        updateViews(null, getString(R.string.text_hotspotStartedExternallyNotice),
                null, null, R.string.butn_stopHotspot);
    }

    // for hotspot
    private void updateViews(String networkName, String password, int keyManagement)
    {
        mHotspotStartedExternally = false;

        try {
            JSONObject object = new JSONObject()
                    .put(Keyword.NETWORK_NAME, networkName)
                    .put(Keyword.NETWORK_PASSWORD, password)
                    .put(Keyword.NETWORK_KEYMGMT, keyManagement);

            updateViews(object, getString(R.string.text_qrCodeAvailableHelp), networkName, password, R.string.butn_stopHotspot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateViews(@Nullable JSONObject codeIndex,
                             @Nullable String text1,
                             @Nullable String text2,
                             @Nullable String text3,
                             @StringRes int buttonText)
    {
        boolean showQRCode = codeIndex != null
                && codeIndex.length() > 0
                && getContext() != null;

        try {

            if (showQRCode) {
                {
                    int networkPin = AppUtils.getUniqueNumber();

                    codeIndex.put(Keyword.NETWORK_PIN, networkPin);

                    AppUtils.getDefaultPreferences(getContext()).edit()
                            .putInt(Keyword.NETWORK_PIN, networkPin)
                            .apply();
                }

                MultiFormatWriter formatWriter = new MultiFormatWriter();
                BitMatrix bitMatrix = formatWriter.encode(codeIndex.toString(), BarcodeFormat.QR_CODE, 400, 400);
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

    private void showMenu()
    {
        if (mHelpMenuItem != null)
            mHelpMenuItem.setVisible(getConnectionUtils().getHotspotUtils().getConfiguration() != null
                    && getConnectionUtils().getHotspotUtils().isEnabled());
    }

    private void updateState()
    {
        boolean isEnabled = getUIConnectionUtils().getConnectionUtils().getHotspotUtils().isEnabled();
        WifiConfiguration wifiConfiguration = getConnectionUtils().getHotspotUtils().getConfiguration();

        showMenu();

        if (!isEnabled) {
            updateViewsWithBlank();
        } else if (getConnectionUtils().getHotspotUtils() instanceof HotspotUtils.HackAPI
                && wifiConfiguration != null) {
            updateViews(wifiConfiguration.SSID, wifiConfiguration.preSharedKey, NetworkUtils.getAllowedKeyManagement(wifiConfiguration));
        } else if (Build.VERSION.SDK_INT >= 26)
            AppUtils.startForegroundService(getActivity(),
                    new Intent(getActivity(), CommunicationService.class)
                            .setAction(CommunicationService.ACTION_REQUEST_HOTSPOT_STATUS));
    }

    private class StatusReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (NetworkStatusReceiver.WIFI_AP_STATE_CHANGED.equals(intent.getAction()))
                updateState();
            else if (CommunicationService.ACTION_HOTSPOT_STATUS.equals(intent.getAction())) {
                if (intent.getBooleanExtra(CommunicationService.EXTRA_HOTSPOT_ENABLED, false))
                    updateViews(intent.getStringExtra(CommunicationService.EXTRA_HOTSPOT_NAME),
                            intent.getStringExtra(CommunicationService.EXTRA_HOTSPOT_PASSWORD),
                            intent.getIntExtra(CommunicationService.EXTRA_HOTSPOT_KEY_MGMT, 0));
                else if (getConnectionUtils().getHotspotUtils().isEnabled()
                        && !intent.getBooleanExtra(CommunicationService.EXTRA_HOTSPOT_DISABLING, false)) {
                    updateViewsStartedExternally();
                }
            }
        }
    }
}
