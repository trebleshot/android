package com.genonbeta.android.framework.util.date;

import android.support.annotation.NonNull;

import com.genonbeta.android.framework.util.listing.ComparableMerger;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * created by: Veli
 * date: 29.03.2018 01:23
 */
public class DateMerger<T>
		extends ComparableMerger<T>
{
	private long mTime;
	private int mYear;
	private int mMonth;
	private int mDay;
	private int mDayOfYear;

	public DateMerger(long time)
	{
		Calendar calendar = GregorianCalendar.getInstance();

		calendar.setTime(new Date(time));

		mTime = time;
		mYear = calendar.get(Calendar.YEAR);
		mMonth = calendar.get(Calendar.MONTH);
		mDay = calendar.get(Calendar.DAY_OF_MONTH);
		mDayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
	}

	@Override
	public int compareTo(@NonNull ComparableMerger<T> merger)
	{
		if (!(merger instanceof DateMerger))
			return -1;

		DateMerger o = (DateMerger) merger;

		if (getYear() < o.getYear())
			return -1;
		else if (getYear() > o.getYear())
			return 1;
		else if (getDayOfYear() == o.getDayOfYear())
			return 0;

		return getDayOfYear() < o.getDayOfYear() ? -1 : 1;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof DateMerger))
			return false;

		DateMerger dateMerger = (DateMerger) obj;

		return getYear() == dateMerger.getYear()
				&& getMonth() == dateMerger.getMonth()
				&& getDay() == dateMerger.getDay();
	}

	public int getDay()
	{
		return mDay;
	}

	public int getDayOfYear()
	{
		return mDayOfYear;
	}

	public int getMonth()
	{
		return mMonth;
	}

	public long getTime()
	{
		return mTime;
	}

	public int getYear()
	{
		return mYear;
	}
}
