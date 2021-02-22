package org.monora.uprotocol.client.android.viewmodel

import android.text.format.DateUtils
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.ViewModel
import org.monora.uprotocol.client.android.database.model.SharedTextModel

class SharedTextViewModel(sharedTextModel: SharedTextModel) : ViewModel() {
    val sharedText = sharedTextModel
}

@BindingAdapter("clock")
fun clock(textView: TextView, time: Long) {
    textView.text = DateUtils.formatDateTime(textView.context, time, DateUtils.FORMAT_SHOW_TIME)
}
