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
import com.genonbeta.android.framework.util.listing.Merger

/**
 * created by: Veli
 * date: 18.11.2017 13:32
 */
class ImageListAdapter(fragment: IEditableListFragment<ImageHolder?, GroupViewHolder?>?) :
    GalleryGroupEditableListAdapter<ImageHolder?, GroupViewHolder?>(
        fragment,
        GalleryGroupEditableListAdapter.Companion.MODE_GROUP_BY_ALBUM
    ) {
    private val mResolver: ContentResolver
    private val mSelectedInset: Int
    protected override fun onLoad(lister: GroupLister<ImageHolder>) {
        val cursor: Cursor = mResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null,
            null, null
        )
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val titleIndex = cursor.getColumnIndex(MediaStore.Images.Media.TITLE)
                val displayIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val albumIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val dateAddedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val typeIndex = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                do {
                    val holder = ImageHolder(
                        cursor.getLong(idIndex), cursor.getString(titleIndex),
                        cursor.getString(displayIndex), cursor.getString(albumIndex), cursor.getString(typeIndex),
                        cursor.getLong(dateAddedIndex) * 1000, cursor.getLong(sizeIndex),
                        Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString() + "/" + cursor.getInt(idIndex))
                    )
                    holder.dateTakenString = TimeUtils.formatDateTime(getContext(), holder.comparableDate).toString()
                    lister.offerObliged(this, holder)
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val holder: GroupViewHolder = if (viewType == EditableListAdapter.Companion.VIEW_TYPE_DEFAULT) GroupViewHolder(
            getInflater().inflate(
                if (isGridLayoutRequested()) R.layout.list_image_grid else R.layout.list_image, parent, false
            )
        ) else createDefaultViews(parent, viewType, true)
        if (!holder.isRepresentative()) {
            getFragment().registerLayoutViewClicks(holder)
            val visitView: View = holder.itemView.findViewById<View>(R.id.visitView)
            visitView.setOnClickListener { v: View? -> getFragment().performLayoutClickOpen(holder) }
            visitView.setOnLongClickListener { v: View? -> getFragment().performLayoutLongClick(holder) }
            holder.itemView.findViewById<View>(if (isGridLayoutRequested()) R.id.selectorContainer else R.id.selector)
                .setOnClickListener(View.OnClickListener { v: View? -> getFragment().setItemSelected(holder, true) })
        }
        return holder
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        try {
            val parentView: View = holder.itemView
            val `object`: ImageHolder = getItem(position)
            if (!holder.tryBinding(`object`)) {
                val container = parentView.findViewById<ViewGroup>(R.id.container)
                val image = parentView.findViewById<ImageView>(R.id.image)
                val text1: TextView = parentView.findViewById<TextView>(R.id.text)
                val text2: TextView = parentView.findViewById<TextView>(R.id.text2)
                text1.setText(`object`.friendlyName)
                text2.setText(`object`.dateTakenString)
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

    protected override fun onGenerateRepresentative(text: String, merger: Merger<ImageHolder>?): ImageHolder {
        return ImageHolder(text)
    }

    class ImageHolder : GalleryGroupShareable {
        var dateTakenString: String? = null

        constructor(representativeText: String?) : super(
            GroupEditableListAdapter.Companion.VIEW_TYPE_REPRESENTATIVE,
            representativeText
        ) {
        }

        constructor(
            id: Long, title: String?, fileName: String?, albumName: String?, mimeType: String?, date: Long,
            size: Long, uri: Uri?
        ) : super(id, title, fileName, albumName, mimeType, date, size, uri) {
        }
    }

    init {
        mResolver = getContext().getContentResolver()
        mSelectedInset = getContext().getResources().getDimension(R.dimen.space_list_grid)
    }
}