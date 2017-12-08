package com.genonbeta.TrebleShot.util;

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
}
