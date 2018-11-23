package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * created by: veli
 * date: 11/04/18 20:53
 */
public class HotspotManagerFragment
        extends com.genonbeta.android.framework.app.Fragment
        implements TitleSupport, IconSupport
{
    public static final int REQUEST_LOCATION_PERMISSION = 643;

    private IntentFilter mIntentFilter = new IntentFilter();
    private StatusReceiver mStatusReceiver = new StatusReceiver();
    private UIConnectionUtils mConnectionUtils;

    private View mContainerText1;
    private View mContainerText2;
    private View mContainerText3;
    private AppCompatTextView mText1;
    private TextView mText2;
    private TextView mText3;
    private ImageView mCodeView;
    private AppCompatButton mToggleButton;

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
                getUIConnectionUtils().toggleHotspot(true, getActivity(), REQUEST_LOCATION_PERMISSION);
            }
        });

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

    private void updateViewsWithBlank()
    {
        updateViews(null,
                getString(R.string.text_qrCodeHotspotDisabledHelp),
                null,
                null,
                R.string.text_startHotspot);
    }

    // for hotspot
    private void updateViews(String networkName, String password, int keyManagement)
    {
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
            mToggleButton.setText(buttonText);
        }
    }

    private void updateState()
    {
        boolean isEnabled = getUIConnectionUtils().getConnectionUtils().getHotspotUtils().isEnabled();
        WifiConfiguration wifiConfiguration = getConnectionUtils().getHotspotUtils().getConfiguration();

        if (!isEnabled) {
            updateViewsWithBlank();
        } else {
            if (getConnectionUtils().getHotspotUtils() instanceof HotspotUtils.HackAPI
                    && wifiConfiguration != null)
                updateViews(wifiConfiguration.SSID, wifiConfiguration.preSharedKey, NetworkUtils.getAllowedKeyManagement(wifiConfiguration));
            else if (Build.VERSION.SDK_INT >= 26)
                AppUtils.startForegroundService(getActivity(),
                        new Intent(getActivity(), CommunicationService.class)
                                .setAction(CommunicationService.ACTION_REQUEST_HOTSPOT_STATUS));

            if (wifiConfiguration != null
                    && getConnectionUtils().getHotspotUtils().isEnabled()) {
                if (getUIConnectionUtils().notifyShowHotspotHandled()
                        && AppUtils.getHotspotName(getActivity()).equals(wifiConfiguration.SSID)) {
                    final Snackbar snackbar = createSnackbar(R.string.mesg_hotspotCreatedInfo, getConnectionUtils().getHotspotUtils().getConfiguration().SSID, AppUtils.getFriendlySSID(getConnectionUtils().getHotspotUtils().getConfiguration().SSID));

                    snackbar.setAction(R.string.butn_gotIt, new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            snackbar.dismiss();
                        }
                    });

                    snackbar.setDuration(BaseTransientBottomBar.LENGTH_INDEFINITE)
                            .show();
                }
            }
        }
    }

    private class StatusReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (NetworkStatusReceiver.WIFI_AP_STATE_CHANGED.equals(intent.getAction()))
                updateState();
            else if (CommunicationService.ACTION_HOTSPOT_STATUS.equals(intent.getAction())
                    && intent.getBooleanExtra(CommunicationService.EXTRA_HOTSPOT_ENABLED, false)) {
                updateViews(intent.getStringExtra(CommunicationService.EXTRA_HOTSPOT_NAME),
                        intent.getStringExtra(CommunicationService.EXTRA_HOTSPOT_PASSWORD),
                        intent.getIntExtra(CommunicationService.EXTRA_HOTSPOT_KEY_MGMT, 0));
            }
        }
    }
}
