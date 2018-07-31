package com.genonbeta.android.framework.util;

/**
 * created by: Veli
 * date: 9.12.2017 00:36
 */

public class MathUtils
{
	public static int calculatePercentage(long max, long current)
	{
		return (int) (((float) 100 / max) * current);
	}

	public static int compare(long x, long y)
	{
		return x < y ? -1 : (x == y ? 0 : 1);
	}
}
