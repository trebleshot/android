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
package com.genonbeta.TrebleShot.widget.recyclerview

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
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`

/**
 * created by: veli
 * date: 3/11/19 1:02 AM
 */
class SwipeSelectionListener<T : Editable?>(private val mListFragment: EditableListFragmentBase<T>) :
    OnItemTouchListener {
    private var mSelectionActivated = false
    private var mActivationWaiting = false
    private var mLastPosition = 0
    private var mStartPosition = 0
    private var mInitialX = 0
    private var mInitialY = 0
    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (MotionEvent.ACTION_DOWN == e.getAction()) {
            mActivationWaiting = mListFragment.performerEngine != null
            mInitialX = e.getX()
            mInitialY = e.getY()
        } else if (MotionEvent.ACTION_MOVE == e.getAction() && mActivationWaiting
            && (mInitialX != e.getX() as Int || mInitialY != e.getY() as Int)
        ) {
            mSelectionActivated = e.getEventTime() - e.getDownTime() > ViewConfiguration.getLongPressTimeout()
            mActivationWaiting = false
        }
        return mSelectionActivated
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        if (MotionEvent.ACTION_UP == e.getAction() || MotionEvent.ACTION_CANCEL == e.getAction()) {
            setInitials()
        } else if (MotionEvent.ACTION_MOVE == e.getAction() && mSelectionActivated) {
            var currentPos = RecyclerView.NO_POSITION
            val view = mListFragment.listView.findChildViewUnder(e.getX(), e.getY())
            if (view != null) {
                val holder = mListFragment.listView
                    .findContainingViewHolder(view) as RecyclerViewAdapter.ViewHolder?
                if (holder != null) {
                    currentPos = holder.adapterPosition
                    if (currentPos >= 0) {
                        if (mStartPosition < 0) {
                            mStartPosition = currentPos
                            mLastPosition = currentPos
                        }
                        if (currentPos != mLastPosition) {
                            synchronized(mListFragment.adapterImpl.list) {

                                // The idea is that we start with some arbitrary position to select, so, for instance,
                                // when the starting position is 8 and user goes to select 7, 6, 5, we declare these
                                // as selected, however, when the user goes with 6, 7, 8 after 5, we decide that those
                                // numbers are now unselected. This goes on until the user releases the touch event.
                                val startPos = Math.min(currentPos, mLastPosition)
                                val endPos = Math.max(currentPos, mLastPosition)
                                for (i in startPos until endPos + 1) {
                                    val selected =
                                        if (currentPos > mLastPosition) mStartPosition <= i else mStartPosition >= i
                                    mListFragment.engineConnection.setSelected(
                                        mListFragment.adapterImpl.getItem(i), selected
                                    )
                                }
                            }
                            mLastPosition = currentPos
                        }
                    }
                }
            }
            if (mStartPosition < 0 && currentPos < 0) mSelectionActivated = false
            run {
                var scrollY = 0
                var scrollX = 0
                {
                    val viewHeight = rv.height.toFloat()
                    val viewPinPoint = viewHeight / 3
                    val touchPointBelowStart = viewHeight - viewPinPoint
                    if (touchPointBelowStart < e.getY()) {
                        scrollY =
                            (30 * ((Math.min(e.getY(), viewHeight) - touchPointBelowStart) / viewPinPoint)).toInt()
                    } else if (viewPinPoint > e.getY()) {
                        scrollY = (-30 * ((viewPinPoint - Math.max(e.getY(), 0f)) / viewPinPoint)).toInt()
                    }
                }
                {
                    val viewWidth = rv.width.toFloat()
                    val viewPinPoint = viewWidth / 3
                    val touchPointBelowStart = viewWidth - viewPinPoint
                    if (viewWidth - viewPinPoint < e.getX()) {
                        scrollX = (30 * ((Math.min(e.getX(), viewWidth) - touchPointBelowStart) / viewPinPoint)).toInt()
                    } else if (viewPinPoint > e.getX()) {
                        scrollX = (-30 / ((viewPinPoint - Math.max(e.getX(), 0f)) / viewPinPoint)).toInt()
                    }
                }

                // Sadly a previous attempt to make this scroll continuous failed due to the limitations of the
                // smoothScrollBy method of RecyclerView. The problem is that inside the touch events, calling it has
                // no effect. And also, using scrollBy with touch listener has not benefits as it doesn't invoke
                // onScrollStateChanged method which, if it did, could be used to repeat the scrolling process
                // when the state is SETTLING. If it went according to the plan, as long as the mSelectionActivated
                // is true, we could keep scrolling it. Another good solution could be to use SmoothScroller class
                // with the layout manager, however, it was expensive use to because, firstly, it didn't scroll by
                // pixels, but by pointing out the position of a child and secondly, even though it could work, it
                // wasn't the best solution out there, because the next problem would be to guess where the user is
                // pointing his or her hand.
                rv.scrollBy(scrollX, scrollY)
            }
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (disallowIntercept) setInitials()
    }

    fun setInitials() {
        mActivationWaiting = false
        mSelectionActivated = mActivationWaiting
        mLastPosition = -1
        mStartPosition = mLastPosition
        mInitialY = 0
        mInitialX = mInitialY
    }

    companion object {
        val TAG = SwipeSelectionListener::class.java.simpleName
    }

    init {
        setInitials()
    }
}