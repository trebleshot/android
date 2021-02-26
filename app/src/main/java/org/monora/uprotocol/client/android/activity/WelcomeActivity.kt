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
package org.monora.uprotocol.client.android.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.*
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.transition.TransitionManager
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.util.AppUtils
import org.monora.uprotocol.client.android.widget.ViewPagerAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.util.Permissions
import org.monora.uprotocol.client.android.util.Resources.attrToRes
import org.monora.uprotocol.client.android.util.Resources.resToColor
import org.monora.uprotocol.core.persistence.PersistenceProvider
import javax.inject.Inject

@AndroidEntryPoint
class WelcomeActivity : Activity() {
    @Inject
    lateinit var persistenceProvider: PersistenceProvider

    private lateinit var splashView: ViewGroup

    private lateinit var profileView: ViewGroup

    private lateinit var permissionsView: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasIntroductionShown()) finish()
        setContentView(R.layout.activity_welcome)

        skipPermissionRequest = true
        welcomePageDisallowed = true

        val nextButton: FloatingActionButton = findViewById(R.id.activity_welcome_view_next)
        val previousButton: AppCompatImageView = findViewById(R.id.activity_welcome_view_previous)
        val progressBar = findViewById<ProgressBar>(R.id.activity_welcome_progress_bar)
        val viewPager: ViewPager = findViewById(R.id.activity_welcome_view_pager)
        val pagerAdapter = ViewPagerAdapter()
        val appliedColor: Int = R.attr.colorSecondary.attrToRes(this).resToColor(this)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val wrapDrawable: Drawable = DrawableCompat.wrap(progressBar.progressDrawable)
            DrawableCompat.setTint(wrapDrawable, appliedColor)
            progressBar.progressDrawable = DrawableCompat.unwrap(wrapDrawable)
        } else progressBar.progressTintList = ColorStateList.valueOf(appliedColor)

        layoutInflater.inflate(R.layout.layout_welcome_page_1, null, false).also {
            splashView = it as ViewGroup
            pagerAdapter.addView(it)
        }

        if (Build.VERSION.SDK_INT >= 23) layoutInflater.inflate(R.layout.layout_welcome_page_3, null, false).also {
            permissionsView = it as ViewGroup
            pagerAdapter.addView(it)
            it.findViewById<View>(R.id.layout_welcome_page_3_request_button).setOnClickListener {
                requestRequiredPermissions(false)
            }
            checkPermissionsState()
        }

        layoutInflater.inflate(R.layout.layout_welcome_page_2, null, false).also {
            profileView = it as ViewGroup
            pagerAdapter.addView(it)
            setUserProfile()
        }

        pagerAdapter.addView(layoutInflater.inflate(R.layout.layout_welcome_page_4, null, false))

        layoutInflater.inflate(R.layout.layout_welcome_page_5, null, false).also { view: View ->
            val alphaAnimation = AlphaAnimation(0.3f, 1.0f)
            alphaAnimation.duration = 2000
            alphaAnimation.repeatCount = Animation.INFINITE
            alphaAnimation.repeatMode = Animation.REVERSE
            view.findViewById<View>(R.id.layout_welcome_page_5_text).animation = alphaAnimation
            pagerAdapter.addView(view)
        }

        progressBar.max = (pagerAdapter.count - 1) * 100

        previousButton.setOnClickListener {
            if (viewPager.currentItem - 1 >= 0) viewPager.setCurrentItem(
                viewPager.currentItem - 1, true
            )
        }

        nextButton.setOnClickListener {
            if (viewPager.currentItem + 1 < pagerAdapter.count) viewPager.currentItem =
                viewPager.currentItem + 1 else {
                // end presentation
                defaultPreferences.edit()
                    .putBoolean("introduction_shown", true)
                    .apply()
                startActivity(Intent(this@WelcomeActivity, HomeActivity::class.java))
                finish()
            }
        }

        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                progressBar.progress = position * 100 + (positionOffset * 100).toInt()
                if (position == 0) {
                    progressBar.alpha = positionOffset
                    previousButton.alpha = positionOffset
                } else {
                    progressBar.alpha = 1.0f
                    previousButton.alpha = 1.0f
                }
            }

            override fun onPageSelected(position: Int) {
                nextButton.setImageResource(
                    if (position + 1 >= pagerAdapter.count) {
                        R.drawable.ic_check_white_24dp
                    } else {
                        R.drawable.ic_navigate_next_white_24dp
                    }
                )
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        viewPager.adapter = pagerAdapter
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

    private fun checkPermissionsState() {
        if (Build.VERSION.SDK_INT < 23) return
        val permissionsOk = Permissions.checkRunningConditions(this)
        permissionsView.findViewById<View>(R.id.layout_welcome_page_3_perm_ok_image).visibility =
            if (permissionsOk) View.VISIBLE else View.GONE
        permissionsView.findViewById<View>(R.id.layout_welcome_page_3_request_button).visibility =
            if (permissionsOk) View.GONE else View.VISIBLE
    }

    private fun setUserProfile() {
        val localDevice = persistenceProvider.client
        val imageView = profileView.findViewById<ImageView>(R.id.layout_profile_picture_image_default)
        val editImageView = profileView.findViewById<ImageView>(R.id.layout_profile_picture_image_preferred)
        val deviceNameText: TextView = profileView.findViewById(R.id.header_default_device_name_text)
        val versionText: TextView = profileView.findViewById(R.id.header_default_device_version_text)
        deviceNameText.text = localDevice.clientNickname
        versionText.text = localDevice.clientVersionName
        loadProfilePictureInto(localDevice.clientNickname, imageView)
        editImageView.setOnClickListener { startProfileEditor() }
        TransitionManager.beginDelayedTransition(profileView)
    }

    private fun slideSplashView() {
        splashView.findViewById<View>(R.id.layout_welcome_page_1_splash_image).animation =
            AnimationUtils.loadAnimation(this, R.anim.enter_from_bottom_centered)
        splashView.findViewById<View>(R.id.layout_welcome_page_1_details).animation =
            AnimationUtils.loadAnimation(this, R.anim.enter_from_bottom)
    }

    companion object {
        val TAG = WelcomeActivity::class.java.simpleName
    }
}