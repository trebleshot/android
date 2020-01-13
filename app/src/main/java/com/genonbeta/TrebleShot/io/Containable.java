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

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Containable implements Parcelable
{
    public static final Creator<Containable> CREATOR = new Creator<Containable>()
    {
        @Override
        public Containable createFromParcel(Parcel source)
        {
            return new Containable(source);
        }

        @Override
        public Containable[] newArray(int size)
        {
            return new Containable[size];
        }
    };

    public Uri targetUri;
    public Uri[] children;

    public Containable(Parcel in)
    {
        ClassLoader uriClassLoader = Uri.class.getClassLoader();

        targetUri = in.readParcelable(uriClassLoader);
        children = in.createTypedArray(Uri.CREATOR);
    }

    public Containable(Uri targetUri, List<Uri> children)
    {
        this.targetUri = targetUri;
        this.children = new Uri[children.size()];

        children.toArray(this.children);
    }

    public Containable(Uri targetUri, Uri[] children) {
        this.targetUri = targetUri;
        this.children = children;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof Containable ? targetUri.equals(((Containable) obj).targetUri) : super.equals(obj);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeParcelable(targetUri, flags);
        dest.writeTypedArray(children, flags);
    }
}
