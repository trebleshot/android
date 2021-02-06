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
package com.genonbeta.TrebleShot.io

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator

class Containable : Parcelable {
    var targetUri: Uri?
    var children: Array<Uri?>?

    constructor(`in`: Parcel) {
        val uriClassLoader = Uri::class.java.classLoader
        targetUri = `in`.readParcelable(uriClassLoader)
        children = `in`.createTypedArray(Uri.CREATOR)
    }

    constructor(targetUri: Uri?, children: List<Uri>) {
        this.targetUri = targetUri
        this.children = arrayOfNulls(children.size)
        children.toArray<Uri>(this.children)
    }

    constructor(targetUri: Uri?, children: Array<Uri?>?) {
        this.targetUri = targetUri
        this.children = children
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(obj: Any?): Boolean {
        return if (obj is Containable) targetUri == obj.targetUri else super.equals(obj)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(targetUri, flags)
        dest.writeTypedArray(children, flags)
    }

    companion object {
        val CREATOR: Creator<Containable> = object : Creator<Containable?> {
            override fun createFromParcel(source: Parcel): Containable? {
                return Containable(source)
            }

            override fun newArray(size: Int): Array<Containable?> {
                return arrayOfNulls(size)
            }
        }
    }
}