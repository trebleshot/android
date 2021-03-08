package org.monora.uprotocol.client.android.viewmodel

import android.view.View
import androidx.databinding.Observable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EmptyContentViewModel @Inject internal constructor(

) : ViewModel() {
    var hasContent = ObservableBoolean()

    var loading = ObservableBoolean()

    fun with(content: View, hasContent: Boolean) {
        this.hasContent.set(hasContent)
        content.visibility = if (hasContent) View.VISIBLE else View.GONE
    }
}