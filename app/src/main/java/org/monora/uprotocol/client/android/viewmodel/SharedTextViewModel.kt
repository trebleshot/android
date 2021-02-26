package org.monora.uprotocol.client.android.viewmodel

import android.text.format.DateUtils
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.ViewModel
import org.monora.uprotocol.client.android.database.model.SharedText

class SharedTextViewModel(val sharedText: SharedText) : ViewModel()

@BindingAdapter("clock")
fun toClock(textView: TextView, time: Long) {
    textView.text = DateUtils.formatDateTime(textView.context, time, DateUtils.FORMAT_SHOW_TIME)
}

/*
@BindingAdapter("visibleIfSameDate")
fun visibleIfSameDate(textView: TextView, sharedTextViewModel: SharedTextViewModel) {
    val date = DateUtils.formatDateTime(
        textView.context, sharedTextViewModel.sharedTextModel.created, DateUtils.FORMAT_SHOW_DATE
    )
    val datePrev = sharedTextViewModel.prevSharedTextModel?.let {
        DateUtils.formatDateTime(textView.context, it.created, DateUtils.FORMAT_SHOW_DATE)
    }

    textView.visibility = if (datePrev == null || date != datePrev) View.VISIBLE else View.GONE
    textView.text = date
}
*/