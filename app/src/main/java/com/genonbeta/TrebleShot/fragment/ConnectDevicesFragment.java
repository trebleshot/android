package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.SmartFragmentPagerAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.dialog.NavigationViewBottomSheetDialog;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.ui.callback.NetworkDeviceSelectedListener;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

/**
 * created by: veli
 * date: 11/04/18 20:52
 */
public class ConnectDevicesFragment
        extends com.genonbeta.android.framework.app.Fragment
        implements TitleSupport, SnackbarSupport, com.genonbeta.android.framework.app.FragmentImpl
{
    public static final String EXTRA_CDF_FRAGMENT_NAMES_FRONT = "extraCdfFragmentNamesFront";
    public static final String EXTRA_CDF_FRAGMENT_NAMES_BACK = "extraCdfFragmentNamesBack";

    private NetworkDeviceSelectedListener mDeviceSelectedListener;
    private NavigationViewBottomSheetDialog mNavigationDialog;
    private HotspotManagerFragment mHotspotManagerFragment;
    private BarcodeConnectFragment mBarcodeConnectFragment;
    private FloatingActionButton mFAB;

    private final NetworkDeviceSelectedListener mDeviceSelectionListener = new NetworkDeviceSelectedListener()
    {
        @Override
        public boolean onNetworkDeviceSelected(NetworkDevice networkDevice, @Nullable NetworkDevice.Connection connection)
        {
            return mDeviceSelectedListener != null
                    && mDeviceSelectedListener.onNetworkDeviceSelected(networkDevice, connection);
        }

        @Override
        public boolean isListenerEffective()
        {
            return mDeviceSelectedListener != null;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (getActivity() instanceof Activity.OnPreloadArgumentWatcher) {
            if (getArguments() == null)
                setArguments(new Bundle());

            getArguments().putAll(((Activity.OnPreloadArgumentWatcher) getActivity()).passPreLoadingArguments());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        final View view = inflater.inflate(R.layout.layout_connect_devices, container, false);

        mFAB = view.findViewById(R.id.fab);
        mFAB.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showNavigationDialog();
            }
        });

        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        checkFragment();
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

    }

    @IdRes
    public int getShowingFragmentId()
    {
        Fragment fragment = getShowingFragment();

        if (fragment instanceof BarcodeConnectFragment)
            return R.id.scan_qr_code;
        else if (fragment instanceof HotspotManagerFragment)
            return R.id.existing_network;

        return 0;
    }

    @Nullable
    public Fragment getShowingFragment()
    {
        return getChildFragmentManager().findFragmentById(R.id.layout_connect_devices_content_view);
    }

    @Override
    public CharSequence getTitle(Context context)
    {
        return context.getString(R.string.text_connectDevices);
    }

    public void loadIntoSmartPagerAdapterUsingKey(SmartFragmentPagerAdapter pagerAdapter, Bundle args, String key)
    {
        if (args == null || !args.containsKey(key))
            return;

        ArrayList<SmartFragmentPagerAdapter.StableItem> thisParcelables = args.getParcelableArrayList(key);

        if (thisParcelables == null)
            return;

        for (SmartFragmentPagerAdapter.StableItem thisItem : thisParcelables)
            pagerAdapter.add(thisItem);
    }

    public void setDeviceSelectedListener(NetworkDeviceSelectedListener listener)
    {
        mDeviceSelectedListener = listener;
    }

    public void setFragment(@IdRes int menuId)
    {
        @Nullable
        Fragment activeFragment = getShowingFragment();
        Fragment fragmentCandidate = null;

        switch (menuId) {
            case R.id.existing_network:
                fragmentCandidate = mHotspotManagerFragment;
                break;
            case R.id.scan_qr_code:
                fragmentCandidate = mBarcodeConnectFragment;
                break;
            default:
                fragmentCandidate = mHotspotManagerFragment;
        }

        if (activeFragment == null || fragmentCandidate != activeFragment) {
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();

            if (activeFragment != null)
                transaction.remove(activeFragment);

            transaction.add(R.id.layout_connect_devices_content_view, fragmentCandidate);
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

        mNavigationDialog = new NavigationViewBottomSheetDialog(getActivity(),
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
}
