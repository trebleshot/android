/*
 * Copyright (C) 2021 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

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