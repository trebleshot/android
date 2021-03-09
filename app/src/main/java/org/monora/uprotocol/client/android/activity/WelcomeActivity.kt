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
import android.view.animation.AnimationUtils.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.edit
import androidx.core.graphics.drawable.DrawableCompat
import androidx.transition.TransitionManager
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.data.UserDataRepository
import org.monora.uprotocol.client.android.databinding.LayoutProfileEditorBinding
import org.monora.uprotocol.client.android.util.Permissions
import org.monora.uprotocol.client.android.util.Resources.attrToRes
import org.monora.uprotocol.client.android.util.Resources.resToColor
import org.monora.uprotocol.client.android.viewmodel.UserProfileViewModel
import org.monora.uprotocol.client.android.widget.ViewPagerAdapter
import javax.inject.Inject

@AndroidEntryPoint
class WelcomeActivity : Activity() {
    @Inject
    lateinit var userDataRepository: UserDataRepository

    private val userProfileViewModel: UserProfileViewModel by viewModels()

    private val splashView: ViewGroup by lazy {
        layoutInflater.inflate(R.layout.layout_splash, null, false) as ViewGroup
    }

    private val profileViewBinding: LayoutProfileEditorBinding by lazy {
        LayoutProfileEditorBinding.inflate(layoutInflater, null, false).also {
            it.viewModel = userProfileViewModel
            it.lifecycleOwner = this
        }
    }

    private val permissionsView by lazy {
        layoutInflater.inflate(R.layout.layout_permissions, null, false).also {
            it.findViewById<View>(R.id.requestButton).setOnClickListener {
                requestRequiredPermissions(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasIntroductionShown()) {
            finish()
        }

        setContentView(R.layout.activity_welcome)

        skipPermissionRequest = true
        welcomePageDisallowed = true

        val confettiImageView: ImageView = findViewById(R.id.confetti)
        val titleTextView: TextView = findViewById(R.id.textView)
        val nextButton: FloatingActionButton = findViewById(R.id.nextButton)
        val previousButton: AppCompatImageView = findViewById(R.id.previousButton)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val viewPager: ViewPager = findViewById(R.id.activity_welcome_view_pager)
        val pagerAdapter = ViewPagerAdapter()
        val appliedColor: Int = R.attr.colorSecondary.attrToRes(this).resToColor(this)
        val titleList = mutableListOf<Int>().apply {
            add(R.string.text_welcome)
            if (Build.VERSION.SDK_INT >= 23) add(R.string.text_introSetUpPermissions)
            add(R.string.text_introSetUpProfile)
            add(R.string.text_introMakeItYours)
            add(R.string.text_introAllSet)
        }.map { getString(it) }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val wrapDrawable: Drawable = DrawableCompat.wrap(progressBar.progressDrawable)
            DrawableCompat.setTint(wrapDrawable, appliedColor)
            progressBar.progressDrawable = DrawableCompat.unwrap(wrapDrawable)
        } else {
            progressBar.progressTintList = ColorStateList.valueOf(appliedColor)
        }

        pagerAdapter.addView(splashView)

        if (Build.VERSION.SDK_INT >= 23) {
            pagerAdapter.addView(permissionsView)
            checkPermissionsState()
        }

        pagerAdapter.addView(profileViewBinding.root)
        profileViewBinding.executePendingBindings()

        pagerAdapter.addView(layoutInflater.inflate(R.layout.layout_look_preferences, null, false))

        layoutInflater.inflate(R.layout.layout_nyancat, null, false).also {
            pagerAdapter.addView(it)
            GlideApp.with(it.context)
                .load(R.drawable.nyancat)
                .into(it.findViewById(R.id.nyancat))
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
                defaultPreferences.edit {
                    putBoolean("introduction_shown", true)
                }
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
                    titleTextView.alpha = positionOffset
                } else {
                    progressBar.alpha = 1.0f
                    previousButton.alpha = 1.0f
                    titleTextView.alpha = 1.0f
                }
            }

            override fun onPageSelected(position: Int) {
                val lastPage = position == pagerAdapter.count - 1
                TransitionManager.beginDelayedTransition(titleTextView.parent as ViewGroup)

                titleTextView.text = titleList[position]
                progressBar.visibility = if (lastPage) View.GONE else View.VISIBLE

                nextButton.setImageResource(
                    if (lastPage) {
                        R.drawable.ic_check_white_24dp
                    } else {
                        R.drawable.ic_navigate_next_white_24dp
                    }
                )

                if (lastPage) {
                    GlideApp.with(applicationContext)
                        .load(R.drawable.confetti)
                        .into(confettiImageView)
                } else {
                    confettiImageView.setImageDrawable(null)
                }
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
        checkPermissionsState()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checkPermissionsState()
    }

    private fun checkPermissionsState() {
        if (Build.VERSION.SDK_INT < 23) return

        val ok = Permissions.checkRunningConditions(this)
        permissionsView.findViewById<View>(R.id.okImage).visibility = if (ok) View.VISIBLE else View.GONE
        permissionsView.findViewById<View>(R.id.requestButton).visibility = if (ok) View.GONE else View.VISIBLE
    }

    private fun slideSplashView() {
        splashView.findViewById<View>(R.id.logo).animation = loadAnimation(
            this, R.anim.enter_from_bottom_centered
        )
        splashView.findViewById<View>(R.id.appBrand).animation = loadAnimation(this, R.anim.enter_from_bottom)
        splashView.findViewById<View>(R.id.welcomeText).animation = loadAnimation(
            this, android.R.anim.fade_in
        ).also { it.startOffset = 1000 }
    }

    companion object {
        private val TAG = WelcomeActivity::class.simpleName
    }
}