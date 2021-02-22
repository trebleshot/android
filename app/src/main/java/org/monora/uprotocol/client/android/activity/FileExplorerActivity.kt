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
import androidx.appcompat.widget.Toolbar
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.fragment.FileExplorerFragment
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.util.Files
import java.io.FileNotFoundException

class FileExplorerActivity : Activity() {
    private lateinit var fileExplorerFragment: FileExplorerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_explorer)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        fileExplorerFragment = supportFragmentManager.findFragmentById(R.id.activity_file_explorer_fragment_files)
                as FileExplorerFragment? ?: throw NullPointerException()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        checkRequestedPath(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home)
            finish()
        else
            return super.onOptionsItemSelected(item)
        return true
    }

    override fun onBackPressed() {
        if (!fileExplorerFragment.onBackPressed())
            super.onBackPressed()
    }

    fun checkRequestedPath(intent: Intent?) {
        if (intent == null)
            return

        if (intent.hasExtra(EXTRA_FILE_PATH)) {
            val directoryUri = intent.getParcelableExtra<Uri>(EXTRA_FILE_PATH)
            if (directoryUri != null) {
                try {
                    openFolder(Files.fromUri(applicationContext, directoryUri))
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }
        } else
            openFolder(null)
    }

    private fun openFolder(requestedFolder: DocumentFile?) {
        if (requestedFolder != null) fileExplorerFragment.requestPath(requestedFolder)
    }

    companion object {
        const val EXTRA_FILE_PATH = "filePath"
    }
}