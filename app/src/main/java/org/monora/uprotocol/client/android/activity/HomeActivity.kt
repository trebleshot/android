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
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.BuildConfig
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.databinding.LayoutUserProfileBinding
import org.monora.uprotocol.client.android.util.Graphics
import org.monora.uprotocol.client.android.viewmodel.UserProfileViewModel

@AndroidEntryPoint
class HomeActivity : Activity(), NavigationView.OnNavigationItemSelectedListener {
    private val userProfileViewModel: UserProfileViewModel by viewModels()

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            Graphics.saveClientPictureLocal(applicationContext, uri)
        }
    }

    private val userProfileBinding by lazy {
        LayoutUserProfileBinding.bind(navigationView.getHeaderView(0)).also {
            it.viewModel = userProfileViewModel
            it.lifecycleOwner = this
            it.editProfileClickListener = View.OnClickListener {
                editProfile(userProfileViewModel, pickPhoto, this)
            }
        }
    }

    private lateinit var navigationView: NavigationView

    private lateinit var drawerLayout: DrawerLayout

    private var pendingMenuItemId = 0

    private val navController by lazy {
        navController(R.id.nav_host_fragment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        navigationView = findViewById(R.id.nav_view)
        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.text_navigationDrawerOpen, R.string.text_navigationDrawerClose
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        drawerLayout.addDrawerListener(
            object : DrawerLayout.SimpleDrawerListener() {
                override fun onDrawerClosed(drawerView: View) {
                    applyAwaitingDrawerAction()
                }
            }
        )

        toolbar.setupWithNavController(navController, AppBarConfiguration(navController.graph, drawerLayout))
        navigationView.setNavigationItemSelectedListener(this)
        navController.addOnDestinationChangedListener { _, destination, _ -> title = destination.label }
        userProfileBinding.executePendingBindings()

        if (BuildConfig.FLAVOR == "googlePlay") {
            navigationView.menu.findItem(R.id.menu_activity_main_donate).isVisible = true
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        pendingMenuItemId = item.itemId
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun applyAwaitingDrawerAction() {
        if (pendingMenuItemId == 0) {
            return // drawer was opened, but nothing was clicked.
        }

        when (pendingMenuItemId) {
            R.id.menu_activity_main_manage_devices -> {
                startActivity(Intent(this, ManageDevicesActivity::class.java))
            }
            R.id.menu_activity_main_about -> startActivity(Intent(this, AboutActivity::class.java))
            R.id.menu_activity_main_preferences -> {
                startActivity(Intent(this, PreferencesActivity::class.java))
            }
            R.id.menu_activity_main_donate -> try {
                startActivity(
                    Intent(
                        applicationContext,
                        Class.forName("org.monora.uprotocol.client.android.activity.DonationActivity")
                    )
                )
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
        }

        pendingMenuItemId = 0
    }

    companion object {
        const val REQUEST_PERMISSION_ALL = 1
    }
}
