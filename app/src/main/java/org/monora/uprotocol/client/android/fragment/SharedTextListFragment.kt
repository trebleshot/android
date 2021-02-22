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
package org.monora.uprotocol.client.android.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.activity.TextEditorActivity
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.database.AppDatabase
import javax.inject.Inject

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */
@AndroidEntryPoint
class SharedTextListFragment : Fragment(R.layout.layout_shared_text) {
    @Inject
    lateinit var appDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //adapter = SharedTextListAdapter(appDatabase)
        //emptyListImageView.setImageResource(R.drawable.ic_forum_white_24dp)
        //emptyListTextView.text = getString(R.string.text_listEmptyTextStream)
        view.findViewById<View>(R.id.layout_text_stream_fab).setOnClickListener { v: View? ->
            startActivity(
                Intent(activity, TextEditorActivity::class.java).setAction(TextEditorActivity.ACTION_EDIT_TEXT)
            )
        }
    }
}