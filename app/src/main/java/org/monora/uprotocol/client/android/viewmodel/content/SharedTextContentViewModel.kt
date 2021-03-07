package org.monora.uprotocol.client.android.viewmodel.content

import android.view.View
import org.monora.uprotocol.client.android.database.model.SharedText

class SharedTextContentViewModel(sharedText: SharedText, val clickListener: View.OnClickListener) {
    val text = sharedText.text

    val dateCreated = sharedText.dateCreated()
}
