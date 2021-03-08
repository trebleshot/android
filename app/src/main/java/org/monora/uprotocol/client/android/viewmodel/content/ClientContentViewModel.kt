package org.monora.uprotocol.client.android.viewmodel.content

import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.databinding.LayoutClientDetailBinding
import org.monora.uprotocol.core.protocol.Client
import org.monora.uprotocol.core.spec.v1.Config

class ClientContentViewModel(client: UClient) {
    val client: Client = client

    val nickname = client.clientNickname

    val clientType = client.clientType.name

    val manufacturer = client.clientManufacturer

    val product = client.clientProduct

    val supported = Config.VERSION_UPROTOCOL_MIN <= client.clientProtocolVersion
            || client.clientProtocolVersionMin <= Config.VERSION_UPROTOCOL

    val version = client.clientVersionName

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