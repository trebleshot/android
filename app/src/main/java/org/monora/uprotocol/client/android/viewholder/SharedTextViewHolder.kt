package org.monora.uprotocol.client.android.viewholder

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import org.monora.uprotocol.client.android.activity.TextEditorActivity
import org.monora.uprotocol.client.android.database.model.SharedTextModel
import org.monora.uprotocol.client.android.databinding.ListSharedTextBinding
import org.monora.uprotocol.client.android.viewmodel.SharedTextViewModel

class SharedTextViewHolder(private val binding: ListSharedTextBinding) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.root.setOnClickListener { view ->
            binding.viewModel?.sharedTextModel?.let {
                view.context.startActivity(
                    Intent(view.context, TextEditorActivity::class.java)
                        .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                        .putExtra(TextEditorActivity.EXTRA_TEXT_MODEL, it)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    fun bind(sharedTextModel: SharedTextModel) = with(binding) {
        viewModel = SharedTextViewModel(sharedTextModel)
        executePendingBindings()
    }
}