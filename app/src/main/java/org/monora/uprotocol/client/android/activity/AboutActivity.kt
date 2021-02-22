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
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.config.Keyword
import org.monora.uprotocol.client.android.util.AppUtils
import org.monora.uprotocol.client.android.util.Updates

class AboutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<View>(R.id.orgIcon).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_REPO_ORG)))
        }
        findViewById<View>(R.id.activity_about_see_source_layout).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_REPO_APP)))
        }
        findViewById<View>(R.id.activity_about_translate_layout).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_TRANSLATE)))
        }
        findViewById<View>(R.id.activity_about_changelog_layout).setOnClickListener {
            startActivity(Intent(this@AboutActivity, ChangelogActivity::class.java))
        }
        findViewById<View>(R.id.activity_about_telegram_layout).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_TELEGRAM_CHANNEL)))
        }
        findViewById<View>(R.id.activity_about_option_fourth_layout).setOnClickListener {
            if (Keyword.Flavor.googlePlay == AppUtils.buildFlavor) {
                try {
                    startActivity(
                        Intent(
                            this@AboutActivity, Class.forName(
                                "org.monora.uprotocol.client.android.activity.DonationActivity"
                            )
                        )
                    )
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                }
            } else
                Updates.checkForUpdates(
                    this@AboutActivity,
                    Updates.getDefaultUpdater(this@AboutActivity),
                    true,
                    null
                )
        }
        findViewById<View>(R.id.activity_about_third_party_libraries_layout).setOnClickListener {
            startActivity(Intent(this@AboutActivity, ThirdPartyLibrariesActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.actions_about, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
        } else if (id == R.id.actions_about_feedback) {
            AppUtils.startFeedbackActivity(this@AboutActivity)
        } else
            return super.onOptionsItemSelected(item)
        return true
    }

    override fun onResume() {
        super.onResume()

        // calling this in the onCreate sequence causes theming issues
        if (Keyword.Flavor.googlePlay != AppUtils.buildFlavor && Updates.hasNewVersion(applicationContext))
            highlightUpdater(findViewById(R.id.activity_about_option_fourth_text))
    }

    private fun highlightUpdater(textView: TextView) {
        textView.setTextColor(
            ContextCompat.getColor(
                applicationContext, AppUtils.getReference(this@AboutActivity, R.attr.colorAccent)
            )
        )
        textView.setText(R.string.text_newVersionAvailable)
    }
}