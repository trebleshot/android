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
package com.genonbeta.TrebleShot.util

import android.content.*
import android.graphics.drawable.Drawable
import androidx.collection.ArrayMap
import com.genonbeta.TrebleShot.R

/*
 * Modified-by: veli
 * Date: 16/11/2018 18:57
 */   object MimeIconUtils {
    private val sMimeIcons: MutableMap<String?, Int?> = ArrayMap()
    private fun add(mimeType: String, resId: Int) {
        if (sMimeIcons.put(mimeType, resId) != null) {
            throw RuntimeException("$mimeType already registered!")
        }
    }

    fun loadMimeIcon(context: Context?, mimeType: String?): Drawable {
        return ContextCompat.getDrawable(context, loadMimeIcon(mimeType))
    }

    fun loadMimeIcon(mimeType: String?): Int {
        if (DocumentsContract.Document.MIME_TYPE_DIR == mimeType) return R.drawable.ic_folder_white_24dp
        val resId = sMimeIcons[mimeType]
        if (resId != null) return resId
        if (mimeType == null) return R.drawable.ic_insert_drive_file_white_24dp
        val typeOnly = mimeType.split("/".toRegex()).toTypedArray()[0]
        return if ("audio" == typeOnly) {
            R.drawable.ic_music_box_white_24dp
        } else if ("image" == typeOnly) {
            R.drawable.ic_photo_white_24dp
        } else if ("text" == typeOnly) {
            R.drawable.ic_file_document_box_white_24dp
        } else if ("video" == typeOnly) {
            R.drawable.ic_video_white_24dp
        } else {
            R.drawable.ic_insert_drive_file_white_24dp
        }
    }

    init {
        var icon: Int
        // Package
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_android_head_white_24dp
        add("application/vnd.android.package-archive", com.genonbeta.TrebleShot.util.icon)
        // Audio
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_music_box_white_24dp
        add("application/ogg", com.genonbeta.TrebleShot.util.icon)
        add("application/x-flac", com.genonbeta.TrebleShot.util.icon)
        // Certificate
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_certificate_white_24dp
        add("application/pgp-keys", com.genonbeta.TrebleShot.util.icon)
        add("application/pgp-signature", com.genonbeta.TrebleShot.util.icon)
        add("application/x-pkcs12", com.genonbeta.TrebleShot.util.icon)
        add("application/x-pkcs7-certreqresp", com.genonbeta.TrebleShot.util.icon)
        add("application/x-pkcs7-crl", com.genonbeta.TrebleShot.util.icon)
        add("application/x-x509-ca-cert", com.genonbeta.TrebleShot.util.icon)
        add("application/x-x509-user-cert", com.genonbeta.TrebleShot.util.icon)
        add("application/x-pkcs7-certificates", com.genonbeta.TrebleShot.util.icon)
        add("application/x-pkcs7-mime", com.genonbeta.TrebleShot.util.icon)
        add("application/x-pkcs7-signature", com.genonbeta.TrebleShot.util.icon)
        // Source code
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_code_tags_white_24dp
        add("application/rdf+xml", com.genonbeta.TrebleShot.util.icon)
        add("application/rss+xml", com.genonbeta.TrebleShot.util.icon)
        add("application/x-object", com.genonbeta.TrebleShot.util.icon)
        add("application/xhtml+xml", com.genonbeta.TrebleShot.util.icon)
        add("text/css", com.genonbeta.TrebleShot.util.icon)
        add("text/html", com.genonbeta.TrebleShot.util.icon)
        add("text/xml", com.genonbeta.TrebleShot.util.icon)
        add("text/x-c++hdr", com.genonbeta.TrebleShot.util.icon)
        add("text/x-c++src", com.genonbeta.TrebleShot.util.icon)
        add("text/x-chdr", com.genonbeta.TrebleShot.util.icon)
        add("text/x-csrc", com.genonbeta.TrebleShot.util.icon)
        add("text/x-dsrc", com.genonbeta.TrebleShot.util.icon)
        add("text/x-csh", com.genonbeta.TrebleShot.util.icon)
        add("text/x-haskell", com.genonbeta.TrebleShot.util.icon)
        add("text/x-java", com.genonbeta.TrebleShot.util.icon)
        add("text/x-literate-haskell", com.genonbeta.TrebleShot.util.icon)
        add("text/x-pascal", com.genonbeta.TrebleShot.util.icon)
        add("text/x-tcl", com.genonbeta.TrebleShot.util.icon)
        add("text/x-tex", com.genonbeta.TrebleShot.util.icon)
        add("application/x-latex", com.genonbeta.TrebleShot.util.icon)
        add("application/x-texinfo", com.genonbeta.TrebleShot.util.icon)
        add("application/atom+xml", com.genonbeta.TrebleShot.util.icon)
        add("application/ecmascript", com.genonbeta.TrebleShot.util.icon)
        add("application/json", com.genonbeta.TrebleShot.util.icon)
        add("application/javascript", com.genonbeta.TrebleShot.util.icon)
        add("application/xml", com.genonbeta.TrebleShot.util.icon)
        add("text/javascript", com.genonbeta.TrebleShot.util.icon)
        add("application/x-javascript", com.genonbeta.TrebleShot.util.icon)
        // Compressed
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_zip_box_white_24dp
        add("application/mac-binhex40", com.genonbeta.TrebleShot.util.icon)
        add("application/rar", com.genonbeta.TrebleShot.util.icon)
        add("application/zip", com.genonbeta.TrebleShot.util.icon)
        add("application/x-apple-diskimage", com.genonbeta.TrebleShot.util.icon)
        add("application/x-debian-package", com.genonbeta.TrebleShot.util.icon)
        add("application/x-gtar", com.genonbeta.TrebleShot.util.icon)
        add("application/x-iso9660-image", com.genonbeta.TrebleShot.util.icon)
        add("application/x-lha", com.genonbeta.TrebleShot.util.icon)
        add("application/x-lzh", com.genonbeta.TrebleShot.util.icon)
        add("application/x-lzx", com.genonbeta.TrebleShot.util.icon)
        add("application/x-stuffit", com.genonbeta.TrebleShot.util.icon)
        add("application/x-tar", com.genonbeta.TrebleShot.util.icon)
        add("application/x-webarchive", com.genonbeta.TrebleShot.util.icon)
        add("application/x-webarchive-xml", com.genonbeta.TrebleShot.util.icon)
        add("application/gzip", com.genonbeta.TrebleShot.util.icon)
        add("application/x-7z-compressed", com.genonbeta.TrebleShot.util.icon)
        add("application/x-deb", com.genonbeta.TrebleShot.util.icon)
        add("application/x-rar-compressed", com.genonbeta.TrebleShot.util.icon)
        // Contact
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_account_box_white_24dp
        add("text/x-vcard", com.genonbeta.TrebleShot.util.icon)
        add("text/vcard", com.genonbeta.TrebleShot.util.icon)
        // Event
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_calendar_white_24dp
        add("text/calendar", com.genonbeta.TrebleShot.util.icon)
        add("text/x-vcalendar", com.genonbeta.TrebleShot.util.icon)
        // Font
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_file_font_white_24dp
        add("application/x-font", com.genonbeta.TrebleShot.util.icon)
        add("application/font-woff", com.genonbeta.TrebleShot.util.icon)
        add("application/x-font-woff", com.genonbeta.TrebleShot.util.icon)
        add("application/x-font-ttf", com.genonbeta.TrebleShot.util.icon)
        // Image
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_photo_white_24dp
        add("application/vnd.oasis.opendocument.graphics", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.oasis.opendocument.graphics-template", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.oasis.opendocument.image", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.stardivision.draw", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.sun.xml.draw", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.sun.xml.draw.template", com.genonbeta.TrebleShot.util.icon)
        // PDF
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_file_pdf_box_white_24dp
        add("application/pdf", com.genonbeta.TrebleShot.util.icon)
        // Presentation
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_file_presentation_box_white_24dp
        add("application/vnd.stardivision.impress", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.sun.xml.impress", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.sun.xml.impress.template", com.genonbeta.TrebleShot.util.icon)
        add("application/x-kpresenter", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.oasis.opendocument.presentation", com.genonbeta.TrebleShot.util.icon)
        // Spreadsheet
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_google_spreadsheet_white_24dp
        add("application/vnd.oasis.opendocument.spreadsheet", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.oasis.opendocument.spreadsheet-template", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.stardivision.calc", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.sun.xml.calc", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.sun.xml.calc.template", com.genonbeta.TrebleShot.util.icon)
        add("application/x-kspread", com.genonbeta.TrebleShot.util.icon)
        // Document
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_file_document_box_white_24dp
        add("application/vnd.oasis.opendocument.text", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.oasis.opendocument.text-master", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.oasis.opendocument.text-template", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.oasis.opendocument.text-web", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.stardivision.writer", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.stardivision.writer-global", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.sun.xml.writer", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.sun.xml.writer.global", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.sun.xml.writer.template", com.genonbeta.TrebleShot.util.icon)
        add("application/x-abiword", com.genonbeta.TrebleShot.util.icon)
        add("application/x-kword", com.genonbeta.TrebleShot.util.icon)
        // Video
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_video_white_24dp
        add("application/x-quicktimeplayer", com.genonbeta.TrebleShot.util.icon)
        add("application/x-shockwave-flash", com.genonbeta.TrebleShot.util.icon)
        // Word
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_file_word_box_white_24dp
        add("application/msword", com.genonbeta.TrebleShot.util.icon)
        add(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            com.genonbeta.TrebleShot.util.icon
        )
        add(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
            com.genonbeta.TrebleShot.util.icon
        )
        // Excel
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_file_excel_box_white_24dp
        add("application/vnd.ms-excel", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", com.genonbeta.TrebleShot.util.icon)
        add("application/vnd.openxmlformats-officedocument.spreadsheetml.template", com.genonbeta.TrebleShot.util.icon)
        // Powerpoint
        com.genonbeta.TrebleShot.util.icon = R.drawable.ic_file_powerpoint_box_24dp
        add("application/vnd.ms-powerpoint", com.genonbeta.TrebleShot.util.icon)
        add(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            com.genonbeta.TrebleShot.util.icon
        )
        add("application/vnd.openxmlformats-officedocument.presentationml.template", com.genonbeta.TrebleShot.util.icon)
        add(
            "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
            com.genonbeta.TrebleShot.util.icon
        )
    }
}