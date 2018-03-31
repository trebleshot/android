package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ironz.binaryprefs.BinaryPreferencesBuilder;
import com.ironz.binaryprefs.Preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * created by: veli
 * date: 31.03.2018 13:48
 */
public class PreferenceUtils
{
	public static final String TAG = PreferenceUtils.class.getSimpleName();
	public static final String KEY_SYNC_TIME = "__SYNC_TIME";

	public static <T> boolean applyGeneric(String key, T object, SharedPreferences.Editor editor)
	{
		Log.d(PreferenceUtils.class.getSimpleName(), "Put setting: " + key + " => " + String.valueOf(object));

		if (object instanceof Boolean)
			editor.putBoolean(key, (Boolean) object);
		else if (object instanceof Float)
			editor.putFloat(key, (Float) object);
		else if (object instanceof Integer)
			editor.putInt(key, (Integer) object);
		else if (object instanceof Long)
			editor.putLong(key, (Long) object);
		else if (object instanceof String)
			editor.putString(key, (String) object);
		else if (object instanceof Set)
			editor.putStringSet(key, (Set<String>) object);
		else
			return false;

		return true;
	}

	public static int sync(SharedPreferences... objects)
	{
		if (objects.length < 2)
			return 0;

		List<SharedPreferences> preferencesList = new ArrayList<>(Arrays.asList(objects));

		Collections.sort(preferencesList, new Comparator<SharedPreferences>()
		{
			@Override
			public int compare(SharedPreferences source1, SharedPreferences source2)
			{
				long comp1 = source1.getLong(KEY_SYNC_TIME, source1.getAll().size());
				long comp2 = source2.getLong(KEY_SYNC_TIME, source2.getAll().size());

				// last updated must come first
				return MathUtils.compare(comp2, comp1);
			}
		});

		long syncTime = System.currentTimeMillis();
		SharedPreferences chosenSource = preferencesList.get(0);

		chosenSource.edit()
				.putLong(KEY_SYNC_TIME, syncTime)
				.apply();

		Map<String, ?> items = chosenSource.getAll();
		preferencesList.remove(0);

		int totalRegistered = 0;

		for (SharedPreferences syncingPreference : preferencesList)
			totalRegistered += syncPreferences(items, syncingPreference, syncTime);

		return totalRegistered;
	}

	public static int syncPreferences(SharedPreferences from, SharedPreferences to)
	{
		return syncPreferences(from.getAll(), to);
	}

	public static int syncPreferences(Map<String, ?> from, SharedPreferences to)
	{
		return syncPreferences(from, to, System.currentTimeMillis());
	}

	public static int syncPreferences(Map<String, ?> from, SharedPreferences to, long syncTime)
	{
		int totalRegistered = 0;
		SharedPreferences.Editor editor = to.edit();

		for (String key : from.keySet())
			if (applyGeneric(key, from.get(key), editor))
				totalRegistered++;

		editor.putLong(KEY_SYNC_TIME, syncTime)
				.apply();

		return totalRegistered;
	}

	public static void syncDefaults(Context context)
	{
		syncDefaults(context, true, false);
	}

	public static void syncDefaults(Context context, boolean compare, boolean fromXml)
	{
		SharedPreferences preferences = AppUtils.getDefaultLocalPreferences(context);
		SharedPreferences binaryPreferences = AppUtils.getDefaultPreferences(context);

		if (compare)
			sync(preferences, binaryPreferences);
		else {
			if (fromXml)
				syncPreferences(preferences, binaryPreferences);
			else
				syncPreferences(binaryPreferences, preferences);
		}
	}
}
