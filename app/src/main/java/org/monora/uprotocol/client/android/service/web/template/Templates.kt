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

package org.monora.uprotocol.client.android.service.web.template

import android.content.Context
import com.genonbeta.android.framework.util.Files
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.content.App
import org.monora.uprotocol.client.android.content.Image
import org.monora.uprotocol.client.android.content.Song
import org.monora.uprotocol.client.android.content.Video
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.model.FileModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.regex.Pattern

class Templates(context: Context) {
    private val _context = WeakReference(context)

    val context
        get() = _context.get() ?: throw IllegalStateException()

    private val templates = mutableMapOf<String, String>()

    private fun loadTemplate(assetPath: String): String {
        templates[assetPath]?.let { return it }

        val stream = ByteArrayOutputStream()
        val inputStream = context.assets.open(assetPath)

        try {
            var len: Int
            while (inputStream.read().also { len = it } != -1) {
                stream.write(len)
                stream.flush()
            }

            return stream.toString().also { templates[assetPath] = it }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return String()
    }

    private fun parseTemplate(templateIndex: String, values: Map<String, Any>): String {
        val builder = StringBuilder()
        val matcher = TEMPLATE_PATTERN.matcher(templateIndex)
        var previousLocation = 0
        while (matcher.find()) {
            builder.append(templateIndex, previousLocation, matcher.start())
            builder.append(values[matcher.group(1)])
            previousLocation = matcher.end()
        }
        if (previousLocation > -1 && previousLocation < templateIndex.length) {
            builder.append(templateIndex, previousLocation, templateIndex.length)
        }
        return builder.toString()
    }

    fun render(templatePath: String, values: Map<String, Any>): String {
        return parseTemplate(loadTemplate(templatePath), values)
    }

    fun <T> render(templatePath: String, list: List<T>, mapper: (T) -> Map<String, Any>): String {
        val templateIndex = loadTemplate(templatePath)
        val stringBuilder = StringBuilder()

        list.forEach {
            stringBuilder.append(parseTemplate(templateIndex, mapper(it)))
            stringBuilder.append("\n")
        }

        return stringBuilder.toString()
    }

    companion object {
        private val TEMPLATE_PATTERN: Pattern = Pattern.compile("\\$\\{([a-zA-Z_]+)\\}")
    }
}

private fun CharSequence?.escapeHtml(): String {
    if (this == null) {
        return String()
    }

    val out = StringBuilder()
    withinStyle(out, this, 0, this.length)
    return out.toString()
}

private fun withinStyle(out: StringBuilder, text: CharSequence, start: Int, end: Int) {
    var i = start
    while (i < end) {
        val c = text[i]
        if (c == '<') {
            out.append("&lt;")
        } else if (c == '>') {
            out.append("&gt;")
        } else if (c == '&') {
            out.append("&amp;")
        } else if (c.code in 0xD800..0xDFFF) {
            if (c.code < 0xDC00 && i + 1 < end) {
                val d = text[i + 1]
                if (d.code in 0xDC00..0xDFFF) {
                    i++
                    val codepoint = 0x010000 or (c.code - 0xD800 shl 10) or d.code - 0xDC00
                    out.append("&#").append(codepoint).append(";")
                }
            }
        } else if (c.code > 0x7E || c < ' ') {
            out.append("&#").append(c.code).append(";")
        } else if (c == ' ') {
            while (i + 1 < end && text[i + 1] == ' ') {
                out.append("&nbsp;")
                i++
            }
            out.append(' ')
        } else {
            out.append(c)
        }
        i++
    }
}

fun renderHome(templates: Templates, pageTitle: String, contentBody: String, uploadedContent: String = ""): String {
    return templates.render(
        "web/template/home.html",
        mapOf(
            "app_name" to templates.context.getString(R.string.app_name),
            "page_title" to pageTitle,
            "content_body" to contentBody,
            "web_share" to templates.context.getString(R.string.web_share),
            "uploaded_content" to if (uploadedContent.isEmpty()) uploadedContent else renderUploadedContent(
                templates, uploadedContent
            ),
            "web_share_description" to templates.context.getString(R.string.web_share_description),
        )
    )
}

fun renderContents(templates: Templates, list: List<Any>): String {
    if (list.isEmpty()) {
        return renderEmptyContent(templates)
    }

    val builder = StringBuilder()

    val apps = ArrayList<App>(0)
    val folders = ArrayList<FileModel>(0)
    val files = ArrayList<FileModel>(0)
    val songs = ArrayList<Song>(0)
    val images = ArrayList<Image>(0)
    val videos = ArrayList<Video>(0)
    val sharedByThirdParty = ArrayList<UTransferItem>(0)

    list.forEach {
        when (it) {
            is App -> apps.add(it)
            is FileModel -> if (it.file.isDirectory()) folders.add(it) else files.add(it)
            is Song -> songs.add(it)
            is Image -> images.add(it)
            is Video -> videos.add(it)
            is UTransferItem -> sharedByThirdParty.add(it)
        }
    }

    if (apps.isNotEmpty()) {
        builder.append(
            renderTitle(templates, templates.context.getString(R.string.apps))
        )
        builder.append(
            templates.render("web/template/list/app.html", apps) {
                mapOf(
                    "id" to it.hashCode(),
                    "title" to it.label.escapeHtml(),
                    "name" to "${it.label}_${it.versionName}.apk".escapeHtml(),
                    "size" to Files.formatLength(File(it.info.sourceDir).length()),
                    "downloadType" to if (it.isSplit) "zip" else "download",
                    "nameSuffix" to if (it.isSplit) ".zip" else "",
                    "sizeSuffix" to if (it.isSplit) "~ (split)" else "",
                )
            }
        )
    }

    if (folders.isNotEmpty()) {
        builder.append(
            renderTitle(templates, templates.context.getString(R.string.folders))
        )
        builder.append(
            templates.render("web/template/list/folder.html", folders) {
                mapOf(
                    "id" to it.hashCode(),
                    "name" to it.file.getName().escapeHtml(),
                    "filesCount" to templates.context.resources.getQuantityString(
                        R.plurals.files, it.indexCount, it.indexCount
                    ),
                )
            }
        )
    }

    if (files.isNotEmpty()) {
        builder.append(
            renderTitle(templates, templates.context.getString(R.string.files))
        )
        builder.append(
            templates.render("web/template/list/file.html", files) {
                mapOf(
                    "id" to it.hashCode(),
                    "name" to it.file.getName().escapeHtml(),
                    "size" to Files.formatLength(it.file.getLength()),
                )
            }
        )
    }

    if (songs.isNotEmpty()) {
        builder.append(
            renderTitle(templates, templates.context.getString(R.string.songs))
        )
        builder.append(
            templates.render("web/template/list/song.html", songs) {
                mapOf(
                    "id" to it.hashCode(),
                    "title" to it.title.escapeHtml(),
                    "artist" to it.artist.escapeHtml(),
                    "name" to it.displayName.escapeHtml(),
                    "size" to Files.formatLength(it.size),
                )
            }
        )
    }

    if (images.isNotEmpty()) {
        builder.append(
            renderTitle(templates, templates.context.getString(R.string.images))
        )
        builder.append(
            templates.render("web/template/list/image.html", images) {
                mapOf(
                    "id" to it.hashCode(),
                    "title" to it.title.escapeHtml(),
                    "name" to it.displayName.escapeHtml(),
                    "size" to Files.formatLength(it.size),
                )
            }
        )
    }

    if (videos.isNotEmpty()) {
        builder.append(
            renderTitle(templates, templates.context.getString(R.string.videos))
        )
        builder.append(
            templates.render("web/template/list/video.html", videos) {
                mapOf(
                    "id" to it.hashCode(),
                    "title" to it.title.escapeHtml(),
                    "name" to it.displayName.escapeHtml(),
                    "size" to Files.formatLength(it.size),
                )
            }
        )
    }

    if (sharedByThirdParty.isNotEmpty()) {
        builder.append(
            renderTitle(templates, templates.context.getString(R.string.shared_by_other_apps))
        )
        builder.append(
            templates.render("web/template/list/openable.html", sharedByThirdParty) {
                mapOf(
                    "id" to it.hashCode(),
                    "name" to it.name.escapeHtml(),
                    "size" to Files.formatLength(it.size),
                )
            }
        )
    }

    return builder.toString()
}

fun renderTitle(templates: Templates, title: String): String {
    return templates.render("web/template/include/title.html", mapOf("title" to title))
}

fun renderEmptyContent(templates: Templates): String {
    return templates.render(
        "web/template/include/empty_content.html", mapOf(
            "title" to templates.context.getString(R.string.empty_text),
            "description" to templates.context.getString(R.string.web_share_empty_notice)
        )
    )
}

fun renderUploadedContent(templates: Templates, content: String): String {
    return templates.render(
        "web/template/include/uploaded_content.html", mapOf(
            "uploaded_content" to templates.context.getString(R.string.send_using_web_share_success_notice, content.escapeHtml()),
        )
    )
}

// Hey, Mr. Rager!
