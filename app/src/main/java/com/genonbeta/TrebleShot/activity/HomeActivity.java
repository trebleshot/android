
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

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class HomeActivity
        extends Activity
        implements NavigationView.OnNavigationItemSelectedListener, PowerfulActionModeSupport
{
    public static final int REQUEST_PERMISSION_ALL = 1;

    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;
    private PowerfulActionMode mActionMode;

    private long mExitPressTime;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mActionMode = findViewById(R.id.content_powerful_action_mode);
        mNavigationView = findViewById(R.id.nav_view);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.text_navigationDrawerOpen, R.string.text_navigationDrawerClose);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

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
            AlertDialog.Builder versionChangeDialog = new AlertDialog.Builder(this);

            versionChangeDialog.setMessage(R.string.mesg_versionUpdatedChangelog);

            versionChangeDialog.setPositiveButton(R.string.butn_show, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    AppUtils.publishLatestChangelogSeen(HomeActivity.this);
                    startActivity(new Intent(HomeActivity.this, ChangelogActivity.class));
                }
            });

            versionChangeDialog.setNegativeButton(R.string.butn_cancel, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    AppUtils.publishLatestChangelogSeen(HomeActivity.this);
                    Toast.makeText(HomeActivity.this, R.string.mesg_versionUpdatedChangelogRejected, Toast.LENGTH_SHORT).show();
                }
            });

            versionChangeDialog.show();
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

        View headerView = mNavigationView.getHeaderView(0);

        if (headerView != null) {
            TextDrawable.IShapeBuilder iconBuilder = AppUtils.getDefaultIconBuilder(this);
            NetworkDevice localDevice = AppUtils.getLocalDevice(getApplicationContext());

            ImageView imageView = headerView.findViewById(R.id.header_default_device_image);
            TextView deviceNameText = headerView.findViewById(R.id.header_default_device_name_text);
            TextView versionText = headerView.findViewById(R.id.header_default_device_version_text);

            imageView.setImageDrawable(iconBuilder.buildRound(localDevice.nickname));
            deviceNameText.setText(localDevice.nickname);
            versionText.setText(localDevice.versionName);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
        if (R.id.menu_activity_main_file_explorer == item.getItemId()) {
            startActivity(new Intent(this, FileExplorerActivity.class));
        } else if (R.id.menu_activity_main_about == item.getItemId()) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (R.id.menu_activity_main_send_application == item.getItemId()) {
            sendThisApplication();
        } else if (R.id.menu_activity_main_preferences == item.getItemId()) {
            startActivity(new Intent(this, PreferencesActivity.class));
        } else if (R.id.menu_activity_main_exit == item.getItemId()) {
            stopService(new Intent(this, CommunicationService.class));
            stopService(new Intent(this, DeviceScannerService.class));
            stopService(new Intent(this, WorkerService.class));

            finish();
        } else if (R.id.menu_activity_main_donate == item.getItemId()) {
            try {
                startActivity(new Intent(this, Class.forName("com.genonbeta.TrebleShot.activity.DonationActivity")));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else
            return false;

        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public void onBackPressed()
    {
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
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    PackageInfo packageInfo = pm.getPackageInfo(getApplicationInfo().packageName, 0);

                    String fileName = packageInfo.applicationInfo.loadLabel(pm) + "_" + packageInfo.versionName + ".apk";

                    DocumentFile storageDirectory = FileUtils.getApplicationDirectory(getApplicationContext(), getDefaultPreferences());
                    DocumentFile codeFile = DocumentFile.fromFile(new File(getApplicationInfo().sourceDir));
                    DocumentFile cloneFile = storageDirectory.createFile(null, FileUtils.getUniqueFileName(storageDirectory, fileName, true));

                    FileUtils.copy(HomeActivity.this, codeFile, cloneFile, interrupter);

                    try {
                        sendIntent
                                .putExtra(ShareActivity.EXTRA_FILENAME_LIST, fileName)
                                .putExtra(Intent.EXTRA_STREAM, FileUtils.getSecureUri(HomeActivity.this, cloneFile))
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
