package com.genonbeta.android.framework.util.date;

/**
 * created by: Veli
 * date: 6.02.2018 12:27
 */

public class ElapsedTime
{
	private long mElapsedTime;
	private long mYears;
	private long mMonths;
	private long mDays;
	private long mHours;
	private long mMinutes;
	private long mSeconds;

	public ElapsedTime(long time)
	{
		set(time);
	}

	public long getElapsedTime()
	{
		return mElapsedTime;
	}

	public long getDays()
	{
		return mDays;
	}

	public long getHours()
	{
		return mHours;
	}

	public long getMinutes()
	{
		return mMinutes;
	}

	public long getMonths()
	{
		return mMonths;
	}

	public long getSeconds()
	{
		return mSeconds;
	}

	public long getYears()
	{
		return mYears;
	}

	public void set(long time)
	{
		mElapsedTime = time;

		ElapsedTimeCalculator calculator = new ElapsedTimeCalculator(time / 1000);

		mYears = calculator.crop(62208000);
		mMonths = calculator.crop(2592000);
		mDays = calculator.crop(86400);
		mHours = calculator.crop(3600);
		mMinutes = calculator.crop(60);
		mSeconds = calculator.getLeftTime();
	}

	public static class ElapsedTimeCalculator
	{
		private long mTime;

		public ElapsedTimeCalculator(long time)
		{
			mTime = time;
		}

		public long crop(long summonBy)
		{
			long result = 0;

			if (getLeftTime() > summonBy) {
				result = getLeftTime() / summonBy;
				setTime(getLeftTime() - (result * summonBy));
			}

			return result;
		}

		public long getLeftTime()
		{
			return mTime;
		}

		public void setTime(long time)
		{
			mTime = time;
		}
	}
}
