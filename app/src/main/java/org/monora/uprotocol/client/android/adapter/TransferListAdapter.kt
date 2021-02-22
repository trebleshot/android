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
package org.monora.uprotocol.client.android.adapter

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.ImageViewCompat
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.IListingFragment
import org.monora.uprotocol.client.android.model.TransferIndex
import org.monora.uprotocol.client.android.util.AppUtils.getReference
import org.monora.uprotocol.client.android.widget.ListingAdapter
import com.genonbeta.android.framework.util.Files
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder
import java.text.NumberFormat
import java.util.*

/**
 * created by: Veli
 * date: 9.11.2017 23:39
 */
class TransferListAdapter(
    fragment: IListingFragment<TransferIndex, ViewHolder>,
) : ListingAdapter<TransferIndex, ViewHolder>(fragment) {
    private val runningTasks: MutableList<Long> = ArrayList()

    private val percentFormat: NumberFormat = NumberFormat.getPercentInstance()

    @ColorInt
    private val colorPending: Int = ContextCompat.getColor(context, getReference(context, R.attr.colorControlNormal))

    @ColorInt
    private val colorDone: Int = ContextCompat.getColor(context, getReference(context, R.attr.colorAccent))

    @ColorInt
    private val colorError: Int = ContextCompat.getColor(context, getReference(context, R.attr.colorError))

    override fun onLoad(): MutableList<TransferIndex> {
        TODO("Not yet implemented")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = ViewHolder(layoutInflater.inflate(R.layout.list_transfer, parent, false))
        fragment.registerLayoutViewClicks(holder)
        holder.itemView.findViewById<View>(R.id.layout_image).setOnClickListener {
            fragment.setItemSelected(holder, true)
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item: TransferIndex = getItem(position)
        val parentView: View = holder.itemView
        val percentage = (item.percentage() * 100).toInt()
        val membersText: String = item.getMemberAsTitle(context)
        val progressBar = parentView.findViewById<ProgressBar>(R.id.progressBar)
        val image = parentView.findViewById<ImageView>(R.id.image)
        val statusLayoutWeb = parentView.findViewById<View>(R.id.statusLayoutWeb)
        val text1: TextView = parentView.findViewById(R.id.text)
        val text2: TextView = parentView.findViewById(R.id.text2)
        val text3: TextView = parentView.findViewById(R.id.text3)
        val text4: TextView = parentView.findViewById(R.id.text4)
        parentView.isSelected = item.selected()
        @ColorInt val appliedColor: Int = when {
            item.hasIssues -> colorError
            item.numberOfCompleted() == item.numberOfTotal() -> colorDone
            else -> colorPending
        }

        if (item.isRunning) {
            image.setImageResource(R.drawable.ic_pause_white_24dp)
        } else {
            if (item.hasOutgoing() == item.hasIncoming()) {
                image.setImageResource(if (item.hasOutgoing()) R.drawable.ic_compare_arrows_white_24dp else R.drawable.ic_error_outline_white_24dp)
            } else image.setImageResource(
                if (item.hasOutgoing()) R.drawable.ic_arrow_up_white_24dp else R.drawable.ic_arrow_down_white_24dp
            )
        }
        statusLayoutWeb.visibility =
            if (item.hasOutgoing() && item.transfer.isServedOnWeb) View.VISIBLE else View.GONE
        text1.text = Files.formatLength(item.bytesTotal(), false)
        text2.text = if (membersText.isNotEmpty()) membersText else context.getString(
            if (item.transfer.isServedOnWeb) R.string.text_transferSharedOnBrowser else R.string.text_emptySymbol
        )
        text3.text = percentFormat.format(item.percentage())
        text4.text = context.getString(
            R.string.text_transferStatusFiles,
            item.numberOfCompleted(), item.numberOfTotal()
        )
        progressBar.max = 100
        if (Build.VERSION.SDK_INT >= 24) progressBar.setProgress(
            if (percentage <= 0) 1 else percentage,
            true
        ) else progressBar.progress = if (percentage <= 0) 1 else percentage
        ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(appliedColor))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val wrapDrawable: Drawable = DrawableCompat.wrap(progressBar.progressDrawable)
            DrawableCompat.setTint(wrapDrawable, appliedColor)
            progressBar.progressDrawable = DrawableCompat.unwrap(wrapDrawable)
        } else progressBar.progressTintList = ColorStateList.valueOf(appliedColor)
    }

    fun updateActiveList(list: List<Long>?) {
        synchronized(runningTasks) {
            runningTasks.clear()
            runningTasks.addAll(list!!)
        }
    }
}