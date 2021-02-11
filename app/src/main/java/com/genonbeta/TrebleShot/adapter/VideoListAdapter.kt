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

import android.database.Cursor
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.genonbeta.TrebleShot.GlideApp
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.util.TimeUtils
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.util.Files
import com.genonbeta.android.framework.util.listing.Merger

/**
 * created by: Veli
 * date: 18.11.2017 13:32
 */
class VideoListAdapter(fragment: IEditableListFragment<VideoHolder?, GroupViewHolder?>?) :
    GalleryGroupEditableListAdapter<VideoHolder?, GroupViewHolder?>(
        fragment,
        GroupEditableListAdapter.MODE_GROUP_BY_DATE
    ) {
    private val mResolver: ContentResolver
    private val mSelectedInset: Int
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val holder: GroupViewHolder = if (viewType == EditableListAdapter.VIEW_TYPE_DEFAULT) GroupViewHolder(
            getInflater().inflate(
                if (isGridLayoutRequested()) R.layout.list_video_grid else R.layout.list_video, parent, false
            )
        ) else createDefaultViews(parent, viewType, true)
        if (!holder.isRepresentative()) {
            getFragment().registerLayoutViewClicks(holder)
            val visitView: View = holder.itemView.findViewById<View>(R.id.visitView)
            visitView.setOnClickListener { v: View? -> getFragment().performLayoutClickOpen(holder) }
            visitView.setOnLongClickListener { v: View? -> getFragment().performLayoutLongClick(holder) }
            holder.itemView.findViewById<View>(if (isGridLayoutRequested()) R.id.selectorContainer else R.id.selector)
                .setOnClickListener(
                    View.OnClickListener { v: View? -> getFragment().setItemSelected(holder, true) })
        }
        return holder
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        try {
            val `object`: VideoHolder = this.getItem(position)
            val parentView: View = holder.itemView
            if (!holder.tryBinding(`object`)) {
                val container = parentView.findViewById<ViewGroup>(R.id.container)
                val image = parentView.findViewById<ImageView>(R.id.image)
                val text1: TextView = parentView.findViewById<TextView>(R.id.text)
                val text2: TextView = parentView.findViewById<TextView>(R.id.text2)
                val text3: TextView = parentView.findViewById<TextView>(R.id.text3)
                text1.setText(`object`.friendlyName)
                text2.setText(`object`.duration)
                text3.setText(Files.sizeExpression(`object`.comparableSize, false))
                parentView.isSelected = `object`.isSelectableSelected
                GlideApp.with(getContext())
                    .load(`object`.uri)
                    .override(300)
                    .centerCrop()
                    .into(image)
            }
        } catch (ignored: Exception) {
        }
    }

    protected override fun onLoad(lister: GroupLister<VideoHolder>) {
        val cursor: Cursor = mResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null,
            null, null
        )
        if (cursor != null) {
            if (cursor.moveToFirst()) {
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
            cursor.close()
        }
    }

    protected override fun onGenerateRepresentative(text: String, merger: Merger<VideoHolder>?): VideoHolder {
        return VideoHolder(text)
    }

    class VideoHolder : GalleryGroupShareable {
        var duration: String? = null

        constructor(representativeText: String?) : super(
            GroupEditableListAdapter.VIEW_TYPE_REPRESENTATIVE,
            representativeText
        ) {
        }

        constructor(
            id: Long, friendlyName: String?, fileName: String?, albumName: String?, mimeType: String?,
            duration: Long, date: Long, size: Long, uri: Uri?
        ) : super(id, friendlyName, fileName, albumName, mimeType, date, size, uri) {
            this.duration = TimeUtils.getDuration(duration)
        }
    }

    companion object {
        const val VIEW_TYPE_TITLE = 1
    }

    init {
        mResolver = getContext().getContentResolver()
        mSelectedInset = getContext().getResources().getDimension(R.dimen.space_list_grid)
    }
}