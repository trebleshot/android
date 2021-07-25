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
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.fragment.PreparationViewModel
import java.util.*

@AndroidEntryPoint
class SharingActivity : Activity() {
    private val preparationViewModel: PreparationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action: String? = intent?.action
        var list: List<Uri>? = null

        when (action) {
            Intent.ACTION_SEND -> if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                startActivity(
                    Intent(this@SharingActivity, TextEditorActivity::class.java)
                        .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                        .putExtra(
                            TextEditorActivity.EXTRA_TEXT,
                            intent.getStringExtra(Intent.EXTRA_TEXT)
                        )
                )
            } else {
                val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                if (uri != null) list = Collections.singletonList(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
        }

        if (list.isNullOrEmpty()) {
            finish()
            return
        }

        preparationViewModel.consume(applicationContext, list)

        setContentView(R.layout.activity_sharing)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)

        navController(R.id.nav_host_fragment).addOnDestinationChangedListener { _, destination, _ ->
            title = destination.label
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}