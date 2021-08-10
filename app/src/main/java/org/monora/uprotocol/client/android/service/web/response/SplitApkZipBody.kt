/*
 * Copyright (C) 2021 Veli Tasalı
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

package org.monora.uprotocol.client.android.service.web.response

import com.yanzhenjie.andserver.http.ResponseBody
import com.yanzhenjie.andserver.util.MediaType
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SplitApkZipBody(private val contents: List<String>) : ResponseBody {
    override fun isRepeatable(): Boolean = false

    override fun contentLength(): Long = -1L

    override fun contentType(): MediaType = MediaType.ALL

    override fun writeTo(output: OutputStream) {
        val buffer = ByteArray(16 * 1024)
        val zipOutputStream = ZipOutputStream(output)

        zipOutputStream.setLevel(0)

        contents.forEach { content ->
            val file = File(content)

            FileInputStream(file).use { inputStream ->
                val entry = ZipEntry(file.name)
                entry.time = file.lastModified()
                zipOutputStream.putNextEntry(entry)

                var len: Int
                while (inputStream.read(buffer).also { len = it } != -1) {
                    if (len > 0) {
                        zipOutputStream.write(buffer, 0, len)
                    }
                }

                zipOutputStream.closeEntry()
            }
        }

        zipOutputStream.finish()
        zipOutputStream.flush()
        zipOutputStream.close()
    }
}
