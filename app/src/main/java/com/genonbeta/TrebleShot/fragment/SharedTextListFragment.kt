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
package com.genonbeta.TrebleShot.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.TextEditorActivity
import com.genonbeta.TrebleShot.adapter.SharedTextListAdapter
import com.genonbeta.TrebleShot.app.ListingFragment
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.model.SharedTextModel
import javax.inject.Inject

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */
@AndroidEntryPoint
class SharedTextListFragment : ListingFragment<SharedTextModel, ViewHolder, SharedTextListAdapter>() {
    @Inject
    lateinit var appDatabase: AppDatabase

    override fun onAttach(context: Context) {
        super.onAttach(context)
        bottomSpaceShown = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutResId = R.layout.layout_text_stream
        filteringSupported = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = SharedTextListAdapter(this, appDatabase)
        emptyListImageView.setImageResource(R.drawable.ic_forum_white_24dp)
        emptyListTextView.text = getString(R.string.text_listEmptyTextStream)
        view.findViewById<View>(R.id.layout_text_stream_fab).setOnClickListener { v: View? ->
            startActivity(
                Intent(activity, TextEditorActivity::class.java).setAction(TextEditorActivity.ACTION_EDIT_TEXT)
            )
        }
    }

    override fun performDefaultLayoutClick(holder: ViewHolder, target: SharedTextModel): Boolean {
        startActivity(
            Intent(context, TextEditorActivity::class.java)
                .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                .putExtra(TextEditorActivity.EXTRA_TEXT_MODEL, target)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }
}