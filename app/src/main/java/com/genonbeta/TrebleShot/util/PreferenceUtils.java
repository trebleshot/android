package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.genonbeta.android.framework.preference.SuperPreferences;

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
public class PreferenceUtils extends com.genonbeta.android.framework.util.PreferenceUtils
{
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
