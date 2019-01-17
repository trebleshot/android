package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.text.format.DateUtils;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.android.framework.util.date.ElapsedTime;

import java.util.ArrayList;
import java.util.List;

/**
 * created by: Veli
 * date: 12.11.2017 10:53
 */

public class TimeUtils
{
    public static CharSequence formatDateTime(Context context, long millis)
    {
        return DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE);
    }

    public static String getDuration(long milliseconds)
    {
        StringBuilder string = new StringBuilder();

        ElapsedTime.ElapsedTimeCalculator calculator = new ElapsedTime.ElapsedTimeCalculator(milliseconds / 1000);

        long hours = calculator.crop(3600);
        long minutes = calculator.crop(60);
        long seconds = calculator.getLeftTime();

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

    public static String getFriendlyElapsedTime(Context context, long estimatedTime)
    {
        ElapsedTime elapsedTime = new ElapsedTime(estimatedTime);
        List<String> appendList = new ArrayList<>();

        if (elapsedTime.getYears() > 0)
            appendList.add(context.getString(R.string.text_yearCountShort, elapsedTime.getYears()));

        if (elapsedTime.getMonths() > 0)
            appendList.add(context.getString(R.string.text_monthCountShort, elapsedTime.getMonths()));

        if (elapsedTime.getYears() == 0) {
            if (elapsedTime.getDays() > 0)
                appendList.add(context.getString(R.string.text_dayCountShort, elapsedTime.getDays()));

            if (elapsedTime.getMonths() == 0) {
                if (elapsedTime.getHours() > 0)
                    appendList.add(context.getString(R.string.text_hourCountShort, elapsedTime.getHours()));

                if (elapsedTime.getDays() == 0) {
                    if (elapsedTime.getMinutes() > 0)
                        appendList.add(context.getString(R.string.text_minuteCountShort, elapsedTime.getMinutes()));

                    if (elapsedTime.getHours() == 0)
                        // always applied
                        appendList.add(context.getString(R.string.text_secondCountShort, elapsedTime.getSeconds()));
                }
            }
        }

        StringBuilder stringBuilder = new StringBuilder();

        for (String appendItem : appendList) {
            if (stringBuilder.length() > 0)
                stringBuilder.append(" ");

            stringBuilder.append(appendItem);
        }

        return stringBuilder.toString();
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
