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
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.NavigationViewBottomSheetDialog;
import com.genonbeta.TrebleShot.fragment.BarcodeConnectFragment;
import com.genonbeta.TrebleShot.fragment.HotspotManagerFragment;
import com.genonbeta.TrebleShot.fragment.NetworkDeviceListFragment;
import com.genonbeta.TrebleShot.fragment.NetworkManagerFragment;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.ui.UIConnectionUtils;
import com.genonbeta.TrebleShot.ui.UITask;
import com.genonbeta.TrebleShot.ui.callback.IconSupport;
import com.genonbeta.TrebleShot.ui.callback.NetworkDeviceSelectedListener;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.genonbeta.android.framework.util.Interrupter;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class ConnectionManagerActivity
        extends Activity
        implements SnackbarSupport
{
    public static final String EXTRA_DEVICE_ID = "extraDeviceId";
    public static final String EXTRA_CONNECTION_ADAPTER = "extraConnectionAdapter";
    public static final String EXTRA_REQUEST_TYPE = "extraRequestType";

    private NavigationViewBottomSheetDialog mNavigationDialog;
    private HotspotManagerFragment mHotspotManagerFragment;
    private BarcodeConnectFragment mBarcodeConnectFragment;
    private NetworkManagerFragment mNetworkManagerFragment;
    private NetworkDeviceListFragment mDeviceListFragment;
    private FloatingActionButton mFAB;
    private RequestType mRequestType = RequestType.RETURN_RESULT;

    private final NetworkDeviceSelectedListener mDeviceSelectionListener = new NetworkDeviceSelectedListener()
    {
        @Override
        public boolean onNetworkDeviceSelected(NetworkDevice networkDevice, NetworkDevice.Connection connection)
        {
            if (mRequestType.equals(RequestType.RETURN_RESULT)) {
                setResult(RESULT_OK, new Intent()
                        .putExtra(EXTRA_DEVICE_ID, networkDevice.deviceId)
                        .putExtra(EXTRA_CONNECTION_ADAPTER, connection.adapterName));

                finish();
            } else {
                ConnectionUtils connectionUtils = ConnectionUtils.getInstance(ConnectionManagerActivity.this);
                UIConnectionUtils uiConnectionUtils = new UIConnectionUtils(connectionUtils, ConnectionManagerActivity.this);

                UITask uiTask = new UITask()
                {
                    @Override
                    public void updateTaskStarted(Interrupter interrupter)
                    {

                    }

                    @Override
                    public void updateTaskStopped()
                    {

                    }
                };

                NetworkDeviceLoader.OnDeviceRegisteredListener registeredListener = new NetworkDeviceLoader.OnDeviceRegisteredListener()
                {
                    @Override
                    public void onDeviceRegistered(AccessDatabase database, final NetworkDevice device, final NetworkDevice.Connection connection)
                    {
                        createSnackbar(R.string.mesg_completing).show();
                    }
                };

                uiConnectionUtils.makeAcquaintance(ConnectionManagerActivity.this, getDatabase(),
                        uiTask, connection.ipAddress, -1, registeredListener);
            }

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
            if (mRequestType.equals(RequestType.RETURN_RESULT)) {
                if (CommunicationService.ACTION_DEVICE_ACQUAINTANCE.equals(intent.getAction())
                        && intent.hasExtra(CommunicationService.EXTRA_DEVICE_ID)
                        && intent.hasExtra(CommunicationService.EXTRA_CONNECTION_ADAPTER_NAME)) {
                    NetworkDevice device = new NetworkDevice(intent.getStringExtra(CommunicationService.EXTRA_DEVICE_ID));
                    NetworkDevice.Connection connection = new NetworkDevice.Connection(device.deviceId, intent.getStringExtra(CommunicationService.EXTRA_CONNECTION_ADAPTER_NAME));

                    try {
                        AppUtils.getDatabase(ConnectionManagerActivity.this).reconstruct(device);
                        AppUtils.getDatabase(ConnectionManagerActivity.this).reconstruct(connection);

                        mDeviceSelectionListener.onNetworkDeviceSelected(device, connection);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (mRequestType.equals(RequestType.MAKE_ACQUAINTANCE)) {
                if (CommunicationService.ACTION_INCOMING_TRANSFER_READY.equals(intent.getAction())
                        && intent.hasExtra(CommunicationService.EXTRA_GROUP_ID)) {
                    ViewTransferActivity.startInstance(ConnectionManagerActivity.this,
                            intent.getLongExtra(CommunicationService.EXTRA_GROUP_ID, -1));
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);
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
        mFilter.addAction(CommunicationService.ACTION_INCOMING_TRANSFER_READY);

        if (getIntent() != null
                && getIntent().hasExtra(EXTRA_REQUEST_TYPE))
            try {
                mRequestType = RequestType.valueOf(getIntent().getStringExtra(EXTRA_REQUEST_TYPE));
            } catch (Exception e) {

            }
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

    public void applyViewChanges(Fragment fragment)
    {
        if (fragment instanceof DeviceSelectionSupport)
            ((DeviceSelectionSupport) fragment).setDeviceSelectedListener(mDeviceSelectionListener);

        if (fragment instanceof IconSupport)
            mFAB.setImageResource(((IconSupport) fragment).getIconRes());
    }

    private void checkFragment()
    {
        Fragment currentFragment = getShowingFragment();

        if (currentFragment == null)
            setFragment(0);
        else
            applyViewChanges(currentFragment);
    }

    @Override
    public Snackbar createSnackbar(int resId, Object... objects)
    {
        return Snackbar.make(findViewById(R.id.inside_container), getString(resId, objects), Snackbar.LENGTH_LONG);
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

    public enum RequestType
    {
        RETURN_RESULT,
        MAKE_ACQUAINTANCE
    }
}
