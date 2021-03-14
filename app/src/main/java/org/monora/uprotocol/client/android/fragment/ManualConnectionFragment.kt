package org.monora.uprotocol.client.android.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.transition.TransitionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.client.android.databinding.LayoutManualConnectionBinding
import org.monora.uprotocol.client.android.model.ClientRoute
import org.monora.uprotocol.client.android.viewmodel.content.ClientContentViewModel
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import org.monora.uprotocol.core.protocol.communication.client.UnauthorizedClientException
import java.net.InetAddress
import java.net.ProtocolException
import java.net.UnknownHostException
import javax.inject.Inject

class ManualConnectionFragment : Fragment(R.layout.layout_manual_connection) {
    private val viewModel: ManualConnectionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutManualConnectionBinding.bind(view)

        binding.confirmButton.setOnClickListener {
            val address = binding.editText.text?.trim()?.toString()

            if (address.isNullOrEmpty()) {
                binding.editText.error = getString(R.string.mesg_enterValidHostAddress)
            } else {
                viewModel.connect(address)
            }
        }

        viewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                is ManualConnectionState.Loading -> {

                }
                is ManualConnectionState.Error -> when (it.exception) {
                    is UnknownHostException -> binding.editText.error = getString(R.string.mesg_unknownHostError)
                    is UnauthorizedClientException -> binding.editText.error = getString(R.string.mesg_notAllowed)
                    else -> binding.editText.error = it.exception.message
                }
                is ManualConnectionState.Loaded -> {
                    Log.d("ManualConnection", "onViewCreated: Loaded: ${it.clientRoute}")
                    binding.clientContentViewModel = ClientContentViewModel(it.clientRoute.client)
                    binding.executePendingBindings()
                }
            }

            TransitionManager.beginDelayedTransition(binding.root as ViewGroup)
            binding.progressBar.visibility = if (it.loading) View.VISIBLE else View.GONE
            binding.confirmButton.isEnabled = !it.loading
        }
    }
}

@HiltViewModel
class ManualConnectionViewModel @Inject internal constructor(
    private var connectionFactory: ConnectionFactory,
    private var persistenceProvider: PersistenceProvider,
) : ViewModel() {
    private val _state = MutableLiveData<ManualConnectionState>()

    private var _job: Job? = null

    val state = liveData {
        emitSource(_state)
    }

    fun connect(address: String) = _job ?: viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(ManualConnectionState.Loading())

        try {
            val inetAddress = InetAddress.getByName(address)
            val bridge = CommunicationBridge.connect(
                connectionFactory, persistenceProvider, inetAddress
            )

            if (!bridge.requestAcquaintance()) {
                throw ProtocolException("Should not have returned this")
            }

            val client = bridge.remoteClient
            val clientAddress = bridge.remoteClientAddress

            if (client !is UClient || clientAddress !is UClientAddress) {
                throw UnsupportedOperationException("Hello dear")
            }

            _state.postValue(ManualConnectionState.Loaded(ClientRoute(client, clientAddress)))
        } catch (e: Exception) {
            e.printStackTrace()
            _state.postValue(ManualConnectionState.Error(e))
        } finally {
            _job = null
        }
    }.also { _job = it }
}

sealed class ManualConnectionState(val loading: Boolean) {
    class Loading : ManualConnectionState(true)

    class Error(val exception: Exception) : ManualConnectionState(false)

    class Loaded(val clientRoute: ClientRoute) : ManualConnectionState(false)
}