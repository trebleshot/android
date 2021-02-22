package org.monora.uprotocol.client.android.viewmodel

import androidx.lifecycle.ViewModel
import org.monora.uprotocol.client.android.database.model.SharedTextModel

class SharedTextViewModel(sharedTextModel: SharedTextModel) : ViewModel() {
    val sharedText = sharedTextModel
}