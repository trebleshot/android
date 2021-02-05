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
package com.genonbeta.TrebleShot.app

import android.app.Activity
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
import android.net.Uri
import android.os.*
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.util.Log
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import androidx.appcompat.widget.SearchView
import androidx.collection.ArrayMap
import com.genonbeta.TrebleShot.dataobject.Editable
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.util.FileUtils
import com.genonbeta.android.framework.widget.recyclerview.FastScroller
import java.util.ArrayList

/**
 * created by: Veli
 * date: 21.11.2017 10:12
 */
abstract class EditableListFragment<T : Editable?, V : RecyclerViewAdapter.ViewHolder?, E : EditableListAdapter<T, V>?> :
    DynamicRecyclerViewFragment<T, V, E>(), IEditableListFragment<T, V> {
    private val mEngineConnection: IEngineConnection<T> = EngineConnection(this, this)
    private val mPerformerEngine: IPerformerEngine = PerformerEngine()
    private var mPerformerMenu: PerformerMenu? = null
    private var mFilteringDelegate: FilteringDelegate<T>? = null
    private var mRefreshRequested = false
    private var mSortingSupported = true
    private var mFilteringSupported = false
    private var mItemOffsetDecorationEnabled = false
    private var mItemOffsetForEdges = true
    private var mTwoRowLayoutState = false
    private var mSelectByClick = false
    private var mHasBottomSpace = false
    private var mLocalSelectionActivated = false
    private var mDefaultItemOffsetPadding = -1f
    private var mDefaultOrderingCriteria: Int = EditableListAdapter.Companion.MODE_SORT_ORDER_ASCENDING
    private var mDefaultSortingCriteria: Int = EditableListAdapter.Companion.MODE_SORT_BY_NAME
    private var mDefaultViewingGridSize = 1
    private var mDefaultViewingGridSizeLandscape = 1
    private var mDividerResId = R.id.abstract_layout_fast_scroll_recyclerview_bottom_divider
    private var mLayoutResId = R.layout.abstract_layout_editable_list_fragment
    var fastScroller: FastScroller? = null
        private set
    private val mSortingOptions: MutableMap<String, Int> = ArrayMap()
    private val mOrderingOptions: MutableMap<String, Int> = ArrayMap()
    private val mSelectableList: List<T> = ArrayList()
    private var mObserver: ContentObserver? = null
    private var mLayoutClickListener: LayoutClickListener<V>? = null
    private var mSearchText: String? = null
    private val mDefaultFilteringDelegate: FilteringDelegate<T> = object : FilteringDelegate<T> {
        override fun changeFilteringKeyword(keyword: String?): Boolean {
            mSearchText = keyword
            return true
        }

        override fun getFilteringKeyword(listFragment: EditableListFragmentBase<T>?): Array<String?>? {
            return if (mSearchText != null && mSearchText!!.length > 0) mSearchText!!.split(" ".toRegex())
                .toTypedArray() else null
        }
    }

    open fun onCreatePerformerMenu(context: Context?): PerformerMenu? {
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mTwoRowLayoutState = isTwoRowLayout
        val arguments = arguments
        if (arguments != null) {
            mSelectByClick = arguments.getBoolean(ARG_SELECT_BY_CLICK, mSelectByClick)
            mHasBottomSpace = arguments.getBoolean(ARG_HAS_BOTTOM_SPACE, mHasBottomSpace)
        }
        if (mPerformerMenu != null) mPerformerMenu!!.setUp(mPerformerEngine)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(mLayoutResId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fastScroller = view.findViewById(R.id.abstract_layout_fast_scroll_recyclerview_fastscroll_view)
        fastScroller.setViewProvider(LongTextBubbleFastScrollViewProvider())
        setDividerVisible(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        if (mItemOffsetDecorationEnabled) {
            val padding =
                if (mDefaultItemOffsetPadding > -1) mDefaultItemOffsetPadding else resources.getDimension(R.dimen.padding_list_content_parent_layout)
            val offsetDecoration = ItemOffsetDecoration(
                padding.toInt(), mItemOffsetForEdges,
                isHorizontalOrientation
            )
            offsetDecoration.prepare(listView)
            listView.addItemDecoration(offsetDecoration)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
        if (mTwoRowLayoutState != isTwoRowLayout) toggleTwoRowLayout()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mPerformerMenu = onCreatePerformerMenu(context)
        mEngineConnection.setDefinitiveTitle(getDistinctiveTitle(context))
        mEngineConnection.addSelectionListener(this)
        if (performerEngine != null) performerEngine!!.ensureSlot(this, engineConnection)
    }

    override fun onDetach() {
        super.onDetach()
        if (performerEngine != null) performerEngine!!.removeSlot(engineConnection)
    }

    override fun onSelected(
        engine: IPerformerEngine, owner: IEngineConnection<T>, selectable: T, isSelected: Boolean,
        position: Int
    ) {
        if (position >= 0) adapter!!.syncAndNotify(position) else adapter!!.syncAllAndNotify()
        ensureLocalSelection()
    }

    override fun onSelected(
        engine: IPerformerEngine, owner: IEngineConnection<T>, selectableList: List<T>,
        isSelected: Boolean, positions: IntArray
    ) {
        adapter!!.syncAllAndNotify()
        ensureLocalSelection()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (isUsingLocalSelection && isLocalSelectionActivated) mPerformerMenu!!.populateMenu(menu) else {
            inflater.inflate(R.menu.actions_abs_editable_list, menu)
            val filterItem = menu.findItem(R.id.actions_abs_editable_filter)
            if (filterItem != null) {
                filterItem.isVisible = mFilteringSupported
                if (mFilteringSupported) {
                    val view = filterItem.actionView
                    if (view is SearchView) {
                        view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                            override fun onQueryTextSubmit(query: String): Boolean {
                                refreshList()
                                return true
                            }

                            override fun onQueryTextChange(newText: String): Boolean {
                                mSearchText = newText
                                refreshList()
                                return true
                            }
                        })
                    }
                }
            }
            val gridSizeItem = menu.findItem(R.id.actions_abs_editable_grid_size)
            if (gridSizeItem != null) {
                val gridSizeMenu: Menu = gridSizeItem.subMenu
                for (i in 1 until if (isScreenLandscape) 7 else 5) gridSizeMenu.add(
                    R.id.actions_abs_editable_group_grid_size, 0, i,
                    resources.getQuantityString(R.plurals.text_gridRow, i, i)
                )
                gridSizeMenu.setGroupCheckable(R.id.actions_abs_editable_group_grid_size, true, true)
            }
            val sortingOptions: MutableMap<String, Int> = ArrayMap()
            onSortingOptions(sortingOptions)
            if (sortingOptions.size > 0) {
                mSortingOptions.clear()
                mSortingOptions.putAll(sortingOptions)
                applyDynamicMenuItems(
                    menu.findItem(R.id.actions_abs_editable_sort_by),
                    R.id.actions_abs_editable_group_sorting, mSortingOptions
                )
                val orderingOptions: MutableMap<String, Int> = ArrayMap()
                onOrderingOptions(orderingOptions)
                if (orderingOptions.size > 0) {
                    mOrderingOptions.clear()
                    mOrderingOptions.putAll(orderingOptions)
                    applyDynamicMenuItems(
                        menu.findItem(R.id.actions_abs_editable_order_by),
                        R.id.actions_abs_editable_group_sort_order, mOrderingOptions
                    )
                }
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (!isLocalSelectionActivated) {
            menu.findItem(R.id.actions_abs_editable_sort_by).isEnabled = isSortingSupported
            menu.findItem(R.id.actions_abs_editable_multi_select).isVisible = mPerformerMenu != null
            menu.findItem(R.id.actions_abs_editable_grid_size).isVisible = isGridSupported
            val sortingItem = menu.findItem(R.id.actions_abs_editable_sort_by)
            if (sortingItem != null) {
                sortingItem.isVisible = mSortingSupported
                if (sortingItem.isVisible) {
                    checkPreferredDynamicItem(sortingItem, sortingCriteria, mSortingOptions)
                    val orderingItem = menu.findItem(R.id.actions_abs_editable_order_by)
                    orderingItem?.let { checkPreferredDynamicItem(it, orderingCriteria, mOrderingOptions) }
                }
            }
            val gridSizeItem = menu.findItem(R.id.actions_abs_editable_grid_size)
            if (gridSizeItem != null) {
                val gridRowMenu: Menu = gridSizeItem.subMenu
                val currentRow = viewingGridSize - 1
                if (currentRow < gridRowMenu.size()) gridRowMenu.getItem(currentRow).isChecked = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val groupId = item.groupId
        if (groupId == R.id.actions_abs_editable_group_sorting) changeSortingCriteria(item.order) else if (groupId == R.id.actions_abs_editable_group_sort_order) changeOrderingCriteria(
            item.order
        ) else if (groupId == R.id.actions_abs_editable_group_grid_size) changeGridViewSize(item.order) else if (id == R.id.actions_abs_editable_multi_select && mPerformerMenu != null && activity != null) isLocalSelectionActivated =
            !mLocalSelectionActivated else if (mPerformerMenu != null && mPerformerMenu!!.onMenuItemClick(item)) isLocalSelectionActivated =
            false else super.onOptionsItemSelected(item)
        return true
    }

    open fun onSortingOptions(options: MutableMap<String, Int>) {
        options[getString(R.string.text_sortByName)] = EditableListAdapter.Companion.MODE_SORT_BY_NAME
        options[getString(R.string.text_sortByDate)] = EditableListAdapter.Companion.MODE_SORT_BY_DATE
        options[getString(R.string.text_sortBySize)] = EditableListAdapter.Companion.MODE_SORT_BY_SIZE
    }

    fun onOrderingOptions(options: MutableMap<String, Int>) {
        options[getString(R.string.text_sortOrderAscending)] = EditableListAdapter.Companion.MODE_SORT_ORDER_ASCENDING
        options[getString(R.string.text_sortOrderDescending)] = EditableListAdapter.Companion.MODE_SORT_ORDER_DESCENDING
    }

    open fun onGridSpanSize(viewType: Int, currentSpanSize: Int): Int {
        return 1
    }

    protected fun applyDynamicMenuItems(mainItem: MenuItem?, transferId: Int, options: Map<String, Int>) {
        if (mainItem != null) {
            mainItem.isVisible = true
            val dynamicMenu: Menu = mainItem.subMenu
            for (currentKey in options.keys) {
                val modeId = options[currentKey]!!
                dynamicMenu.add(transferId, 0, modeId, currentKey)
            }
            dynamicMenu.setGroupCheckable(transferId, true, true)
        }
    }

    override fun applyViewingChanges(gridSize: Int) {
        applyViewingChanges(gridSize, false)
    }

    fun applyViewingChanges(gridSize: Int, override: Boolean) {
        if (!isGridSupported && !override) return
        adapter!!.notifyGridSizeUpdate(gridSize, isScreenLarge)
        listView.layoutManager = layoutManager
        listView.adapter = adapter
        refreshList()
    }

    fun canShowWideView(): Boolean {
        return !isGridSupported && isScreenLarge && !isHorizontalOrientation
    }

    override fun changeGridViewSize(gridSize: Int) {
        viewPreferences!!.edit()
            .putInt(getUniqueSettingKey("GridSize" + if (isScreenLandscape) "Landscape" else ""), gridSize)
            .apply()
        applyViewingChanges(gridSize)
    }

    override fun changeOrderingCriteria(id: Int) {
        viewPreferences!!.edit()
            .putInt(getUniqueSettingKey("SortOrder"), id)
            .apply()
        adapter!!.setSortingCriteria(sortingCriteria, id)
        refreshList()
    }

    override fun changeSortingCriteria(id: Int) {
        viewPreferences!!.edit()
            .putInt(getUniqueSettingKey("SortBy"), id)
            .apply()
        adapter!!.setSortingCriteria(id, orderingCriteria)
        refreshList()
    }

    fun checkPreferredDynamicItem(dynamicItem: MenuItem?, preferredItemId: Int, options: Map<String, Int>) {
        if (dynamicItem != null) {
            val gridSizeMenu: Menu = dynamicItem.subMenu
            for (title in options.keys) {
                if (options[title] == preferredItemId) {
                    var menuItem: MenuItem
                    var iterator = 0
                    while (gridSizeMenu.getItem(iterator).also { menuItem = it } != null) {
                        if (title == menuItem.title.toString()) {
                            menuItem.isChecked = true
                            return
                        }
                        iterator++
                    }

                    // normally we should not be here
                    return
                }
            }
        }
    }

    protected fun ensureLocalSelection() {
        val shouldBeEnabled = mEngineConnection.getSelectedItemList().size > 0
        if (mLocalSelectionActivated != shouldBeEnabled) {
            Log.d(TAG, "ensureLocalSelection: Altering local selection state to '$shouldBeEnabled'")
            isLocalSelectionActivated = shouldBeEnabled
        }
    }

    override fun getAdapterImpl(): EditableListAdapterBase<T>? {
        return adapter
    }

    val defaultContentObserver: ContentObserver
        get() {
            if (mObserver == null) mObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun deliverSelfNotifications(): Boolean {
                    return true
                }

                override fun onChange(selfChange: Boolean) {
                    refreshList()
                }
            }
            return mObserver!!
        }

    override fun getFilteringDelegate(): FilteringDelegate<T> {
        return if (mFilteringDelegate == null) mDefaultFilteringDelegate else mFilteringDelegate
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager {
        val layoutManager = generateGridLayoutManager()
        val optimumGridSize = optimumGridSize
        layoutManager.spanCount = optimumGridSize
        layoutManager.spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                // should be reserved so it can occupy all the available space of a row.
                val type = adapter!!.getItemViewType(position)
                return if (type == EditableListAdapter.Companion.VIEW_TYPE_DEFAULT) 1 else onGridSpanSize(
                    type,
                    optimumGridSize
                )
            }
        }
        return layoutManager
    }

    override fun getOrderingCriteria(): Int {
        return viewPreferences!!.getInt(getUniqueSettingKey("SortOrder"), mDefaultOrderingCriteria)
    }

    private val optimumGridSize: Int
        private get() {
            val preferredGridSize = viewingGridSize
            return if (preferredGridSize > 1) preferredGridSize else if (canShowWideView() && isTwoRowLayout) 2 else 1
        }

    override fun getEngineConnection(): IEngineConnection<T> {
        return mEngineConnection
    }

    override fun getPerformerEngine(): IPerformerEngine? {
        if (context != null && activity is PerformerEngineProvider) return (activity as PerformerEngineProvider).performerEngine
        return if (mPerformerMenu != null) mPerformerEngine else null
    }

    override fun getSelectableList(): List<T> {
        return mSelectableList
    }

    override fun getSortingCriteria(): Int {
        return viewPreferences!!.getInt(getUniqueSettingKey("SortBy"), mDefaultSortingCriteria)
    }

    override fun getUniqueSettingKey(setting: String): String {
        return javaClass.simpleName + "_" + setting
    }

    val viewingGridSize: Int
        get() {
            if (viewPreferences == null) return 1
            return if (isScreenLandscape) viewPreferences!!.getInt(
                getUniqueSettingKey("GridSizeLandscape"),
                mDefaultViewingGridSizeLandscape
            ) else viewPreferences!!.getInt(
                getUniqueSettingKey("GridSize"),
                mDefaultViewingGridSize
            )
        }
    val viewPreferences: SharedPreferences?
        get() = AppUtils.getViewingPreferences(context!!)

    fun invokeClickListener(holder: V, longClick: Boolean): Boolean {
        return mLayoutClickListener != null && mLayoutClickListener.onLayoutClick(this, holder, longClick)
    }

    override fun isGridSupported(): Boolean {
        return false
    }

    override fun isLocalSelectionActivated(): Boolean {
        return mLocalSelectionActivated
    }

    override fun isRefreshRequested(): Boolean {
        return mRefreshRequested
    }

    val isSelectByClick: Boolean
        get() = mSelectByClick || mLocalSelectionActivated

    override fun isSortingSupported(): Boolean {
        return mSortingSupported
    }

    val isTwoRowLayout: Boolean
        get() = AppUtils.getDefaultPreferences(context)!!.getBoolean("two_row_layout", true)

    override fun isUsingLocalSelection(): Boolean {
        return activity !is PerformerEngineProvider && mPerformerMenu != null
    }

    override fun loadIfRequested(): Boolean {
        val refreshed = isRefreshRequested
        isRefreshRequested = false
        if (refreshed) refreshList()
        return refreshed
    }

    override fun openUri(uri: Uri?): Boolean {
        return FileUtils.openUri(requireContext(), uri)
    }

    override fun performLayoutClickOpen(holder: V): Boolean {
        val pos = holder!!.adapterPosition
        return pos != RecyclerView.NO_POSITION && performLayoutClickOpen(holder, adapterImpl!!.getItem(pos))
    }

    override fun performLayoutClickOpen(holder: V, `object`: T): Boolean {
        return `object` is Shareable && openUri((`object` as Shareable).uri)
    }

    override fun performDefaultLayoutLongClick(holder: V, `object`: T): Boolean {
        return false
    }

    override fun performLayoutClick(holder: V): Boolean {
        val position = holder!!.adapterPosition
        return if (position == RecyclerView.NO_POSITION) false else setItemSelected(holder) || invokeClickListener(
            holder,
            false
        )
                || performDefaultLayoutClick(holder, adapterImpl!!.getItem(position))
    }

    override fun performLayoutLongClick(holder: V): Boolean {
        val position = holder!!.adapterPosition
        return if (position == RecyclerView.NO_POSITION) false else invokeClickListener(
            holder,
            true
        ) || performDefaultLayoutLongClick(
            holder,
            adapterImpl!!.getItem(position)
        ) || setItemSelected(holder, true)
    }

    override fun registerLayoutViewClicks(holder: V) {
        holder!!.itemView.setOnClickListener { v: View? -> performLayoutClick(holder) }
        holder.itemView.setOnLongClickListener { v: View? -> performLayoutLongClick(holder) }
    }

    fun setDefaultItemOffsetPadding(defaultPadding: Float) {
        mDefaultItemOffsetPadding = defaultPadding
    }

    fun setDefaultOrderingCriteria(criteria: Int) {
        mDefaultOrderingCriteria = criteria
    }

    fun setDefaultSortingCriteria(criteria: Int) {
        mDefaultSortingCriteria = criteria
    }

    fun setDefaultViewingGridSize(gridSize: Int, gridSizeLandscape: Int) {
        mDefaultViewingGridSize = gridSize
        mDefaultViewingGridSizeLandscape = gridSizeLandscape
    }

    fun setDividerVisible(visible: Boolean) {
        if (view != null) {
            val divider = view!!.findViewById<View>(mDividerResId)
            divider.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    fun setDividerView(resId: Int) {
        mDividerResId = resId
    }

    override fun setFilteringDelegate(delegate: FilteringDelegate<T>?) {
        mFilteringDelegate = delegate
    }

    fun setFilteringSupported(supported: Boolean) {
        mFilteringSupported = supported
    }

    fun setHasBottomSpace(has: Boolean) {
        mHasBottomSpace = has
    }

    fun setItemOffsetDecorationEnabled(enabled: Boolean) {
        mItemOffsetDecorationEnabled = enabled
    }

    fun setItemOffsetForEdgesEnabled(enabled: Boolean) {
        mItemOffsetForEdges = enabled
    }

    override fun setItemSelected(holder: V): Boolean {
        return setItemSelected(holder, isSelectByClick)
    }

    override fun setItemSelected(holder: V, force: Boolean): Boolean {
        if (engineConnection.getSelectedItemList().size <= 0 && !force) return false
        try {
            engineConnection.setSelected(holder)
            return true
        } catch (e: SelectableNotFoundException) {
            e.printStackTrace()
        } catch (ignored: CouldNotAlterException) {
        }
        return false
    }

    override fun setLayoutClickListener(clickListener: LayoutClickListener<V>?) {
        mLayoutClickListener = clickListener
    }

    override fun setListAdapter(adapter: E, hadAdapter: Boolean) {
        super.setListAdapter(adapter, hadAdapter)
        fastScroller!!.setRecyclerView(listView)
        mEngineConnection.setSelectableProvider(adapter)
        adapter!!.fragment = this
        adapter.notifyGridSizeUpdate(viewingGridSize, isScreenLarge)
        adapter.setSortingCriteria(sortingCriteria, orderingCriteria)
    }

    override fun setListView(listView: RecyclerView) {
        super.setListView(listView)
        setSnackbarContainer(listView)
        listView.addOnItemTouchListener(SwipeSelectionListener(this))
        if (mHasBottomSpace) {
            val bottomSpace = (resources.getDimension(R.dimen.fab_margin) * 12).toInt()
            listView.clipToPadding = false
            listView.setPadding(0, 0, 0, bottomSpace)
        }
    }

    protected fun setLocalSelectionActivated(activate: Boolean) {
        mLocalSelectionActivated = activate
        if (!mLocalSelectionActivated) {
            val selectedItems: List<T> = ArrayList(mEngineConnection.getSelectedItemList())
            if (selectedItems.size > 0) mEngineConnection.setSelected(
                selectedItems,
                IntArray(selectedItems.size),
                false
            )
        }
        if (activity != null) activity!!.invalidateOptionsMenu()
    }

    protected fun setLayoutResId(resId: Int) {
        mLayoutResId = resId
    }

    fun setRefreshRequested(requested: Boolean) {
        mRefreshRequested = requested
    }

    fun setSortingSupported(sortingSupported: Boolean) {
        mSortingSupported = sortingSupported
    }

    fun toggleTwoRowLayout() {
        mTwoRowLayoutState = isTwoRowLayout
        applyViewingChanges(optimumGridSize, true)
    }

    interface LayoutClickListener<V : RecyclerViewAdapter.ViewHolder?> {
        fun onLayoutClick(listFragment: EditableListFragmentBase<*>?, holder: V, longClick: Boolean): Boolean
    }

    interface FilteringDelegate<T : Editable?> {
        fun changeFilteringKeyword(keyword: String?): Boolean
        fun getFilteringKeyword(listFragment: EditableListFragmentBase<T>?): Array<String?>?
    }

    open class SelectionCallback(val activity: Activity?, private val mProvider: PerformerEngineProvider) :
        PerformerMenu.Callback, PerformerEngineProvider {
        private var mPreviewSelections: MenuItem? = null
        private var mForegroundConnection: IEngineConnection<*>? = null
        private var mCancellable = true
        override fun onPerformerMenuList(
            performerMenu: PerformerMenu,
            inflater: MenuInflater,
            targetMenu: Menu
        ): Boolean {
            inflater.inflate(R.menu.action_mode_abs_editable, targetMenu)
            if (!mCancellable) targetMenu.findItem(R.id.action_mode_abs_editable_cancel_selection).isVisible = false
            mPreviewSelections = targetMenu.findItem(R.id.action_mode_abs_editable_preview_selections)
            if (performerEngine != null) updateTitle(SelectionUtils.getTotalSize(performerEngine))
            return true
        }

        override fun onPerformerMenuSelected(performerMenu: PerformerMenu, item: MenuItem): Boolean {
            val id = item.itemId
            if (id == R.id.action_mode_abs_editable_cancel_selection) {
                setSelectionState(false, false)
            } else if (id == R.id.action_mode_abs_editable_select_all) {
                setSelectionState(true, true)
            } else if (id == R.id.action_mode_abs_editable_select_none) {
                setSelectionState(false, true)
            } else if (id == R.id.action_mode_abs_editable_preview_selections) SelectionEditorDialog(
                activity,
                mProvider
            ).show()

            // Returning false means that the target item did not execute anything other than small changes.
            // It can be said that descendants can interfere with this by making changes and returning true;
            return false
        }

        override fun onPerformerMenuItemSelection(
            performerMenu: PerformerMenu, engine: IPerformerEngine,
            owner: IBaseEngineConnection, selectable: Selectable,
            isSelected: Boolean, position: Int
        ): Boolean {
            return true
        }

        override fun onPerformerMenuItemSelection(
            performerMenu: PerformerMenu, engine: IPerformerEngine,
            owner: IBaseEngineConnection,
            selectableList: List<Selectable>, isSelected: Boolean,
            positions: IntArray
        ): Boolean {
            return true
        }

        override fun onPerformerMenuItemSelected(
            performerMenu: PerformerMenu, engine: IPerformerEngine,
            owner: IBaseEngineConnection, selectable: Selectable, isSelected: Boolean,
            position: Int
        ) {
            updateTitle(SelectionUtils.getTotalSize(engine))
        }

        override fun onPerformerMenuItemSelected(
            performerMenu: PerformerMenu, engine: IPerformerEngine,
            owner: IBaseEngineConnection, selectableList: List<Selectable>,
            isSelected: Boolean, positions: IntArray
        ) {
            updateTitle(SelectionUtils.getTotalSize(engine))
        }

        fun setSelectionState(newState: Boolean, tryForeground: Boolean) {
            val engine = mProvider.performerEngine
            if (mForegroundConnection != null && tryForeground) setSelectionState(
                mForegroundConnection!!,
                newState
            ) else if (engine != null) {
                for (baseEngineConnection in engine.connectionList) if (baseEngineConnection is IEngineConnection<*>) setSelectionState(
                    baseEngineConnection as IEngineConnection<out Selectable?>,
                    newState
                )
            }
        }

        private fun <T : Selectable?> setSelectionState(connection: IEngineConnection<T>, newState: Boolean) {
            val availableList = connection.getAvailableList()
            if (availableList.size > 0) {
                val positions = IntArray(availableList.size)
                for (i in positions.indices) positions[i] = i
                connection.setSelected(availableList, positions, newState)
            }
        }

        fun setCancellable(cancellable: Boolean) {
            mCancellable = cancellable
        }

        override fun getPerformerEngine(): IPerformerEngine? {
            return mProvider.performerEngine
        }

        private fun updateTitle(totalSelections: Int) {
            // For local selections, the menu may be invalidated or may be just created meaning the menu item may not be
            // available yet.
            if (mPreviewSelections != null) {
                mPreviewSelections!!.title = totalSelections.toString()
                mPreviewSelections!!.isEnabled = totalSelections > 0
            }
        }

        /**
         * If you want to use only a single connection with [.setSelectionState] calls, you should provide the
         * foreground connection that should be used.
         *
         * @param connection to be used with foreground operations like select all or none
         */
        fun setForegroundConnection(connection: IEngineConnection<*>?) {
            mForegroundConnection = connection
        }
    }

    companion object {
        const val ARG_SELECT_BY_CLICK = "argSelectByClick"
        const val ARG_HAS_BOTTOM_SPACE = "argHasBottomSpace"
    }
}