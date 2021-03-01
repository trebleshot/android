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
import android.text.format.DateUtils
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.TextEditorActivity
import org.monora.uprotocol.client.android.adapter.SharedTextAdapter
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.model.ContentModel
import org.monora.uprotocol.client.android.model.DateSectionContentModel
import org.monora.uprotocol.client.android.viewmodel.SharedTextDataViewModel

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */
@AndroidEntryPoint
class SharedTextFragment : Fragment(R.layout.layout_shared_text) {
    private val viewModel: SharedTextDataViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val adapter = SharedTextAdapter()

        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter

        //adapter = SharedTextListAdapter(appDatabase)
        //emptyListImageView.setImageResource(R.drawable.ic_forum_white_24dp)
        //emptyListTextView.text = getString(R.string.text_listEmptyTextStream)
        view.findViewById<View>(R.id.fab).setOnClickListener {
            startActivity(
                Intent(activity, TextEditorActivity::class.java).setAction(TextEditorActivity.ACTION_EDIT_TEXT)
            )
        }

        viewModel.sharedTexts.asLiveData().observe(viewLifecycleOwner) { result ->
            adapter.submitList(withDateSections(result))
        }
    }

    @Synchronized
    private fun withDateSections(list: List<SharedText>): List<ContentModel> {
        val newList = ArrayList<ContentModel>()
        var previous: DateSectionContentModel? = null

        list.forEach {
            val dateText = DateUtils.formatDateTime(context, it.created, DateUtils.FORMAT_SHOW_DATE)
            if (previous?.dateText != dateText) {
                newList.add(DateSectionContentModel(dateText, it.created).also { model -> previous = model })
            }
            newList.add(it)
        }

        return newList
    }
}