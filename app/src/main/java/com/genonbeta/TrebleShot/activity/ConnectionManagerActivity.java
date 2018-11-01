package com.genonbeta.TrebleShot.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.dialog.NavigationViewBottomSheetDialog;
import com.genonbeta.TrebleShot.fragment.BarcodeConnectFragment;
import com.genonbeta.TrebleShot.fragment.HotspotManagerFragment;
import com.genonbeta.TrebleShot.fragment.NetworkDeviceListFragment;
import com.genonbeta.TrebleShot.fragment.NetworkManagerFragment;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.ui.callback.IconSupport;
import com.genonbeta.TrebleShot.ui.callback.NetworkDeviceSelectedListener;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class ConnectionManagerActivity extends Activity
{
    private NavigationViewBottomSheetDialog mNavigationDialog;
    private HotspotManagerFragment mHotspotManagerFragment;
    private BarcodeConnectFragment mBarcodeConnectFragment;
    private NetworkManagerFragment mNetworkManagerFragment;
    private NetworkDeviceListFragment mDeviceListFragment;
    private FloatingActionButton mFAB;

    private final NetworkDeviceSelectedListener mDeviceSelectionListener = new NetworkDeviceSelectedListener()
    {
        @Override
        public boolean onNetworkDeviceSelected(NetworkDevice networkDevice, @Nullable NetworkDevice.Connection connection)
        {
            return true;
        }

        @Override
        public boolean isListenerEffective()
        {
            return true;
        }
    };

    private final IntentFilter mFilter = new IntentFilter();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (CommunicationService.ACTION_DEVICE_ACQUAINTANCE.equals(intent.getAction())
                    && intent.hasExtra(CommunicationService.EXTRA_DEVICE_ID)
                    && intent.hasExtra(CommunicationService.EXTRA_CONNECTION_ADAPTER_NAME)) {
                NetworkDevice device = new NetworkDevice(intent.getStringExtra(CommunicationService.EXTRA_DEVICE_ID));
                NetworkDevice.Connection connection = new NetworkDevice.Connection(device.deviceId, intent.getStringExtra(CommunicationService.EXTRA_CONNECTION_ADAPTER_NAME));

                try {
                    AppUtils.getDatabase(ConnectionManagerActivity.this).reconstruct(device);
                    AppUtils.getDatabase(ConnectionManagerActivity.this).reconstruct(connection);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_connection_manager);

        BottomAppBar bar = findViewById(R.id.bar);
        setSupportActionBar(bar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mBarcodeConnectFragment = (BarcodeConnectFragment) Fragment.instantiate(this, BarcodeConnectFragment.class.getName());
        mHotspotManagerFragment = (HotspotManagerFragment) Fragment.instantiate(this, HotspotManagerFragment.class.getName());
        mNetworkManagerFragment = (NetworkManagerFragment) Fragment.instantiate(this, NetworkManagerFragment.class.getName());
        mDeviceListFragment = (NetworkDeviceListFragment) Fragment.instantiate(this, NetworkDeviceListFragment.class.getName());

        mFAB = findViewById(R.id.fab);
        mFAB.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showNavigationDialog();
            }
        });

        mFilter.addAction(CommunicationService.ACTION_DEVICE_ACQUAINTANCE);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        checkFragment();
        registerReceiver(mReceiver, mFilter);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home)
            finish();
        else
            return super.onOptionsItemSelected(item);

        return true;
    }

    private void checkFragment()
    {
        Fragment currentFragment = getShowingFragment();

        if (currentFragment == null)
            setFragment(0);
        else
            applyViewChanges(currentFragment);
    }

    public void applyViewChanges(Fragment fragment)
    {
        if (fragment instanceof DeviceSelectionSupport)
            ((DeviceSelectionSupport) fragment).setDeviceSelectedListener(mDeviceSelectionListener);

        if (fragment instanceof IconSupport)
            mFAB.setImageResource(((IconSupport) fragment).getIconRes());
    }

    @IdRes
    public int getShowingFragmentId()
    {
        Fragment fragment = getShowingFragment();

        if (fragment instanceof BarcodeConnectFragment)
            return R.id.scan_qr_code;
        else if (fragment instanceof HotspotManagerFragment)
            return R.id.set_up_hotspot;
        else if (fragment instanceof NetworkManagerFragment)
            return R.id.existing_network;
        else if (fragment instanceof NetworkDeviceListFragment)
            return R.id.known_device;

        return 0;
    }

    @Nullable
    public Fragment getShowingFragment()
    {
        return getSupportFragmentManager().findFragmentById(R.id.activity_connection_establishing_content_view);
    }

    public void setFragment(@IdRes int menuId)
    {
        @Nullable
        Fragment activeFragment = getShowingFragment();
        Fragment fragmentCandidate = null;

        switch (menuId) {
            case R.id.existing_network:
                fragmentCandidate = mNetworkManagerFragment;
                break;
            case R.id.scan_qr_code:
                fragmentCandidate = mBarcodeConnectFragment;
                break;
            case R.id.known_device:
                fragmentCandidate = mDeviceListFragment;
                break;
            default:
                fragmentCandidate = mHotspotManagerFragment;
        }

        if (activeFragment == null || fragmentCandidate != activeFragment) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            if (activeFragment != null)
                transaction.remove(activeFragment);

            transaction.add(R.id.activity_connection_establishing_content_view, fragmentCandidate);
            transaction.commit();

            applyViewChanges(fragmentCandidate);
        }
    }

    public void showNavigationDialog()
    {
        if (mNavigationDialog != null && mNavigationDialog.isShowing()) {
            mNavigationDialog.cancel();
            mNavigationDialog = null;
        }

        mNavigationDialog = new NavigationViewBottomSheetDialog(this,
                R.menu.drawer_connect_devices,
                getShowingFragmentId(),
                new NavigationView.OnNavigationItemSelectedListener()
                {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem)
                    {
                        mNavigationDialog.cancel();
                        setFragment(menuItem.getItemId());
                        return true;
                    }
                });

        mNavigationDialog.show();
    }

    public interface DeviceSelectionSupport
    {
        void setDeviceSelectedListener(NetworkDeviceSelectedListener listener);
    }
}
