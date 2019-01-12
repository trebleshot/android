package com.genonbeta.TrebleShot.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.fragment.HomeFragment;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.DeviceScannerService;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.ui.callback.PowerfulActionModeSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.UpdateUtils;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.util.Interrupter;
import com.genonbeta.android.framework.widget.PowerfulActionMode;
import com.google.android.material.navigation.NavigationView;

import java.io.File;

public class HomeActivity
        extends Activity
        implements NavigationView.OnNavigationItemSelectedListener, PowerfulActionModeSupport
{
    public static final int REQUEST_PERMISSION_ALL = 1;

    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;
    private PowerfulActionMode mActionMode;
    private HomeFragment mHomeFragment;

    private long mExitPressTime;
    private int mChosenMenuItemId;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mHomeFragment = (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.activitiy_home_fragment);
        mActionMode = findViewById(R.id.content_powerful_action_mode);
        mNavigationView = findViewById(R.id.nav_view);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.text_navigationDrawerOpen, R.string.text_navigationDrawerClose);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        mDrawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener()
        {
            @Override
            public void onDrawerClosed(View drawerView)
            {
                applyAwaitingDrawerAction();
            }
        });

        mNavigationView.setNavigationItemSelectedListener(this);

        mActionMode.setOnSelectionTaskListener(new PowerfulActionMode.OnSelectionTaskListener()
        {
            @Override
            public void onSelectionTask(boolean started, PowerfulActionMode actionMode)
            {
                toolbar.setVisibility(!started ? View.VISIBLE : View.GONE);
            }
        });

        if (UpdateUtils.hasNewVersion(this))
            highlightUpdater(getDefaultPreferences().getString("availableVersion", null));

        if (!AppUtils.isLatestChangeLogSeen(this)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.mesg_versionUpdatedChangelog)
                    .setPositiveButton(R.string.butn_yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            AppUtils.publishLatestChangelogSeen(HomeActivity.this);
                            startActivity(new Intent(HomeActivity.this, ChangelogActivity.class));
                        }
                    })
                    .setNeutralButton(R.string.butn_never, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            getDefaultPreferences().edit()
                                    .putBoolean("show_changelog_dialog", false)
                                    .apply();
                        }
                    })
                    .setNegativeButton(R.string.butn_no, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            AppUtils.publishLatestChangelogSeen(HomeActivity.this);
                            Toast.makeText(HomeActivity.this, R.string.mesg_versionUpdatedChangelogRejected, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        }

        if (Keyword.Flavor.googlePlay.equals(AppUtils.getBuildFlavor())) {
            MenuItem donateItem = mNavigationView.getMenu()
                    .findItem(R.id.menu_activity_main_donate);

            if (donateItem != null)
                donateItem.setVisible(true);
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        createHeaderView();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
        mChosenMenuItemId = item.getItemId();

        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public void onBackPressed()
    {
        if (mHomeFragment.onBackPressed())
            return;

        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START))
            mDrawerLayout.closeDrawer(GravityCompat.START);
        else if ((System.currentTimeMillis() - mExitPressTime) < 2000)
            super.onBackPressed();
        else {
            mExitPressTime = System.currentTimeMillis();
            Toast.makeText(this, R.string.mesg_secureExit, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUserProfileUpdated()
    {
        createHeaderView();
    }

    private void applyAwaitingDrawerAction()
    {
        if (mChosenMenuItemId == 0) {
            // Do nothing
        } else if (R.id.menu_activity_main_manage_devices == mChosenMenuItemId) {
            startActivity(new Intent(this, ManageDevicesActivity.class));
        } else if (R.id.menu_activity_main_about == mChosenMenuItemId) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (R.id.menu_activity_main_send_application == mChosenMenuItemId) {
            sendThisApplication();
        } else if (R.id.menu_activity_main_preferences == mChosenMenuItemId) {
            startActivity(new Intent(this, PreferencesActivity.class));
        } else if (R.id.menu_activity_main_exit == mChosenMenuItemId) {
            stopService(new Intent(this, CommunicationService.class));
            stopService(new Intent(this, DeviceScannerService.class));
            stopService(new Intent(this, WorkerService.class));

            finish();
        } else if (R.id.menu_activity_main_donate == mChosenMenuItemId) {
            try {
                startActivity(new Intent(this, Class.forName("com.genonbeta.TrebleShot.activity.DonationActivity")));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else if (R.id.menu_activity_feedback == mChosenMenuItemId) {
            AppUtils.createFeedbackIntent(HomeActivity.this);
        }

        mChosenMenuItemId = 0;
    }

    private void createHeaderView()
    {
        View headerView = mNavigationView.getHeaderView(0);

        if (headerView != null) {
            NetworkDevice localDevice = AppUtils.getLocalDevice(getApplicationContext());

            ImageView imageView = headerView.findViewById(R.id.header_default_device_image);
            ImageView editImageView = headerView.findViewById(R.id.header_default_device_edit_image);
            TextView deviceNameText = headerView.findViewById(R.id.header_default_device_name_text);
            TextView versionText = headerView.findViewById(R.id.header_default_device_version_text);

            deviceNameText.setText(localDevice.nickname);
            versionText.setText(localDevice.versionName);
            loadProfilePictureInto(localDevice.nickname, imageView);

            editImageView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    startProfileEditor();
                }
            });
        }
    }

    @Override
    public PowerfulActionMode getPowerfulActionMode()
    {
        return mActionMode;
    }

    private void highlightUpdater(String availableVersion)
    {
        MenuItem item = mNavigationView.getMenu().findItem(R.id.menu_activity_main_about);

        item.setChecked(true);
        item.setTitle(R.string.text_newVersionAvailable);
    }

    private void sendThisApplication()
    {
        new Handler(Looper.myLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    Interrupter interrupter = new Interrupter();

                    PackageManager pm = getPackageManager();
                    PackageInfo packageInfo = pm.getPackageInfo(getApplicationInfo().packageName, 0);

                    String fileName = packageInfo.applicationInfo.loadLabel(pm) + "_" + packageInfo.versionName + ".apk";

                    DocumentFile storageDirectory = FileUtils.getApplicationDirectory(getApplicationContext());
                    DocumentFile codeFile = DocumentFile.fromFile(new File(getApplicationInfo().sourceDir));
                    DocumentFile cloneFile = storageDirectory.createFile(null, FileUtils.getUniqueFileName(storageDirectory, fileName, true));

                    FileUtils.copy(HomeActivity.this, codeFile, cloneFile, interrupter);

                    try {
                        Intent sendIntent = new Intent(Intent.ACTION_SEND)
                                .putExtra(ShareActivity.EXTRA_FILENAME_LIST, fileName)
                                .putExtra(Intent.EXTRA_STREAM, FileUtils.getSecureUri(HomeActivity.this, cloneFile))
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                .setType(cloneFile.getType());

                        startActivity(Intent.createChooser(sendIntent, getString(R.string.text_fileShareAppChoose)));
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(HomeActivity.this, R.string.mesg_providerNotAllowedError, Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
