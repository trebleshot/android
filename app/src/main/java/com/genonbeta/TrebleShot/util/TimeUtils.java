package com.genonbeta.TrebleShot.util;

import android.content.Context;

import com.genonbeta.TrebleShot.R;

/**
 * created by: Veli
 * date: 12.11.2017 10:53
 */

public class TimeUtils
{

	public static String getDuration(long milliseconds)
	{
		StringBuilder string = new StringBuilder();

		long hours = (milliseconds / 3600000);
		long minutes = (milliseconds - (hours * 3600000)) / 60000;
		long seconds = (milliseconds - (hours * 3600000) - (minutes * 60000)) / 1000;

		if (hours > 0) {
			if (hours < 10)
				string.append("0");

			string.append(hours);
			string.append(":");
		}

		if (minutes < 10)
			string.append("0");

		string.append(minutes);
		string.append(":");

		if (seconds < 10)
			string.append("0");

		string.append(seconds);

		return string.toString();
	}

	public static String getTimeAgo(Context context, long time)
	{
		int differ = (int) ((System.currentTimeMillis() - time) / 1000);

		if (differ == 0)
			return context.getString(R.string.text_timeJustNow);
		else if (differ < 60)
			return context.getResources().getQuantityString(R.plurals.text_secondsAgo, differ, differ);
		else if (differ < 3600)
			return context.getResources().getQuantityString(R.plurals.text_minutesAgo, differ / 60, differ / 60);

		return context.getString(R.string.text_longAgo);
	}
}
