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
package com.genonbeta.TrebleShot.widget

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
import android.os.Parcelable
import android.os.Parcel
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import android.content.DialogInterface
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import android.content.Intent
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
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.Bundle
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
import android.os.PowerManager
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
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.ViewGroup
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.os.Looper
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import com.genonbeta.TrebleShot.dataobject.Editable
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.util.listing.Lister
import com.genonbeta.android.framework.util.listing.Merger
import java.lang.IllegalArgumentException
import java.util.*

/**
 * created by: Veli
 * date: 29.03.2018 08:00
 */
abstract class GroupEditableListAdapter<T : GroupEditable?, V : GroupViewHolder?>(
    fragment: IEditableListFragment<T, V>,
    private var mGroupBy: Int
) : EditableListAdapter<T, V>(fragment) {
    protected abstract fun onLoad(lister: GroupLister<T>)
    protected abstract fun onGenerateRepresentative(text: String, merger: Merger<T>?): T
    override fun onLoad(): List<T> {
        val loadedList: MutableList<T> = ArrayList()
        val groupLister = createLister(loadedList, getGroupBy())
        onLoad(groupLister)
        if (groupLister.getList().size > 0) {
            Collections.sort(
                groupLister.getList(),
                Comparator<ComparableMerger<T>> { o1: ComparableMerger<T>?, o2: ComparableMerger<T> -> o2.compareTo(o1) })
            for (thisMerger in groupLister.getList()) {
                Collections.sort(thisMerger.getBelongings(), this)
                val generated: T? = onGenerateRepresentative(getRepresentativeText(thisMerger), thisMerger)
                val firstEditable: T = thisMerger.getBelongings().get(0)
                if (generated != null) {
                    loadedList.add(generated)
                    generated.setSize(thisMerger.getBelongings().size.toLong())
                    generated.setDate(firstEditable.comparableDate)
                    generated.setId(generated.getRepresentativeText().hashCode().inv().toLong())
                }
                loadedList.addAll(thisMerger.getBelongings())
            }
        } else Collections.sort(loadedList, this)
        return loadedList
    }

    open fun createLister(loadedList: MutableList<T>, groupBy: Int): GroupLister<T> {
        return GroupLister(loadedList, groupBy)
    }

    protected fun createDefaultViews(parent: ViewGroup?, viewType: Int, noPadding: Boolean): GroupViewHolder {
        if (viewType == VIEW_TYPE_REPRESENTATIVE) return GroupViewHolder(
            inflater.inflate(
                if (noPadding) R.layout.layout_list_title_no_padding else R.layout.layout_list_title,
                parent,
                false
            ), R.id.layout_list_title_text
        ) else if (viewType == VIEW_TYPE_ACTION_BUTTON) return GroupViewHolder(
            inflater.inflate(
                R.layout.layout_list_action_button, parent,
                false
            ), R.id.text
        )
        throw IllegalArgumentException("$viewType is not defined in defaults")
    }

    open fun getGroupBy(): Int {
        return mGroupBy
    }

    fun setGroupBy(groupBy: Int) {
        mGroupBy = groupBy
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).getViewType()
    }

    open fun getRepresentativeText(merger: Merger<out T>): String {
        if (merger is DateMerger<*>) return getSectionNameDate((merger as DateMerger<*>).getTime()).toString() else if (merger is StringMerger<*>) return (merger as StringMerger<*>).getString()
        return merger.toString()
    }

    override fun getSectionName(position: Int, `object`: T): String {
        if (`object`.isGroupRepresentative()) return `object`.getRepresentativeText()
        return if (getGroupBy() == MODE_GROUP_BY_DATE) getSectionNameDate(`object`.comparableDate) else super.getSectionName(
            position,
            `object`
        )
    }

    interface GroupEditable : Editable {
        fun getViewType(): Int
        fun getRequestCode(): Int
        fun getRepresentativeText(): String?
        fun setRepresentativeText(text: CharSequence)
        fun isGroupRepresentative(): Boolean
        fun setDate(date: Long)
        override fun setId(id: Long)
        fun setSize(size: Long)
    }

    abstract class GroupShareable : Shareable, GroupEditable {
        private var mViewType: Int = EditableListAdapter.Companion.VIEW_TYPE_DEFAULT

        constructor() : super() {}
        constructor(viewType: Int, representativeText: String) {
            mViewType = viewType
            friendlyName = representativeText
        }

        override fun getRequestCode(): Int {
            return 0
        }

        override fun getViewType(): Int {
            return mViewType
        }

        override fun getRepresentativeText(): String? {
            return friendlyName
        }

        override fun setRepresentativeText(text: CharSequence) {
            friendlyName = text.toString()
        }

        override fun isGroupRepresentative(): Boolean {
            return mViewType == VIEW_TYPE_REPRESENTATIVE || mViewType == VIEW_TYPE_ACTION_BUTTON
        }

        override fun setDate(date: Long) {
            comparableDate = date
        }

        override fun setSize(size: Long) {
            comparableSize = size
        }

        override fun setSelectableSelected(selected: Boolean): Boolean {
            return !isGroupRepresentative() && super.setSelectableSelected(selected)
        }
    }

    class GroupViewHolder : ViewHolder {
        private var mRepresentativeTextView: TextView? = null
        private var mRequestCode = 0

        constructor(itemView: View?, textView: TextView?) : super(itemView) {
            mRepresentativeTextView = textView
        }

        constructor(itemView: View, resRepresentativeText: Int) : this(
            itemView,
            itemView.findViewById<TextView>(resRepresentativeText)
        ) {
        }

        constructor(itemView: View?) : super(itemView) {}

        fun getRepresentativeTextView(): TextView? {
            return mRepresentativeTextView
        }

        fun getRequestCode(): Int {
            return mRequestCode
        }

        fun setRequestCode(requestCode: Int): GroupViewHolder {
            mRequestCode = requestCode
            return this
        }

        fun isRepresentative(): Boolean {
            return mRepresentativeTextView != null
        }

        fun tryBinding(editable: GroupEditable): Boolean {
            if (getRepresentativeTextView() == null || editable.getRepresentativeText() == null) return false
            getRepresentativeTextView().setText(editable.getRepresentativeText())
            setRequestCode(editable.getRequestCode())
            return true
        }
    }

    class GroupLister<T : GroupEditable?>(private val mNoGroupingList: MutableList<T>, private val mMode: Int) :
        Lister<T, ComparableMerger<T>?>() {
        private var mCustomLister: CustomGroupLister<T>? = null

        constructor(noGroupingList: MutableList<T>, mode: Int, customList: CustomGroupLister<T>?) : this(
            noGroupingList,
            mode
        ) {
            mCustomLister = customList
        }

        fun offerObliged(adapter: EditableListAdapterBase<T>, `object`: T) {
            if (adapter.filterItem(`object`)) offer(`object`)
        }

        fun offer(`object`: T) {
            if (mCustomLister == null || !mCustomLister.onCustomGroupListing(this, mMode, `object`)) {
                if (mMode == MODE_GROUP_BY_DATE) offer(
                    `object`, DateMerger<T>(
                        `object`!!.comparableDate
                    )
                ) else mNoGroupingList.add(`object`)
            }
        }

        fun setCustomLister(customLister: CustomGroupLister<T>?): GroupLister<T> {
            mCustomLister = customLister
            return this
        }

        interface CustomGroupLister<T : GroupEditable?> {
            fun onCustomGroupListing(lister: GroupLister<T>, mode: Int, `object`: T): Boolean
        }
    }

    companion object {
        const val VIEW_TYPE_REPRESENTATIVE = 100
        const val VIEW_TYPE_ACTION_BUTTON = 110
        const val MODE_GROUP_BY_NOTHING = 100
        const val MODE_GROUP_BY_DATE = 110
    }
}