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

package com.genonbeta.TrebleShot.io;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

public class ParcelableFile implements Parcelable
{
    public static final Creator<ParcelableFile> CREATOR = new Creator<ParcelableFile>()
    {
        @Override
        public ParcelableFile createFromParcel(Parcel source)
        {
            return new ParcelableFile(source);
        }

        @Override
        public ParcelableFile[] newArray(int size)
        {
            return new ParcelableFile[size];
        }
    };

    public File file;

    private ParcelableFile(Parcel in) {
        file = (File) in.readSerializable();
    }

    public ParcelableFile(File file) {
        this.file = file;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeSerializable(file);
    }
}
