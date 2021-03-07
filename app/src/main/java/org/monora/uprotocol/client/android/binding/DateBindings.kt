package org.monora.uprotocol.client.android.binding

import android.text.format.DateUtils
import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("clock")
fun toClock(textView: TextView, time: Long) {
    textView.text = DateUtils.formatDateTime(textView.context, time, DateUtils.FORMAT_SHOW_TIME)
}
