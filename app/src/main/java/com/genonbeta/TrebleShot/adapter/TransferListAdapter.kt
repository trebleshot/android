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
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.util.Files
import com.genonbeta.android.framework.util.listing.Merger
import java.text.NumberFormat
import java.util.*

/**
 * created by: Veli
 * date: 9.11.2017 23:39
 */
class TransferListAdapter(fragment: IEditableListFragment<TransferIndex?, GroupViewHolder?>?) :
    GroupEditableListAdapter<TransferIndex?, GroupViewHolder?>(
        fragment,
        GroupEditableListAdapterMODE_GROUP_BY_DATE
    ) {
    private val mRunningTasks: MutableList<Long> = ArrayList()
    private val mPercentFormat: NumberFormat

    @ColorInt
    private val mColorPending: Int
    private val mColorDone: Int
    private val mColorError: Int
    protected override fun onLoad(lister: GroupLister<TransferIndex>) {
        val activeList: List<Long> = ArrayList(mRunningTasks)
        for (index in AppUtils.getKuick(getContext()).castQuery<Device, TransferIndex>(
            SQLQuery.Select(KuickTABLE_TRANSFER), TransferIndex::class.java
        )) {
            loadTransferInfo(getContext(), index)
            index.isRunning = activeList.contains(index.transfer.id)
            lister.offerObliged(this, index)
        }
    }

    protected override fun onGenerateRepresentative(text: String, merger: Merger<TransferIndex>?): TransferIndex {
        return TransferIndex(text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val holder: GroupViewHolder = if (viewType == EditableListAdapterVIEW_TYPE_DEFAULT) GroupViewHolder(
            getInflater().inflate(
                R.layout.list_transfer, parent, false
            )
        ) else createDefaultViews(
            parent, viewType,
            true
        )
        if (!holder.isRepresentative()) {
            getFragment().registerLayoutViewClicks(holder)
            holder.itemView.findViewById<View>(R.id.layout_image)
                .setOnClickListener(View.OnClickListener { v: View? -> getFragment().setItemSelected(holder, true) })
        }
        return holder
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        try {
            val `object`: TransferIndex = getItem(position)
            if (!holder.tryBinding(`object`)) {
                val parentView: View = holder.itemView
                @ColorInt val appliedColor: Int
                val percentage = (`object`.percentage() * 100) as Int
                val membersText: String = `object`.getMemberAsTitle(getContext())
                val progressBar = parentView.findViewById<ProgressBar>(R.id.progressBar)
                val image = parentView.findViewById<ImageView>(R.id.image)
                val statusLayoutWeb = parentView.findViewById<View>(R.id.statusLayoutWeb)
                val text1: TextView = parentView.findViewById<TextView>(R.id.text)
                val text2: TextView = parentView.findViewById<TextView>(R.id.text2)
                val text3: TextView = parentView.findViewById<TextView>(R.id.text3)
                val text4: TextView = parentView.findViewById<TextView>(R.id.text4)
                parentView.isSelected = `object`.isSelectableSelected
                appliedColor =
                    if (`object`.hasIssues) mColorError else if (`object`.numberOfCompleted() == `object`.numberOfTotal()) mColorDone else mColorPending
                if (`object`.isRunning) {
                    image.setImageResource(R.drawable.ic_pause_white_24dp)
                } else {
                    if (`object`.hasOutgoing() == `object`.hasIncoming()) image.setImageResource(if (`object`.hasOutgoing()) R.drawable.ic_compare_arrows_white_24dp else R.drawable.ic_error_outline_white_24dp) else image.setImageResource(
                        if (`object`.hasOutgoing()) R.drawable.ic_arrow_up_white_24dp else R.drawable.ic_arrow_down_white_24dp
                    )
                }
                statusLayoutWeb.visibility =
                    if (`object`.hasOutgoing() && `object`.transfer.isServedOnWeb) View.VISIBLE else View.GONE
                text1.setText(Files.sizeExpression(`object`.bytesTotal(), false))
                text2.setText(
                    if (membersText.length > 0) membersText else getContext().getString(
                        if (`object`.transfer.isServedOnWeb) R.string.text_transferSharedOnBrowser else R.string.text_emptySymbol
                    )
                )
                text3.setText(mPercentFormat.format(`object`.percentage()))
                text4.setText(
                    getContext().getString(
                        R.string.text_transferStatusFiles,
                        `object`.numberOfCompleted(), `object`.numberOfTotal()
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
        synchronized(mRunningTasks) {
            mRunningTasks.clear()
            mRunningTasks.addAll(list!!)
        }
    }

    init {
        val context: Context = getContext()
        mPercentFormat = NumberFormat.getPercentInstance()
        mColorPending = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorControlNormal))
        mColorDone = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorAccent))
        mColorError = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorError))
    }
}