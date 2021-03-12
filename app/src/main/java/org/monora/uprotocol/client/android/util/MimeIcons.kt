/*
 * Copyright (C) 2019 Veli Tasalı
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
package org.monora.uprotocol.client.android.util

import android.content.*
import android.graphics.drawable.Drawable
import android.provider.DocumentsContract
import androidx.collection.ArrayMap
import androidx.core.content.ContextCompat
import org.monora.uprotocol.client.android.R

/*
 * Modified-by: veli
 * Date: 16/11/2018 18:57
 */
object MimeIcons {
    private val mimeIcons: MutableMap<String, Int> = ArrayMap()

    private fun add(mimeType: String, resId: Int) {
        if (mimeIcons.put(mimeType, resId) != null) {
            throw RuntimeException("$mimeType already registered!")
        }
    }

    fun loadMimeIcon(context: Context, mimeType: String?): Drawable {
        return ContextCompat.getDrawable(context, loadMimeIcon(mimeType))!!
    }

    fun loadMimeIcon(mimeType: String?): Int {
        if (DocumentsContract.Document.MIME_TYPE_DIR == mimeType) return R.drawable.ic_folder_white_24dp

        val resId = mimeIcons[mimeType]

        if (resId != null) return resId
        if (mimeType == null) return R.drawable.ic_insert_drive_file_white_24dp

        return when (mimeType.split("/".toRegex()).toTypedArray()[0]) {
            "audio" -> R.drawable.ic_music_box_white_24dp
            "image" -> R.drawable.ic_photo_white_24dp
            "text" -> R.drawable.ic_file_document_box_white_24dp
            "video" -> R.drawable.ic_video_white_24dp
            else -> R.drawable.ic_insert_drive_file_white_24dp
        }
    }

    init {
        // Package
        var icon: Int = R.drawable.ic_android_head_white_24dp
        add("application/vnd.android.package-archive", icon)
        // Audio
        icon = R.drawable.ic_music_box_white_24dp
        add("application/ogg", icon)
        add("application/x-flac", icon)
        // Certificate
        icon = R.drawable.ic_certificate_white_24dp
        add("application/pgp-keys", icon)
        add("application/pgp-signature", icon)
        add("application/x-pkcs12", icon)
        add("application/x-pkcs7-certreqresp", icon)
        add("application/x-pkcs7-crl", icon)
        add("application/x-x509-ca-cert", icon)
        add("application/x-x509-user-cert", icon)
        add("application/x-pkcs7-certificates", icon)
        add("application/x-pkcs7-mime", icon)
        add("application/x-pkcs7-signature", icon)
        // Source code
        icon = R.drawable.ic_code_tags_white_24dp
        add("application/rdf+xml", icon)
        add("application/rss+xml", icon)
        add("application/x-object", icon)
        add("application/xhtml+xml", icon)
        add("text/css", icon)
        add("text/html", icon)
        add("text/xml", icon)
        add("text/x-c++hdr", icon)
        add("text/x-c++src", icon)
        add("text/x-chdr", icon)
        add("text/x-csrc", icon)
        add("text/x-dsrc", icon)
        add("text/x-csh", icon)
        add("text/x-haskell", icon)
        add("text/x-java", icon)
        add("text/x-literate-haskell", icon)
        add("text/x-pascal", icon)
        add("text/x-tcl", icon)
        add("text/x-tex", icon)
        add("application/x-latex", icon)
        add("application/x-texinfo", icon)
        add("application/atom+xml", icon)
        add("application/ecmascript", icon)
        add("application/json", icon)
        add("application/javascript", icon)
        add("application/xml", icon)
        add("text/javascript", icon)
        add("application/x-javascript", icon)
        // Compressed
        icon = R.drawable.ic_zip_box_white_24dp
        add("application/mac-binhex40", icon)
        add("application/rar", icon)
        add("application/zip", icon)
        add("application/x-apple-diskimage", icon)
        add("application/x-debian-package", icon)
        add("application/x-gtar", icon)
        add("application/x-iso9660-image", icon)
        add("application/x-lha", icon)
        add("application/x-lzh", icon)
        add("application/x-lzx", icon)
        add("application/x-stuffit", icon)
        add("application/x-tar", icon)
        add("application/x-webarchive", icon)
        add("application/x-webarchive-xml", icon)
        add("application/gzip", icon)
        add("application/x-7z-compressed", icon)
        add("application/x-deb", icon)
        add("application/x-rar-compressed", icon)
        // Contact
        icon = R.drawable.ic_account_box_white_24dp
        add("text/x-vcard", icon)
        add("text/vcard", icon)
        // Event
        icon = R.drawable.ic_calendar_white_24dp
        add("text/calendar", icon)
        add("text/x-vcalendar", icon)
        // Font
        icon = R.drawable.ic_file_font_white_24dp
        add("application/x-font", icon)
        add("application/font-woff", icon)
        add("application/x-font-woff", icon)
        add("application/x-font-ttf", icon)
        // Image
        icon = R.drawable.ic_photo_white_24dp
        add("application/vnd.oasis.opendocument.graphics", icon)
        add("application/vnd.oasis.opendocument.graphics-template", icon)
        add("application/vnd.oasis.opendocument.image", icon)
        add("application/vnd.stardivision.draw", icon)
        add("application/vnd.sun.xml.draw", icon)
        add("application/vnd.sun.xml.draw.template", icon)
        // PDF
        icon = R.drawable.ic_file_pdf_box_white_24dp
        add("application/pdf", icon)
        // Presentation
        icon = R.drawable.ic_file_presentation_box_white_24dp
        add("application/vnd.stardivision.impress", icon)
        add("application/vnd.sun.xml.impress", icon)
        add("application/vnd.sun.xml.impress.template", icon)
        add("application/x-kpresenter", icon)
        add("application/vnd.oasis.opendocument.presentation", icon)
        // Spreadsheet
        icon = R.drawable.ic_google_spreadsheet_white_24dp
        add("application/vnd.oasis.opendocument.spreadsheet", icon)
        add("application/vnd.oasis.opendocument.spreadsheet-template", icon)
        add("application/vnd.stardivision.calc", icon)
        add("application/vnd.sun.xml.calc", icon)
        add("application/vnd.sun.xml.calc.template", icon)
        add("application/x-kspread", icon)
        // Document
        icon = R.drawable.ic_file_document_box_white_24dp
        add("application/vnd.oasis.opendocument.text", icon)
        add("application/vnd.oasis.opendocument.text-master", icon)
        add("application/vnd.oasis.opendocument.text-template", icon)
        add("application/vnd.oasis.opendocument.text-web", icon)
        add("application/vnd.stardivision.writer", icon)
        add("application/vnd.stardivision.writer-global", icon)
        add("application/vnd.sun.xml.writer", icon)
        add("application/vnd.sun.xml.writer.global", icon)
        add("application/vnd.sun.xml.writer.template", icon)
        add("application/x-abiword", icon)
        add("application/x-kword", icon)
        // Video
        icon = R.drawable.ic_video_white_24dp
        add("application/x-quicktimeplayer", icon)
        add("application/x-shockwave-flash", icon)
        // Word
        icon = R.drawable.ic_file_word_box_white_24dp
        add("application/msword", icon)
        add("application/vnd.openxmlformats-officedocument.wordprocessingml.document", icon)
        add("application/vnd.openxmlformats-officedocument.wordprocessingml.template", icon)
        // Excel
        icon = R.drawable.ic_file_excel_box_white_24dp
        add("application/vnd.ms-excel", icon)
        add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", icon)
        add("application/vnd.openxmlformats-officedocument.spreadsheetml.template", icon)
        // Powerpoint
        icon = R.drawable.ic_file_powerpoint_box_24dp
        add("application/vnd.ms-powerpoint", icon)
        add("application/vnd.openxmlformats-officedocument.presentationml.presentation", icon)
        add("application/vnd.openxmlformats-officedocument.presentationml.template", icon)
        add("application/vnd.openxmlformats-officedocument.presentationml.slideshow", icon)
    }
}