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

import android.content.Context
import com.genonbeta.android.framework.io.DocumentFile
import com.yanzhenjie.andserver.http.ResponseBody
import com.yanzhenjie.andserver.util.MediaType
import java.io.File
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileZipBody(context: Context, private val file: DocumentFile) : ResponseBody {
    private val context = WeakReference(context)

    override fun isRepeatable(): Boolean = false

    override fun contentLength(): Long = -1L

    override fun contentType(): MediaType = MediaType.ALL

    override fun writeTo(output: OutputStream) {
        val buffer = ByteArray(16 * 1024)
        val context = context.get() ?: return
        val zipOutputStream = ZipOutputStream(output)

        zipOutputStream.setLevel(0)
        //zipOutputStream.setMethod(ZipEntry.STORED);

        fun travel(file: DocumentFile, path: String? = null) {
            val childPath = if (path != null) path + File.separator + file.getName() else file.getName()

            if (file.isDirectory()) {
                for (childFile in file.listFiles(context)) {
                    travel(childFile, childPath)
                }

                return
            }

            try {
                context.contentResolver.openInputStream(file.getUri())?.use { inputStream ->
                    val entry = ZipEntry(childPath)
                    entry.time = file.getLastModified()

                    zipOutputStream.putNextEntry(entry)

                    var len: Int
                    while (inputStream.read(buffer).also { len = it } != -1) {
                        if (len > 0) {
                            zipOutputStream.write(buffer, 0, len)
                        }
                    }

                    zipOutputStream.closeEntry()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        travel(file)

        zipOutputStream.finish()
        zipOutputStream.flush()
        zipOutputStream.close()
    }
}
