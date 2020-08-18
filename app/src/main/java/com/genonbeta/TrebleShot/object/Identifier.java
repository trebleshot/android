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

package com.genonbeta.TrebleShot.object;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import androidx.annotation.Nullable;

public class Identifier implements Parcelable
{
    public String key;
    public String value;
    public boolean isNull;

    public Identifier()
    {

    }

    protected Identifier(Parcel in)
    {
        key = in.readString();
        value = in.readString();
        isNull = in.readByte() != 0;
    }

    public static final Parcelable.Creator<Identifier> CREATOR = new Parcelable.Creator<Identifier>()
    {
        @Override
        public Identifier createFromParcel(Parcel in)
        {
            return new Identifier(in);
        }

        @Override
        public Identifier[] newArray(int size)
        {
            return new Identifier[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public boolean equals(@Nullable Object obj)
    {
        if (obj instanceof Identifier) {
            Identifier other = (Identifier) obj;
            return key.equals(other.key) && isNull == other.isNull && (isNull || value.equals(other.value));
        }

        return super.equals(obj);
    }

    public static Identifier from(Enum<?> key, Object value)
    {
        return from(key.toString(), value);
    }

    public static Identifier from(String key, Object value)
    {
        Identifier identifier = new Identifier();
        identifier.key = key;
        identifier.isNull = value == null;
        identifier.value = identifier.isNull ? "" : String.valueOf(value);

        return identifier;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(key);
        dest.writeString(value);
        dest.writeByte((byte) (isNull ? 1 : 0));
    }
}

