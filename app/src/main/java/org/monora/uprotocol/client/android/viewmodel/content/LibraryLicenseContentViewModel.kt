package org.monora.uprotocol.client.android.viewmodel.content

import android.content.Intent
import android.net.Uri
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.model.LibraryLicense

class LibraryLicenseContentViewModel(license: LibraryLicense) {
    val packageName = "${license.artifactId.group} / ${license.libraryName}"

    val license = license.license

    val menuClick = View.OnClickListener {
        val popupMenu = PopupMenu(it.context, it)
        popupMenu.menuInflater.inflate(R.menu.popup_third_party_library_item, popupMenu.menu)
        popupMenu.menu.findItem(R.id.popup_visitWebPage).isEnabled = license.url != null
        popupMenu.menu.findItem(R.id.popup_goToLicenceURL).isEnabled = license.licenseUrl != null
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.popup_goToLicenceURL -> it.context.startActivity(
                    Intent(Intent.ACTION_VIEW).setData(Uri.parse(license.licenseUrl))
                )
                R.id.popup_visitWebPage -> it.context.startActivity(
                    Intent(Intent.ACTION_VIEW).setData(Uri.parse(license.url))
                )
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        popupMenu.show()
    }
}