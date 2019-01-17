package com.genonbeta.TrebleShot.object;

/**
 * created by: Veli
 * date: 18.01.2018 20:53
 */

public interface Comparable
{
    boolean comparisonSupported();

    String getComparableName();

    long getComparableDate();

    long getComparableSize();
}
