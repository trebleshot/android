package org.monora.uprotocol.client.android.binding

import android.view.View
import androidx.databinding.BindingAdapter

@BindingAdapter("onLongClick")
fun assignLongClickListener(view: View, clickListener: View.OnClickListener) {
    view.setOnLongClickListener {
        clickListener.onClick(it)
        true
    }
}