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

import android.content.*
import android.graphics.drawable.Drawable
import android.os.*
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.TransferIndex
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.Transfers.loadTransferInfo
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.*
import com.genonbeta.android.database.SQLQuery
import com.genonbeta.android.framework.util.Files
import com.genonbeta.android.framework.util.listing.Merger
import java.text.NumberFormat
import java.util.*

/**
 * created by: Veli
 * date: 9.11.2017 23:39
 */
class TransferListAdapter(
    fragment: IEditableListFragment<TransferIndex, GroupViewHolder>,
) : GroupEditableListAdapter<TransferIndex, GroupViewHolder>(fragment, MODE_GROUP_BY_DATE) {
    private val runningTasks: MutableList<Long> = ArrayList()

    private val percentFormat: NumberFormat

    @ColorInt
    private val colorPending: Int

    @ColorInt
    private val colorDone: Int

    @ColorInt
    private val colorError: Int

    override fun onLoad(lister: GroupLister<TransferIndex>) {
        val activeList: List<Long> = ArrayList(runningTasks)
        for (index in AppUtils.getKuick(context).castQuery(
            SQLQuery.Select(Kuick.TABLE_TRANSFER), TransferIndex::class.java
        )) {
            loadTransferInfo(context, index)
            index.isRunning = activeList.contains(index.transfer.id)
            lister.offerObliged(this, index)
        }
    }

    protected override fun onGenerateRepresentative(text: String, merger: Merger<TransferIndex>?): TransferIndex {
        return TransferIndex(text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val holder: GroupViewHolder = if (viewType == VIEW_TYPE_DEFAULT) GroupViewHolder(
            layoutInflater.inflate(R.layout.list_transfer, parent, false)
        ) else createDefaultViews(
            parent, viewType, true
        )
        if (!holder.isRepresentative()) {
            fragment.registerLayoutViewClicks(holder)
            holder.itemView.findViewById<View>(R.id.layout_image).setOnClickListener { v: View? ->
                fragment.setItemSelected(holder, true)
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        try {
            val item: TransferIndex = getItem(position)
            if (!holder.tryBinding(item)) {
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
                parentView.isSelected = item.isSelectableSelected
                @ColorInt val appliedColor: Int = when {
                    item.hasIssues -> colorError
                    item.numberOfCompleted() == item.numberOfTotal() -> colorDone
                    else -> colorPending
                }

                if (item.isRunning) {
                    image.setImageResource(R.drawable.ic_pause_white_24dp)
                } else {
                    if (item.hasOutgoing() == item.hasIncoming()) image.setImageResource(if (item.hasOutgoing()) R.drawable.ic_compare_arrows_white_24dp else R.drawable.ic_error_outline_white_24dp) else image.setImageResource(
                        if (item.hasOutgoing()) R.drawable.ic_arrow_up_white_24dp else R.drawable.ic_arrow_down_white_24dp
                    )
                }
                statusLayoutWeb.visibility =
                    if (item.hasOutgoing() && item.transfer.isServedOnWeb) View.VISIBLE else View.GONE
                text1.setText(Files.sizeExpression(item.bytesTotal(), false))
                text2.setText(
                    if (membersText.length > 0) membersText else getContext().getString(
                        if (item.transfer.isServedOnWeb) R.string.text_transferSharedOnBrowser else R.string.text_emptySymbol
                    )
                )
                text3.setText(percentFormat.format(item.percentage()))
                text4.setText(
                    getContext().getString(
                        R.string.text_transferStatusFiles,
                        item.numberOfCompleted(), item.numberOfTotal()
                    )
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
                    progressBar.progressDrawable = DrawableCompat.unwrap<Drawable>(wrapDrawable)
                } else progressBar.progressTintList = ColorStateList.valueOf(appliedColor)
            }
        } catch (ignored: Exception) {
        }
    }

    fun updateActiveList(list: List<Long>?) {
        synchronized(runningTasks) {
            runningTasks.clear()
            runningTasks.addAll(list!!)
        }
    }

    init {
        val context: Context = getContext()
        percentFormat = NumberFormat.getPercentInstance()
        colorPending = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorControlNormal))
        colorDone = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorAccent))
        colorError = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorError))
    }
}