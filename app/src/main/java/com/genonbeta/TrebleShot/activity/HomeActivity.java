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

package com.genonbeta.TrebleShot.activity;

import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.view.Menu;
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
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.ShareAppDialog;
import com.genonbeta.TrebleShot.fragment.HomeFragment;
import com.genonbeta.TrebleShot.migration.db.Migration;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.ui.callback.PowerfulActionModeSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.UpdateUtils;
import com.genonbeta.android.framework.widget.PowerfulActionMode;
import com.google.android.material.navigation.NavigationView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class HomeActivity extends Activity
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
        mActionMode.setOnSelectionTaskListener((started, actionMode) -> toolbar.setVisibility(!started ? View.VISIBLE : View.GONE));

        if (UpdateUtils.hasNewVersion(this))
            highlightUpdater(getDefaultPreferences().getString("availableVersion", null));


        try (InputStream inputStream = openFileInput(Keyword.Local.FILENAME_UNHANDLED_CRASH_LOG)) {
            File logFile = getFileStreamPath(Keyword.Local.FILENAME_UNHANDLED_CRASH_LOG);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int len;
            byte[] buffer = new byte[8196];
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
                outputStream.flush();
            }

            String report = outputStream.toString();
            final TextStreamObject streamObject = new TextStreamObject();

            streamObject.text = report;
            streamObject.date = logFile.lastModified();
            streamObject.id = AppUtils.getUniqueNumber();

            logFile.delete();

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

            dialogBuilder.setTitle(R.string.text_crashReport);
            dialogBuilder.setMessage(R.string.text_crashInfo);
            dialogBuilder.setNegativeButton(R.string.butn_dismiss, null);
            dialogBuilder.setNeutralButton(android.R.string.copy, (dialog, which) -> {
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(
                        Service.CLIPBOARD_SERVICE);

                clipboardManager.setPrimaryClip(ClipData.newPlainText(getString(R.string.text_crashReport),
                        outputStream.toString()));

                Toast.makeText(this, R.string.mesg_textCopiedToClipboard, Toast.LENGTH_SHORT).show();
            });

            dialogBuilder.setPositiveButton(R.string.butn_save, (dialog, which) -> {
                getDatabase().insert(streamObject);
                Toast.makeText(this, R.string.mesg_textStreamSaved, Toast.LENGTH_SHORT).show();
            });

            dialogBuilder.show();
        } catch (IOException ignored) {

        }

        if (!AppUtils.isLatestChangeLogSeen(this)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.mesg_versionUpdatedChangelog)
                    .setPositiveButton(R.string.butn_yes, (dialog, which) -> {
                        AppUtils.publishLatestChangelogSeen(HomeActivity.this);
                        startActivity(new Intent(HomeActivity.this, ChangelogActivity.class));
                    })
                    .setNeutralButton(R.string.butn_never, (dialog, which) -> getDefaultPreferences().edit()
                            .putBoolean("show_changelog_dialog", false)
                            .apply())
                    .setNegativeButton(R.string.butn_no, (dialog, which) -> {
                        AppUtils.publishLatestChangelogSeen(HomeActivity.this);
                        Toast.makeText(HomeActivity.this, R.string.mesg_versionUpdatedChangelogRejected, Toast.LENGTH_SHORT).show();
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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        Menu devMenu = menu.addSubMenu("Dev. options");
        devMenu.add(-100, -2, 1, "Crash test");
        devMenu.add(-100, -3, 2, "Run migration");

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == -2) {
            Toast.makeText(this, "Crashing the app", Toast.LENGTH_SHORT).show();
            throw new NullPointerException("The crash was intentional, since 'Crash now' was called");
        } else if (item.getItemId() == -3) {
            AccessDatabase db = AppUtils.getDatabase(this);
            int dbVersion = AccessDatabase.DATABASE_VERSION;

            Toast.makeText(this, "Running migration rules again", Toast.LENGTH_SHORT).show();
            Migration.migrate(db, db.getWritableDatabase(), dbVersion, dbVersion);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onUserProfileUpdated()
    {
        createHeaderView();
    }

    /***
     * This method helps to reduce the glitch when the drawer option is chosen
     * and loaded at the same time. To prevent the glitch we wait for the signal
     * that the drawer is closed. We also hold the id of the menu item that has been clicked.
     * After this is called, we also clean the chosen menu item id.
     */
    private void applyAwaitingDrawerAction()
    {
        if (mChosenMenuItemId == 0)
            // drawer was opened, but nothing was clicked.
            return;

        if (R.id.menu_activity_main_manage_devices == mChosenMenuItemId) {
            startActivity(new Intent(this, ManageDevicesActivity.class));
        } else if (R.id.menu_activity_main_about == mChosenMenuItemId) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (R.id.menu_activity_main_send_application == mChosenMenuItemId) {
            new ShareAppDialog(HomeActivity.this)
                    .show();
        } else if (R.id.menu_activity_main_preferences == mChosenMenuItemId) {
            startActivity(new Intent(this, PreferencesActivity.class));
        } else if (R.id.menu_activity_main_exit == mChosenMenuItemId) {
            exitApp();
        } else if (R.id.menu_activity_main_donate == mChosenMenuItemId) {
            try {
                startActivity(new Intent(this, Class.forName("com.genonbeta.TrebleShot.activity.DonationActivity")));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else if (R.id.menu_activity_main_dev_survey == mChosenMenuItemId) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.text_developmentSurvey);
            builder.setMessage(R.string.text_developmentSurveySummary);
            builder.setNegativeButton(R.string.genfw_uwg_later, null);
            builder.setPositiveButton(R.string.butn_temp_doIt, (dialog, which) -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(
                            "https://docs.google.com/forms/d/e/1FAIpQLScmwX923MACmHvZTpEyZMDCxRQj" +
                                    "rd8b67u9p9MOjV1qFVp-_A/viewform?usp=sf_link"
                    )));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(HomeActivity.this, R.string.mesg_temp_noBrowser,
                            Toast.LENGTH_SHORT).show();
                }
            });
            builder.show();
        } else if (R.id.menu_activity_feedback == mChosenMenuItemId) {
            AppUtils.createFeedbackIntent(HomeActivity.this);
        }

        mChosenMenuItemId = 0;
    }

    private void createHeaderView()
    {
        View headerView = mNavigationView.getHeaderView(0);

        {
            /*MenuItem surveyItem = mNavigationView.getMenu().findItem(R.id.menu_activity_main_dev_survey);
            Configuration configuration = getApplication().getResources().getConfiguration();

            if (Build.VERSION.SDK_INT >= 24) {
                LocaleList list = configuration.getLocales();

                if (list.size() > 0)
                    for (int pos = 0; pos < list.size(); pos++)
                        if (list.get(pos).toLanguageTag().startsWith("en")) {
                            surveyItem.setVisible(true);
                            break;
                        }
            } else
                surveyItem.setVisible(configuration.locale.toString().startsWith("en"));*/
        }

        if (headerView != null) {
            NetworkDevice localDevice = AppUtils.getLocalDevice(getApplicationContext());

            ImageView imageView = headerView.findViewById(R.id.layout_profile_picture_image_default);
            ImageView editImageView = headerView.findViewById(R.id.layout_profile_picture_image_preferred);
            TextView deviceNameText = headerView.findViewById(R.id.header_default_device_name_text);
            TextView versionText = headerView.findViewById(R.id.header_default_device_version_text);

            deviceNameText.setText(localDevice.nickname);
            versionText.setText(localDevice.versionName);
            loadProfilePictureInto(localDevice.nickname, imageView);

            editImageView.setOnClickListener(v -> startProfileEditor());
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
        item.setTitle(R.string.text_newVersionAvailable);
    }
}
