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
import org.monora.uprotocol.client.android.database.AppDatabase
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
            contentComponent.appDatabase().clientDao().update(clientImpl)
        }
    }

    fun onTrustChanged(buttonView: CompoundButton, isChecked: Boolean) {
        val contentComponent = EntryPoints.get(buttonView.findActivity(), ClientContentComponent::class.java)
        clientImpl.isClientTrusted = isChecked
        GlobalScope.launch(Dispatchers.IO) {
            contentComponent.appDatabase().clientDao().update(clientImpl)
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
    fun appDatabase(): AppDatabase
}