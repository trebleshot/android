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
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.Companion.compileFrom
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withORs
import com.genonbeta.TrebleShot.dataobject.Identifier.Companion.from
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesPending
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.bytesValue
import com.genonbeta.TrebleShot.dataobject.TransferItem.flag
import com.genonbeta.TrebleShot.dataobject.TransferItem.putFlag
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withANDs
import com.genonbeta.TrebleShot.dataobject.TransferItem.Companion.from
import com.genonbeta.TrebleShot.dataobject.DeviceAddress.hostAddress
import com.genonbeta.TrebleShot.dataobject.Container.expand
import com.genonbeta.TrebleShot.dataobject.Device.equals
import com.genonbeta.TrebleShot.dataobject.TransferItem.flags
import com.genonbeta.TrebleShot.dataobject.TransferItem.getFlag
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.toString
import com.genonbeta.TrebleShot.dataobject.TransferItem.reconstruct
import com.genonbeta.TrebleShot.dataobject.Device.generatePictureId
import com.genonbeta.TrebleShot.dataobject.TransferItem.setDeleteOnRemoval
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.selectableTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasOutgoing
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasIncoming
import com.genonbeta.TrebleShot.dataobject.Comparable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableDate
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableName
import com.genonbeta.TrebleShot.dataobject.Editable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Editable.id
import com.genonbeta.TrebleShot.dataobject.Shareable.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.initialize
import com.genonbeta.TrebleShot.dataobject.Shareable.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Shareable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Shareable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Device.hashCode
import com.genonbeta.TrebleShot.dataobject.TransferIndex.percentage
import com.genonbeta.TrebleShot.dataobject.TransferIndex.getMemberAsTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfCompleted
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfTotal
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesTotal
import com.genonbeta.TrebleShot.dataobject.TransferItem.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.senderFlagList
import com.genonbeta.TrebleShot.dataobject.TransferItem.getPercentage
import com.genonbeta.TrebleShot.dataobject.TransferItem.setId
import com.genonbeta.TrebleShot.dataobject.TransferItem.comparableDate
import com.genonbeta.TrebleShot.dataobject.Identity.equals
import com.genonbeta.TrebleShot.dataobject.Transfer.equals
import com.genonbeta.TrebleShot.dataobject.TransferMember.reconstruct
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import androidx.annotation.DrawableRes
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.android.framework.ui.PerformerMenu
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.PickListener
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import androidx.annotation.StyleRes
import android.content.pm.PackageManager
import com.genonbeta.TrebleShot.activity.WelcomeActivity
import com.genonbeta.TrebleShot.GlideApp
import com.bumptech.glide.request.target.CustomTarget
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import com.genonbeta.TrebleShot.config.AppConfig
import kotlin.jvm.Synchronized
import com.genonbeta.TrebleShot.service.BackgroundService
import android.graphics.BitmapFactory
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog
import android.widget.ProgressBar
import android.view.LayoutInflater
import kotlin.jvm.JvmOverloads
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.EngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.TrebleShot.app.EditableListFragment.FilteringDelegate
import android.database.ContentObserver
import android.os.*
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.ViewGroup
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import android.widget.ImageView
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.util.FileUtils
import com.genonbeta.android.framework.util.listing.Merger
import java.lang.Exception
import java.text.NumberFormat
import java.util.ArrayList

/**
 * created by: Veli
 * date: 9.11.2017 23:39
 */
class TransferListAdapter(fragment: IEditableListFragment<TransferIndex?, GroupViewHolder?>?) :
    GroupEditableListAdapter<TransferIndex?, GroupViewHolder?>(
        fragment,
        GroupEditableListAdapter.Companion.MODE_GROUP_BY_DATE
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
            SQLQuery.Select(Kuick.Companion.TABLE_TRANSFER), TransferIndex::class.java
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
        val holder: GroupViewHolder = if (viewType == EditableListAdapter.Companion.VIEW_TYPE_DEFAULT) GroupViewHolder(
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
                text1.setText(FileUtils.sizeExpression(`object`.bytesTotal(), false))
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