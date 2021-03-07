package org.monora.uprotocol.client.android.viewholder

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import org.monora.uprotocol.client.android.activity.TextEditorActivity
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.databinding.ListSharedTextBinding
import org.monora.uprotocol.client.android.viewmodel.content.SharedTextContentViewModel

class SharedTextViewHolder(private val binding: ListSharedTextBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(sharedText: SharedText) {
        binding.viewModel = SharedTextContentViewModel(sharedText) { view ->
            view.context.startActivity(
                Intent(view.context, TextEditorActivity::class.java)
                    .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                    .putExtra(TextEditorActivity.EXTRA_TEXT_MODEL, sharedText)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        binding.executePendingBindings()
    }
}