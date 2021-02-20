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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.fragment.FileExplorerFragment
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.util.Files
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

/**
 * Created by: veli
 * Date: 5/29/17 3:18 PM
 */
class FilePickerActivity : Activity() {
    private lateinit var fileExplorerFragment: FileExplorerFragment

    private lateinit var fab: FloatingActionButton

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filepicker)
        fileExplorerFragment = supportFragmentManager.findFragmentById(R.id.activity_filepicker_fragment_files)
                as FileExplorerFragment? ?: throw NullPointerException()
        fab = findViewById(R.id.content_fab)
    }

    override fun onStart() {
        super.onStart()
        if (intent != null) {
            var hasTitlesDefined = false
            if (intent != null) {
                supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                if (intent.hasExtra(EXTRA_ACTIVITY_TITLE).also { hasTitlesDefined = it })
                    supportActionBar?.setTitle(intent.getStringExtra(EXTRA_ACTIVITY_TITLE))
            }

            if (ACTION_CHOOSE_DIRECTORY == intent.action) {
                if (supportActionBar != null) {
                    if (!hasTitlesDefined) supportActionBar!!.setTitle(R.string.text_chooseFolder) else supportActionBar!!.setSubtitle(
                        R.string.text_chooseFolder
                    )
                }
                fileExplorerFragment.adapter.also {
                    it.showDirectories = true
                    it.showFiles = false
                }
                fileExplorerFragment.refreshList()
                fileExplorerFragment.listView.setPadding(0, 0, 0, 200)
                fileExplorerFragment.listView.clipToPadding = false
                fab.show()
                fab.setOnClickListener { v: View ->
                    val selectedPath = fileExplorerFragment.adapter.path
                    if (selectedPath != null && selectedPath.canWrite())
                        finishWithResult(selectedPath)
                    else
                        Snackbar.make(v, R.string.mesg_currentPathUnavailable, Snackbar.LENGTH_SHORT).show()
                }
            } else if (ACTION_CHOOSE_FILE == intent.action) {
                if (!hasTitlesDefined)
                    supportActionBar?.setTitle(R.string.text_chooseFile)
                else
                    supportActionBar?.setSubtitle(R.string.text_chooseFolder)
                fileExplorerFragment.setLayoutClickListener(object : LayoutClickListener<ViewHolder> {
                    override fun onLayoutClick(
                        listFragment: EditableListFragmentBase<*>,
                        holder: ViewHolder,
                        longClick: Boolean,
                    ): Boolean {
                        if (longClick)
                            return false
                        // FIXME: 2/20/21 Return selected item
                        /*
                        val fileHolder = fileExplorerFragment.adapter.getItem(holder)
                        val file = fileHolder.file

                        if (file?.isDirectory() == true) {
                            finishWithResult(file)
                            return true
                        }*/
                        return false
                    }
                })
            } else {
                finish()
            }

            if (!isFinishing && intent.hasExtra(EXTRA_START_PATH)) try {
                fileExplorerFragment.goPath(
                    Files.fromUri(this, Uri.parse(intent.getStringExtra(EXTRA_START_PATH)))
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) finish() else return super.onOptionsItemSelected(item)
        return true
    }

    override fun onBackPressed() {
        if (!fileExplorerFragment.onBackPressed())
            super.onBackPressed()
    }

    private fun finishWithResult(file: DocumentFile) {
        setResult(
            RESULT_OK, Intent(ACTION_CHOOSE_DIRECTORY)
                .putExtra(EXTRA_CHOSEN_PATH, file.getUri())
        )
        finish()
    }

    companion object {
        const val ACTION_CHOOSE_DIRECTORY = "com.genonbeta.intent.action.CHOOSE_DIRECTORY"
        const val ACTION_CHOOSE_FILE = "com.genonbeta.intent.action.CHOOSE_FILE"
        const val EXTRA_ACTIVITY_TITLE = "activityTitle"
        const val EXTRA_START_PATH = "startPath"

        // belongs to returned result intent
        const val EXTRA_CHOSEN_PATH = "chosenPath"
    }
}