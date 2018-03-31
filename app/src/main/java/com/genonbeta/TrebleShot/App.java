package com.genonbeta.TrebleShot;

import android.app.Application;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.PreferenceUtils;
import com.ironz.binaryprefs.BinaryPreferencesBuilder;
import com.ironz.binaryprefs.Preferences;
import com.ironz.binaryprefs.PreferencesEditor;

import java.util.Map;

/**
 * created by: Veli
 * date: 25.02.2018 01:23
 */

public class App extends Application
{
	public static final String TAG = App.class.getSimpleName();

	@Override
	public void onCreate()
	{
		super.onCreate();

		SharedPreferences defaultPreferences = AppUtils.getDefaultLocalPreferences(this);

		boolean nsdDefined = defaultPreferences.contains("nsd_enabled");

		PreferenceManager.setDefaultValues(this, R.xml.preferences_defaults_main, false);

		if (!nsdDefined)
			defaultPreferences.edit()
					.putBoolean("nsd_enabled", Build.VERSION.SDK_INT >= 19)
					.apply();

		PreferenceUtils.syncDefaults(getApplicationContext());
	}
}
