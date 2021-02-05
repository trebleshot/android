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
package com.genonbeta.TrebleShot.activity

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
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.transition.TransitionManager
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`

class WelcomeActivity : Activity() {
    private var mSplashView: ViewGroup? = null
    private var mProfileView: ViewGroup? = null
    private var mPermissionsView: ViewGroup? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasIntroductionShown()) finish()
        setContentView(R.layout.activity_welcome)
        setSkipPermissionRequest(true)
        setWelcomePageDisallowed(true)
        val nextButton: FloatingActionButton = findViewById<FloatingActionButton>(R.id.activity_welcome_view_next)
        val previousButton: AppCompatImageView = findViewById<AppCompatImageView>(R.id.activity_welcome_view_previous)
        val progressBar = findViewById<ProgressBar>(R.id.activity_welcome_progress_bar)
        val viewPager: ViewPager = findViewById<ViewPager>(R.id.activity_welcome_view_pager)
        val pagerAdapter = DynamicViewPagerAdapter()
        run {
            @ColorInt val appliedColor: Int = ContextCompat.getColor(
                this, AppUtils.getReference(
                    this,
                    R.attr.colorSecondary
                )
            )
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                val wrapDrawable: Drawable = DrawableCompat.wrap(progressBar.progressDrawable)
                DrawableCompat.setTint(wrapDrawable, appliedColor)
                progressBar.progressDrawable = DrawableCompat.unwrap<Drawable>(wrapDrawable)
            } else progressBar.progressTintList = ColorStateList.valueOf(appliedColor)
        }
        run {
            mSplashView = layoutInflater.inflate(
                R.layout.layout_welcome_page_1, null,
                false
            ) as ViewGroup
            pagerAdapter.addView(mSplashView)
        }
        if (Build.VERSION.SDK_INT >= 23) {
            mPermissionsView = layoutInflater.inflate(
                R.layout.layout_welcome_page_3, null,
                false
            ) as ViewGroup
            pagerAdapter.addView(mPermissionsView)
            checkPermissionsState()
            mPermissionsView!!.findViewById<View>(R.id.layout_welcome_page_3_request_button)
                .setOnClickListener { v: View? -> requestRequiredPermissions(false) }
        }
        run {
            mProfileView = layoutInflater.inflate(
                R.layout.layout_welcome_page_2, null,
                false
            ) as ViewGroup
            pagerAdapter.addView(mProfileView)
            setUserProfile()
        }
        pagerAdapter.addView(layoutInflater.inflate(R.layout.layout_welcome_page_4, null, false))
        run {
            val view = layoutInflater.inflate(R.layout.layout_welcome_page_5, null, false)
            val alphaAnimation = AlphaAnimation(0.3f, 1.0f)
            alphaAnimation.setDuration(2000)
            alphaAnimation.setRepeatCount(Animation.INFINITE)
            alphaAnimation.setRepeatMode(Animation.REVERSE)
            view.findViewById<View>(R.id.layout_welcome_page_5_text).animation = alphaAnimation
            pagerAdapter.addView(view)
        }
        progressBar.max = (pagerAdapter.getCount() - 1) * 100
        previousButton.setOnClickListener(View.OnClickListener { v: View? ->
            if (viewPager.getCurrentItem() - 1 >= 0) viewPager.setCurrentItem(
                viewPager.getCurrentItem() - 1,
                true
            )
        })
        nextButton.setOnClickListener(View.OnClickListener { v: View? ->
            if (viewPager.getCurrentItem() + 1 < pagerAdapter.getCount()) viewPager.setCurrentItem(viewPager.getCurrentItem() + 1) else {
                // end presentation
                defaultPreferences.edit()
                    .putBoolean("introduction_shown", true)
                    .apply()
                startActivity(Intent(this@WelcomeActivity, HomeActivity::class.java))
                finish()
            }
        })
        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                progressBar.progress = position * 100 + (positionOffset * 100).toInt()
                if (position == 0) {
                    progressBar.alpha = positionOffset
                    previousButton.setAlpha(positionOffset)
                } else {
                    progressBar.alpha = 1.0f
                    previousButton.setAlpha(1.0f)
                }
            }

            override fun onPageSelected(position: Int) {
                val interpolator = OvershootInterpolator()
                nextButton.setImageResource(if (position + 1 >= pagerAdapter.getCount()) R.drawable.ic_check_white_24dp else R.drawable.ic_navigate_next_white_24dp)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        viewPager.setAdapter(pagerAdapter)
    }

    override fun onStart() {
        super.onStart()
        slideSplashView()
    }

    override fun onResume() {
        super.onResume()
        setUserProfile()
        checkPermissionsState()
    }

    override fun onUserProfileUpdated() {
        super.onUserProfileUpdated()
        setUserProfile()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checkPermissionsState()
    }

    protected fun checkPermissionsState() {
        if (Build.VERSION.SDK_INT < 23) return
        val permissionsOk = AppUtils.checkRunningConditions(this)
        mPermissionsView!!.findViewById<View>(R.id.layout_welcome_page_3_perm_ok_image).visibility =
            if (permissionsOk) View.VISIBLE else View.GONE
        mPermissionsView!!.findViewById<View>(R.id.layout_welcome_page_3_request_button).visibility =
            if (permissionsOk) View.GONE else View.VISIBLE
    }

    protected fun setUserProfile() {
        if (mProfileView != null) {
            val localDevice = AppUtils.getLocalDevice(applicationContext)
            val imageView = mProfileView!!.findViewById<ImageView>(R.id.layout_profile_picture_image_default)
            val editImageView = mProfileView!!.findViewById<ImageView>(R.id.layout_profile_picture_image_preferred)
            val deviceNameText: TextView = mProfileView.findViewById<TextView>(R.id.header_default_device_name_text)
            val versionText: TextView = mProfileView.findViewById<TextView>(R.id.header_default_device_version_text)
            deviceNameText.setText(localDevice!!.username)
            versionText.setText(localDevice.versionName)
            loadProfilePictureInto(localDevice.username, imageView)
            editImageView.setOnClickListener { v: View? -> startProfileEditor() }
            TransitionManager.beginDelayedTransition(mProfileView!!)
        }
    }

    protected fun slideSplashView() {
        mSplashView!!.findViewById<View>(R.id.layout_welcome_page_1_splash_image).animation =
            AnimationUtils.loadAnimation(this, R.anim.enter_from_bottom_centered)
        mSplashView!!.findViewById<View>(R.id.layout_welcome_page_1_details).animation =
            AnimationUtils.loadAnimation(this, R.anim.enter_from_bottom)
    }

    companion object {
        val TAG = WelcomeActivity::class.java.simpleName
    }
}