package com.genonbeta.TrebleShot.util;

import com.genonbeta.android.framework.util.Interrupter;

/**
 * created by: Veli
 * date: 11.02.2018 19:37
 */

abstract public class InterruptAwareJob
{
    abstract protected void onRun();

    public void run(Interrupter interrupter)
    {
        onRun();
        interrupter.removeClosers();
    }
}
