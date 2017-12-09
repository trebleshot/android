package com.genonbeta.TrebleShot.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.fragment.ApplicationListFragment;
import com.genonbeta.TrebleShot.fragment.FileExplorerFragment;
import com.genonbeta.TrebleShot.fragment.MusicListFragment;
import com.genonbeta.TrebleShot.fragment.NetworkDeviceListFragment;
import com.genonbeta.TrebleShot.fragment.TextShareFragment;
import com.genonbeta.TrebleShot.fragment.TransactionGroupListFragment;
import com.genonbeta.TrebleShot.fragment.VideoListFragment;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.TrebleShot.util.PowerfulActionModeSupported;
import com.genonbeta.TrebleShot.util.PredetachListener;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TitleSupport;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import velitasali.updatewithgithub.GitHubUpdater;

public class HomeActivity extends Activity implements NavigationView.OnNavigationItemSelectedListener, PowerfulActionModeSupported
{
	public static final String ACTION_OPEN_RECEIVED_FILES = "genonbeta.intent.action.OPEN_RECEIVED_FILES";
	public static final String ACTION_OPEN_ONGOING_LIST = "genonbeta.intent.action.OPEN_ONGOING_LIST";
	public static final String EXTRA_FILE_PATH = "filePath";

	public static final int REQUEST_PERMISSION_ALL = 1;

	private SharedPreferences mPreferences;
	private PowerfulActionMode mActionMode;
	private NavigationView mNavigationView;
	private DrawerLayout mDrawerLayout;
	private GitHubUpdater mUpdater;
	private Fragment mFragmentDeviceList;
	private Fragment mFragmentFileExplorer;
	private Fragment mFragmentTransactions;
	private Fragment mFragmentShareApplication;
	private Fragment mFragmentShareMusic;
	private Fragment mFragmentShareVideo;
	private Fragment mFragmentShareText;

	private long mExitPressTime;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		mDrawerLayout = findViewById(R.id.drawer_layout);

		if (mDrawerLayout != null) {
			ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.text_navigationDrawerOpen, R.string.text_navigationDrawerClose);
			mDrawerLayout.addDrawerListener(toggle);
			toggle.syncState();
		}

		mUpdater = new GitHubUpdater(this, AppConfig.APP_UPDATE_REPO, R.style.AppTheme);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mActionMode = findViewById(R.id.content_powerful_action_mode);
		mNavigationView = findViewById(R.id.nav_view);
		mNavigationView.setNavigationItemSelectedListener(this);

		mActionMode.setContainerLayout(findViewById(R.id.content_powerful_action_mode_layout));

		mFragmentDeviceList = Fragment.instantiate(this, NetworkDeviceListFragment.class.getName());
		mFragmentFileExplorer = Fragment.instantiate(this, FileExplorerFragment.class.getName());
		mFragmentTransactions = Fragment.instantiate(this, TransactionGroupListFragment.class.getName());
		mFragmentShareApplication = Fragment.instantiate(this, ApplicationListFragment.class.getName());
		mFragmentShareMusic = Fragment.instantiate(this, MusicListFragment.class.getName());
		mFragmentShareVideo = Fragment.instantiate(this, VideoListFragment.class.getName());
		mFragmentShareText = Fragment.instantiate(this, TextShareFragment.class.getName());

		changeFragment(mFragmentDeviceList);
		checkCurrentRequestedFragment(getIntent());

		if (mPreferences.contains("availableVersion") && mUpdater.isNewVersion(mPreferences.getString("availableVersion", null)))
			highlightUpdater(mPreferences.getString("availableVersion", null));
		else
			mUpdater.checkForUpdates(false, new GitHubUpdater.OnInfoAvailableListener()
			{
				@Override
				public void onInfoAvailable(boolean newVersion, String versionName, String title, String description, String releaseDate)
				{
					mPreferences
							.edit()
							.putString("availableVersion", versionName)
							.apply();

					if (newVersion)
						highlightUpdater(versionName);
				}
			});

		NetworkDevice localDevice = AppUtils.getLocalDevice(getApplicationContext());

		if (!mPreferences.contains("migrated_version")) {
			if (localDevice.buildNumber == 49) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);

				builder.setTitle(R.string.text_importantNotice);
				builder.setMessage(R.string.text_migrateNotice49);
				builder.setPositiveButton(R.string.butn_close, null);
				builder.show();
			}
		} else if (mPreferences.getInt("migrated_version", localDevice.buildNumber) < localDevice.buildNumber) {
			// migrating to a new version
		}

		mPreferences.edit()
				.putInt("migrated_version", localDevice.buildNumber)
				.apply();
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		View headerView = mNavigationView.getHeaderView(0);

		if (headerView != null) {
			NetworkDevice localDevice = AppUtils.getLocalDevice(getApplicationContext());

			ImageView imageView = headerView.findViewById(R.id.header_main_image);
			TextView deviceNameText = headerView.findViewById(R.id.header_main_text1);
			TextView versionText = headerView.findViewById(R.id.header_main_text2);

			String firstLetters = TextUtils.getFirstLetters(localDevice.user, 1);
			TextDrawable drawable = TextDrawable.builder().buildRoundRect(firstLetters.length() > 0 ? firstLetters : "?", ContextCompat.getColor(getApplicationContext(), R.color.colorTextDrawable), 100);

			imageView.setImageDrawable(drawable);
			deviceNameText.setText(localDevice.user);
			versionText.setText(localDevice.buildName);
		}
	}

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item)
	{
		if (R.id.menu_activity_main_device_list == item.getItemId()) {
			changeFragment(mFragmentDeviceList);
		} else if (R.id.menu_activity_main_file_explorer == item.getItemId()) {
			changeFragment(mFragmentFileExplorer);
		} else if (R.id.menu_activity_main_ongoing_process == item.getItemId()) {
			changeFragment(mFragmentTransactions);
		} else if (R.id.menu_activity_main_share_app == item.getItemId()) {
			changeFragment(mFragmentShareApplication);
		} else if (R.id.menu_activity_main_share_music == item.getItemId()) {
			changeFragment(mFragmentShareMusic);
		} else if (R.id.menu_activity_main_share_video == item.getItemId()) {
			changeFragment(mFragmentShareVideo);
		} else if (R.id.menu_activity_main_share_text == item.getItemId()) {
			changeFragment(mFragmentShareText);
		} else if (R.id.menu_activity_main_about == item.getItemId()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			builder.setTitle(R.string.text_about);
			builder.setMessage(R.string.text_aboutSummary);
			builder.setNegativeButton(R.string.butn_close, null);
			builder.setPositiveButton(R.string.butn_seeSourceCode, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.APPLICATION_REPO)));
				}
			});

			builder.show();
		} else if (R.id.menu_activity_main_send_application == item.getItemId()) {
			sendThisApplication();
		} else if (R.id.menu_activity_main_preferences == item.getItemId()) {
			startActivity(new Intent(this, PreferencesActivity.class));
		} else if (R.id.menu_activity_main_check_for_updates == item.getItemId()) {
			mUpdater.checkForUpdates(true, null);
		} else
			return false;

		if (mDrawerLayout != null)
			mDrawerLayout.closeDrawer(GravityCompat.START);

		return true;
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		checkCurrentRequestedFragment(intent);
	}

	@Override
	public void onBackPressed()
	{
		if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START))
			mDrawerLayout.closeDrawer(GravityCompat.START);
		else {
			if ((System.currentTimeMillis() - mExitPressTime) < 2000)
				finish();
			else {
				mExitPressTime = System.currentTimeMillis();
				Toast.makeText(this, R.string.mesg_secureExit, Toast.LENGTH_SHORT).show();
			}
		}
	}

	public void changeFragment(Fragment fragment)
	{
		Fragment removedFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);

		if (removedFragment != null && removedFragment instanceof PredetachListener)
			((PredetachListener) removedFragment).onPrepareDetach();

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

		ft.replace(R.id.content_frame, fragment);
		ft.commit();

		if (fragment instanceof TitleSupport)
			setTitle(((TitleSupport) fragment).getTitle(this));
		else
			setTitle(R.string.text_appName);
	}

	public void checkCurrentRequestedFragment(Intent intent)
	{
		if (intent != null)
			if (ACTION_OPEN_RECEIVED_FILES.equals(intent.getAction())) {
				changeFragment(mFragmentFileExplorer);

				if (intent.hasExtra(EXTRA_FILE_PATH))
				{
					File requestedDirectory = new File(intent.getStringExtra(EXTRA_FILE_PATH));

					if (requestedDirectory.isDirectory() && requestedDirectory.canRead())
						((FileExplorerFragment)mFragmentFileExplorer).requestPath(requestedDirectory);
				}

				mNavigationView.setCheckedItem(R.id.menu_activity_main_file_explorer);
			} else if (ACTION_OPEN_ONGOING_LIST.equals(intent.getAction())) {
				changeFragment(mFragmentTransactions);
				mNavigationView.setCheckedItem(R.id.menu_activity_main_ongoing_process);
			}
	}

	private void highlightUpdater(String availableVersion)
	{
		MenuItem item = mNavigationView.getMenu().findItem(R.id.menu_activity_main_check_for_updates);

		item.setChecked(true);
		item.setTitle(R.string.text_newVersionAvailable);
	}

	public PowerfulActionMode getPowerfulActionMode()
	{
		return mActionMode;
	}

	private void sendThisApplication()
	{
		new Handler(Looper.myLooper()).post(new Runnable()
		{
			@Override
			public void run()
			{
				try {
					PackageManager pm = getPackageManager();
					Intent sendIntent = new Intent(Intent.ACTION_SEND);
					PackageInfo packageInfo = pm.getPackageInfo(getApplicationInfo().packageName, 0);

					String fileName = packageInfo.applicationInfo.loadLabel(pm) + "_" + packageInfo.versionName + ".apk";

					sendIntent.putExtra(ShareActivity.EXTRA_FILENAME_LIST, fileName);

					File codeFile = new File(FileUtils.
							getApplicationDirectory(getApplicationContext()).getAbsolutePath()  + File.separator + fileName);

					codeFile = FileUtils.getUniqueFile(codeFile, true);

					FileUtils.copyFile(new File(getApplicationInfo().sourceDir), codeFile);

					sendIntent.putExtra(Intent.EXTRA_STREAM, Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
							? FileProvider.getUriForFile(HomeActivity.this, getPackageName() + ".provider", codeFile)
							: Uri.fromFile(codeFile))
							.setType(FileUtils.getFileContentType(codeFile.getAbsolutePath()))
							.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

					startActivity(Intent.createChooser(sendIntent, getString(R.string.text_fileShareAppChoose)));
				} catch (IOException e) {
					e.printStackTrace();
				} catch (PackageManager.NameNotFoundException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
