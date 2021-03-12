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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.constraintlayout.widget.Group
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.android.framework.io.StreamInfo
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.result.contract.PickClient
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.databinding.ListSharingItemBinding
import org.monora.uprotocol.client.android.itemcallback.UTransferItemCallback
import org.monora.uprotocol.client.android.viewmodel.content.TransferItemContentViewModel
import org.monora.uprotocol.core.transfer.TransferItem.Type.Outgoing
import java.util.*
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class SharingActivity : Activity() {
    private val sharingActivityViewModel: SharingActivityViewModel by viewModels()

    private val pickClient = registerForActivityResult(PickClient()) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)
        setTitle(R.string.butn_shareWithTrebleshot)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)

        val action: String? = intent?.action

        if (Intent.ACTION_SEND == action && intent.hasExtra(Intent.EXTRA_TEXT)) {
            startActivity(
                Intent(this@SharingActivity, TextEditorActivity::class.java)
                    .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                    .putExtra(
                        TextEditorActivity.EXTRA_TEXT,
                        intent.getStringExtra(Intent.EXTRA_TEXT)
                    )
            )
            finish()
            return
        }

        val contents: MutableList<Uri> = ArrayList()

        when (action) {
            Intent.ACTION_SEND -> {
                val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                if (uri != null) contents.add(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val pendingFileUris: List<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                if (pendingFileUris != null) contents.addAll(pendingFileUris)
            }
        }

        if (contents.size == 0) {
            Toast.makeText(this, R.string.mesg_nothingToShare, Toast.LENGTH_SHORT).show()
            finish()
        } else {
            val progressBar: ProgressBar = findViewById(R.id.progressBar)
            val textMain: TextView = findViewById(R.id.textMain)
            val button: Button = findViewById(R.id.cancelButton)
            val groupPreparing: Group = findViewById(R.id.groupPreparing)
            val listParent: View = findViewById(R.id.listParent)
            val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
            val fab: FloatingActionButton = findViewById(R.id.fab)
            val adapter = SharingContentAdapter()

            recyclerView.adapter = adapter

            fab.setOnClickListener { pickClient.launch(PickClient.ConnectionMode.Return) }
            button.setOnClickListener { finish() }

            sharingActivityViewModel.consume(applicationContext, contents).also { data ->
                data.observe(this) {
                    when (it) {
                        is SharingActivityResult.Progress -> {
                            textMain.text = it.title
                            progressBar.max = it.total
                            progressBar.setProgress(it.index, true)
                        }
                        is SharingActivityResult.Result -> {
                            groupPreparing.visibility = View.GONE
                            listParent.visibility = View.VISIBLE

                            adapter.submitList(it.list)
                        }
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        private val TAG = SharingActivity::class.simpleName

        const val EXTRA_DEVICE_ID = "extraDeviceId"
    }
}

class SharingContentAdapter : ListAdapter<UTransferItem, SharingViewHolder>(UTransferItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SharingViewHolder {
        return SharingViewHolder(
            ListSharingItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SharingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long = with(getItem(position)) { groupId + id }
}

class SharingViewHolder(private val binding: ListSharingItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(transferItem: UTransferItem) {
        binding.viewModel = TransferItemContentViewModel(transferItem)
        binding.executePendingBindings()
    }
}

@HiltViewModel
class SharingActivityViewModel @Inject internal constructor(
    transferRepository: TransferRepository,
) : ViewModel() {
    private var consumer: LiveData<SharingActivityResult>? = null

    fun consume(
        context: Context, contents: List<Uri>,
    ) = consumer ?: liveData(Dispatchers.IO) {
        val id = Random.nextLong()
        val list = mutableListOf<UTransferItem>()

        contents.forEachIndexed { index, it ->
            StreamInfo.from(context, it).runCatching {
                emit(SharingActivityResult.Progress(index, contents.size, name))
                list.add(
                    UTransferItem(
                        index.toLong(), id, name, mimeType, size, null, uri.toString(), Outgoing
                    )
                )
            }
        }

        emit(SharingActivityResult.Result(id, list))
    }.also { consumer = it }
}

sealed class SharingActivityResult {
    class Progress(val index: Int, val total: Int, val title: String) : SharingActivityResult()

    class Result(val id: Long, val list: List<UTransferItem>) : SharingActivityResult()
}