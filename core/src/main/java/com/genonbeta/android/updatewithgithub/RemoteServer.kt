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
package com.genonbeta.android.updatewithgithub

import com.github.kevinsawicki.http.HttpRequest
import java.io.IOException
import java.net.URLEncoder

class RemoteServer(var address: String) {
    @Throws(IOException::class)
    fun connect(postKey: String?, postValue: String?): String {
        val request = HttpRequest.get(address)
        val output = StringBuilder()
        request.readTimeout(5000)
        if (postKey != null && postValue != null) request.send(postKey + "=" + URLEncoder.encode(postValue, "UTF-8"))
        request.receive(output)
        return output.toString()
    }
}