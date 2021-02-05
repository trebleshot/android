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
import com.genonbeta.TrebleShot.util.TextUtils
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.util.FileUtils
import com.genonbeta.android.framework.util.MathUtils
import java.lang.IllegalStateException
import java.text.Collator
import java.util.ArrayList

/**
 * created by: Veli
 * date: 12.01.2018 16:55
 */
abstract class EditableListAdapter<T : Editable?, V : RecyclerViewAdapter.ViewHolder?>(fragment: IEditableListFragment<T, V>) :
    RecyclerViewAdapter<T, V>(fragment.context), EditableListAdapterBase<T>, SectionTitleProvider {
    private var mFragment: IEditableListFragment<T, V>? = null
    private var mCollator: Collator? = null
    private val mItemList: MutableList<T> = ArrayList()
    private var mSortingCriteria = MODE_SORT_BY_NAME
    private var mSortingOrderAscending = MODE_SORT_ORDER_ASCENDING
    private var mGridLayoutRequested = false
    override fun onUpdate(passedItem: List<T>) {
        synchronized(mItemList) {
            mItemList.clear()
            mItemList.addAll(passedItem)
            syncSelectionList()
        }
    }

    override fun compare(compare: T, compareTo: T): Int {
        val sortingAscending = getSortingOrder(compare, compareTo) == MODE_SORT_ORDER_ASCENDING
        val obj1 = if (sortingAscending) compare else compareTo
        val obj2 = if (sortingAscending) compareTo else compare
        if (obj1!!.comparisonSupported() == obj2!!.comparisonSupported() && !obj1.comparisonSupported()) return 0 else if (!compare!!.comparisonSupported()) return 1 else if (!compareTo!!.comparisonSupported()) return -1
        return compareItems(getSortingCriteria(compare, compareTo), getSortingOrder(), obj1, obj2)
    }

    fun compareItems(sortingCriteria: Int, sortingOrder: Int, obj1: T, obj2: T): Int {
        when (sortingCriteria) {
            MODE_SORT_BY_DATE -> return MathUtils.compare(
                obj1!!.comparableDate, obj2!!.comparableDate
            )
            MODE_SORT_BY_SIZE -> return MathUtils.compare(
                obj1!!.comparableSize, obj2!!.comparableSize
            )
            MODE_SORT_BY_NAME -> return getDefaultCollator()!!.compare(obj1!!.comparableName, obj2!!.comparableName)
        }
        throw IllegalStateException("Asked for $sortingCriteria which isn't known.")
    }

    override fun filterItem(item: T): Boolean {
        val filteringKeywords = getFragment()!!.filteringDelegate
            .getFilteringKeyword(getFragment())
        return filteringKeywords == null || filteringKeywords.size <= 0 || item!!.applyFilter(filteringKeywords)
    }

    fun isGridLayoutRequested(): Boolean {
        return mGridLayoutRequested
    }

    override fun getCount(): Int {
        return list.size
    }

    fun getDefaultCollator(): Collator? {
        if (mCollator == null) {
            mCollator = Collator.getInstance()
            mCollator.setStrength(Collator.TERTIARY)
        }
        return mCollator
    }

    fun getFragment(): IEditableListFragment<T, V>? {
        return mFragment
    }

    override fun getItemCount(): Int {
        return count
    }

    override fun getItem(position: Int): T {
        synchronized(mItemList) { return mItemList[position] }
    }

    fun getItem(holder: V): T {
        val position = holder!!.adapterPosition
        check(position != RecyclerView.NO_POSITION)
        return getItem(position)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)!!.id
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_DEFAULT
    }

    override fun getList(): List<T> {
        return mItemList
    }

    override fun getSelectableList(): List<T> {
        return list
    }

    open fun getSectionName(position: Int, `object`: T): String {
        when (getSortingCriteria()) {
            MODE_SORT_BY_NAME -> return getSectionNameTrimmedText(
                `object`!!.comparableName
            )
            MODE_SORT_BY_DATE -> return getSectionNameDate(`object`!!.comparableDate)
            MODE_SORT_BY_SIZE -> return FileUtils.sizeExpression(
                `object`!!.comparableSize, false
            )
        }
        return position.toString()
    }

    fun getSectionNameDate(date: Long): String {
        return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE).toString()
    }

    fun getSectionNameTrimmedText(text: String?): String {
        return TextUtils.trimText(text, 1)!!.toUpperCase()
    }

    override fun getSectionTitle(position: Int): String {
        return getSectionName(position, getItem(position))
    }

    open fun getSortingCriteria(objectOne: T, objectTwo: T): Int {
        return getSortingCriteria()
    }

    fun getSortingCriteria(): Int {
        return mSortingCriteria
    }

    open fun getSortingOrder(objectOne: T, objectTwo: T): Int {
        return getSortingOrder()
    }

    fun getSortingOrder(): Int {
        return mSortingOrderAscending
    }

    fun notifyGridSizeUpdate(gridSize: Int, isScreenLarge: Boolean) {
        mGridLayoutRequested = !isScreenLarge && gridSize > 1 || gridSize > 2
    }

    fun setFragment(fragmentImpl: IEditableListFragment<T, V>?) {
        mFragment = fragmentImpl
    }

    fun setSortingCriteria(sortingCriteria: Int, sortingOrder: Int) {
        mSortingCriteria = sortingCriteria
        mSortingOrderAscending = sortingOrder
    }

    override fun syncAndNotify(adapterPosition: Int) {
        syncSelection(adapterPosition)
        notifyItemChanged(adapterPosition)
    }

    override fun syncAllAndNotify() {
        syncSelectionList()
        notifyDataSetChanged()
    }

    @Synchronized
    fun syncSelection(adapterPosition: Int) {
        val item = getItem(adapterPosition)
        item.setSelectableSelected(mFragment!!.engineConnection.isSelectedOnHost(item))
    }

    @Synchronized
    fun syncSelectionList() {
        val itemList: List<T> = ArrayList(list)
        for (item in itemList) item.setSelectableSelected(mFragment!!.engineConnection.isSelectedOnHost(item))
    }

    companion object {
        const val VIEW_TYPE_DEFAULT = 0
        const val MODE_SORT_BY_NAME = 100
        const val MODE_SORT_BY_DATE = 110
        const val MODE_SORT_BY_SIZE = 120
        const val MODE_SORT_ORDER_ASCENDING = 100
        const val MODE_SORT_ORDER_DESCENDING = 110
    }

    init {
        setHasStableIds(true)
        setFragment(fragment)
    }
}