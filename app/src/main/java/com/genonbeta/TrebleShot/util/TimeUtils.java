package com.genonbeta.TrebleShot.util;

import android.content.Context;

import com.genonbeta.TrebleShot.R;

/**
 * created by: Veli
 * date: 12.11.2017 10:53
 */

public class TimeUtils
{
	public static String getTimeAgo(Context context, long time)
	{
		int differ = (int) ((System.currentTimeMillis() - time) / 1000);

		if (differ < 60)
			return context.getResources().getQuantityString(R.plurals.text_secondsAgo, differ, differ);
		if (differ < 3600)
			return context.getResources().getQuantityString(R.plurals.text_minutesAgo, differ / 60, differ / 60);

		return context.getString(R.string.text_longAgo);
	}
}
