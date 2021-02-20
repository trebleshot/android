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
package com.genonbeta.TrebleShot.adapter

import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.genonbeta.TrebleShot.GlideApp
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.adapter.VideoListAdapter.VideoHolder
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.util.TimeUtils
import com.genonbeta.android.framework.util.Files

/**
 * created by: Veli
 * date: 18.11.2017 13:32
 */
class VideoListAdapter(
    fragment: IEditableListFragment<VideoHolder, GroupViewHolder>,
) : GalleryGroupEditableListAdapter<VideoHolder, GroupViewHolder>(fragment, MODE_GROUP_BY_DATE) {
    private val selectedInset: Float = fragment.requireContext().resources.getDimension(R.dimen.space_list_grid)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val holder: GroupViewHolder = if (viewType == VIEW_TYPE_DEFAULT) {
            GroupViewHolder(
                layoutInflater.inflate(
                    if (isGridLayoutRequested()) R.layout.list_video_grid else R.layout.list_video, parent, false
                )
            )
        } else createDefaultViews(parent, viewType, true)

        if (!holder.isRepresentative()) {
            fragment.registerLayoutViewClicks(holder)
            val visitView: View = holder.itemView.findViewById(R.id.visitView)
            visitView.setOnClickListener { v: View? -> fragment.performLayoutClickOpen(holder) }
            visitView.setOnLongClickListener { v: View? -> fragment.performLayoutLongClick(holder) }
            holder.itemView.findViewById<View>(
                if (isGridLayoutRequested()) R.id.selectorContainer else R.id.selector
            ).setOnClickListener { v: View? ->
                fragment.setItemSelected(holder, true)
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        try {
            val item: VideoHolder = this.getItem(position)
            val parentView: View = holder.itemView
            if (!holder.tryBinding(item)) {
                val container = parentView.findViewById<ViewGroup>(R.id.container)
                val image = parentView.findViewById<ImageView>(R.id.image)
                val text1: TextView = parentView.findViewById(R.id.text)
                val text2: TextView = parentView.findViewById(R.id.text2)
                val text3: TextView = parentView.findViewById(R.id.text3)
                text1.setText(item.friendlyName)
                text2.setText(item.duration)
                text3.setText(Files.formatLength(item.getComparableSize(), false))
                parentView.isSelected = item.isSelectableSelected()
                GlideApp.with(context)
                    .load(item.uri)
                    .override(300)
                    .centerCrop()
                    .into(image)
            }
        } catch (ignored: Exception) {
        }
    }

    protected override fun onLoad(lister: GroupLister<VideoHolder>) {
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null,
            null, null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return

            val idIndex = cursor.getColumnIndex(MediaStore.Video.Media._ID)
            val titleIndex = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
            val displayIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val albumIndex = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val lengthIndex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            val dateIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
            val sizeIndex = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
            val typeIndex = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
            do {
                val holder = VideoHolder(
                    cursor.getInt(idIndex).toLong(), cursor.getString(titleIndex),
                    cursor.getString(displayIndex), cursor.getString(albumIndex), cursor.getString(typeIndex),
                    cursor.getLong(lengthIndex), cursor.getLong(dateIndex) * 1000,
                    cursor.getLong(sizeIndex), Uri.parse(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString() + "/"
                                + cursor.getInt(idIndex)
                    )
                )
                lister.offerObliged(this, holder)
            } while (cursor.moveToNext())
        }
    }

    class VideoHolder : GalleryGroupShareable {
        var duration: String? = null

        constructor(representativeText: String) : super(VIEW_TYPE_REPRESENTATIVE, representativeText)

        constructor(
            id: Long, friendlyName: String, fileName: String, albumName: String, mimeType: String,
            duration: Long, date: Long, size: Long, uri: Uri,
        ) : super(id, friendlyName, fileName, albumName, mimeType, date, size, uri) {
            this.duration = TimeUtils.getDuration(duration)
        }
    }

    companion object {
        const val VIEW_TYPE_TITLE = 1
    }
}