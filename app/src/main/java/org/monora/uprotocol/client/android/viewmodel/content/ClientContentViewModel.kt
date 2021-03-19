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

import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.ObservableBoolean
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.data.ClientRepository
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.databinding.LayoutClientDetailBinding
import org.monora.uprotocol.client.android.util.findActivity
import org.monora.uprotocol.core.protocol.Client
import org.monora.uprotocol.core.spec.v1.Config

class ClientContentViewModel(private val clientImpl: UClient) : BaseObservable() {
    val client: Client = clientImpl

    val nickname = client.clientNickname

    val clientType = client.clientType.name

    val manufacturer = client.clientManufacturer

    val product = client.clientProduct

    val supported = Config.VERSION_UPROTOCOL_MIN <= client.clientProtocolVersion
            || client.clientProtocolVersionMin <= Config.VERSION_UPROTOCOL

    val version = client.clientVersionName

    @Bindable
    val blocked = ObservableBoolean(client.isClientBlocked)

    @Bindable
    val trusted = ObservableBoolean(client.isClientTrusted)

    fun onBlockedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        val contentComponent = EntryPoints.get(buttonView.findActivity(), ClientContentComponent::class.java)
        clientImpl.isClientBlocked = isChecked
        GlobalScope.launch(Dispatchers.IO) {
            contentComponent.clientRepository().update(clientImpl)
        }
    }

    fun onTrustChanged(buttonView: CompoundButton, isChecked: Boolean) {
        val contentComponent = EntryPoints.get(buttonView.findActivity(), ClientContentComponent::class.java)
        clientImpl.isClientTrusted = isChecked
        GlobalScope.launch(Dispatchers.IO) {
            contentComponent.clientRepository().update(clientImpl)
        }
    }

    val openClientProfileListener = View.OnClickListener {
        val binding = LayoutClientDetailBinding.inflate(
            LayoutInflater.from(it.context), null, false
        )
        val bottomSheetDialog = BottomSheetDialog(it.context)

        binding.viewModel = this
        binding.executePendingBindings()

        bottomSheetDialog.setContentView(binding.root)
        bottomSheetDialog.show()
    }
}

@EntryPoint
@InstallIn(ActivityComponent::class)
interface ClientContentComponent {
    fun clientRepository(): ClientRepository
}