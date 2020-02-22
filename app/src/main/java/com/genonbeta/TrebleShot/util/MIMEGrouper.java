/*
 * Copyright (C) 2020 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.util;

import java.io.File;

public class MIMEGrouper
{
    public static final String TYPE_GENERIC = "*";

    private String mMajor;
    private String mMinor;
    private boolean mLocked;

    public boolean isLocked()
    {
        return mLocked;
    }

    public String getMajor()
    {
        return mMajor == null ? TYPE_GENERIC : mMajor;
    }

    public String getMinor()
    {
        return mMinor == null ? TYPE_GENERIC : mMinor;
    }

    public void process(String mimeType)
    {
        if (mimeType == null || mimeType.length() < 3 || !mimeType.contains(File.separator))
            return;

        String[] splitMIME = mimeType.split(File.separator);

        process(splitMIME[0], splitMIME[1]);
    }

    public void process(String major, String minor)
    {
        if (mMajor == null || mMinor == null) {
            mMajor = major;
            mMinor = minor;
        } else if (getMajor().equals(TYPE_GENERIC))
            mLocked = true;
        else if (!getMajor().equals(major)) {
            mMajor = TYPE_GENERIC;
            mMinor = TYPE_GENERIC;

            mLocked = true;
        } else if (!getMinor().equals(minor)) {
            mMinor = TYPE_GENERIC;
        }
    }

    @Override
    public String toString()
    {
        return getMajor() + File.separator + getMinor();
    }
}
