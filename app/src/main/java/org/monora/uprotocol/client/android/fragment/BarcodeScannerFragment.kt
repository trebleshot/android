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

package org.monora.uprotocol.client.android.fragment

import android.Manifest.permission.CAMERA
import android.content.*
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.monora.android.codescanner.CodeScanner
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.TextEditorActivity
import org.monora.uprotocol.client.android.config.Keyword
import org.monora.uprotocol.client.android.data.SharedTextRepository
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.databinding.LayoutBarcodeScannerBinding
import org.monora.uprotocol.client.android.model.ClientRoute
import org.monora.uprotocol.client.android.model.NetworkDescription
import org.monora.uprotocol.client.android.util.Connections
import java.net.UnknownHostException
import javax.inject.Inject

@AndroidEntryPoint
@RequiresApi(19)
class BarcodeScannerFragment : Fragment(R.layout.layout_barcode_scanner) {
    @Inject
    lateinit var sharedTextRepository: SharedTextRepository

    private val viewModel: BarcodeScannerViewModel by viewModels()

    private val requestPermissions = registerForActivityResult(RequestMultiplePermissions()) {
        emitState()
    }

    private val connections by lazy {
        Connections(requireContext())
    }

    private val intentFilter by lazy {
        IntentFilter().apply {
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        }
    }

    private val binding by lazy {
        LayoutBarcodeScannerBinding.bind(requireView())
    }

    private val scanner by lazy {
        CodeScanner(
            requireContext(), binding.barcodeView, {
                lifecycleScope.launch {
                    handleBarcode(it.text)
                }
            }
        )
    }

    private val state = MutableLiveData<Change>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION == intent.action
                || ConnectivityManager.CONNECTIVITY_ACTION == intent.action
                || LocationManager.PROVIDERS_CHANGED_ACTION == intent.action
            ) {
                emitState()
            }
        }
    }

    private val requestPermissionsClickListener = View.OnClickListener {
        state.value?.let {
            if (!it.camera) {
                requestPermissions.launch(arrayOf(CAMERA))
            } else if (!it.location) {
                connections.validateLocationPermission(snackbarPlacementProvider, requestPermissions)
            } else if (!it.wifi) {
                connections.turnOnWiFi(snackbarPlacementProvider)
            }
        }
    }

    private val snackbarPlacementProvider = SnackbarPlacementProvider { resId, objects ->
        return@SnackbarPlacementProvider view?.let {
            Snackbar.make(it, getString(resId, objects), Snackbar.LENGTH_LONG).addCallback(
                object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onShown(transientBottomBar: Snackbar?) {
                        super.onShown(transientBottomBar)
                        scanner.startPreview()
                    }

                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        resumeIfPossible()
                    }
                }
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel
        viewModel.requestPermissionsClickListener = requestPermissionsClickListener

        binding.executePendingBindings()

        state.observe(viewLifecycleOwner) {
            viewModel.needsAccess.set(!it.camera || !it.location || !it.wifi)

            with(viewModel) {
                if (!it.camera) {
                    stateImage.set(R.drawable.ic_camera_white_144dp)
                    stateText.set(getString(R.string.text_cameraPermissionRequired))
                    stateButtonText.set(getString(R.string.butn_ask))
                } else if (!it.location) {
                    stateImage.set(R.drawable.ic_perm_device_information_white_144dp)
                    stateText.set(getString(R.string.mesg_locationPermissionRequiredAny))
                    stateButtonText.set(getString(R.string.butn_enable))
                } else if (!it.wifi) {
                    stateImage.set(R.drawable.ic_signal_wifi_off_white_144dp)
                    stateText.set(getString(R.string.text_scanQRWifiRequired))
                    stateButtonText.set(getString(R.string.butn_enable))
                }
            }

            if (viewModel.needsAccess.get()) {
                scanner.releaseResources()
            } else {
                scanner.startPreview()
            }
        }

        viewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                is State.Scan -> {

                }
                is State.Error -> {
                    Toast.makeText(context, "Error ${it.e.message}", Toast.LENGTH_LONG).show()
                }
                is State.Result -> {

                }
                is State.Running -> {

                }
            }


            if (it.running) {
                scanner.startPreview()
            } else {
                resumeIfPossible()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(receiver, intentFilter)
        emitState()
        resumeIfPossible()
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(receiver)
        scanner.releaseResources()
    }

    fun emitState() {
        state.postValue(
            Change(
                connections.canAccessLocation(),
                connections.wifiManager.isWifiEnabled,
                checkSelfPermission(requireContext(), CAMERA) == PERMISSION_GRANTED
            )
        )
    }

    @Synchronized
    private fun handleBarcode(code: String) {
        try {
            val values: Array<String> = code.split(";".toRegex()).toTypedArray()
            val type = values[0]

            // empty-strings cause trouble and are harder to manage.
            for (i in values.indices)
                when (type) {
                    Keyword.QR_CODE_TYPE_HOTSPOT -> {
                        val pin = values[1].toInt()
                        val ssid = values[2]
                        val bssid = values[3]
                        val password = values[4]
                        viewModel.consume(NetworkDescription(ssid, bssid, password)) //, pin)
                    }
                    Keyword.QR_CODE_TYPE_WIFI -> {
                        val pin = values[1].toInt()
                        val ssid = values[2]
                        val bssid = values[3]
                        val ip = values[4]
                        //run(InetAddress.getByName(ip), bssid, pin)
                    }
                    else -> throw Exception("Request is unknown")
                }
        } catch (e: UnknownHostException) {
            snackbarPlacementProvider.createSnackbar(R.string.mesg_unknownHostError)?.show()
        } catch (e: Exception) {
            AlertDialog.Builder(requireActivity())
                .setTitle(R.string.text_unrecognizedQrCode)
                .setMessage(code)
                .setNegativeButton(R.string.butn_close, null)
                .setPositiveButton(R.string.butn_show) { _: DialogInterface?, _: Int ->
                    val sharedText = SharedText(0, code)

                    lifecycleScope.launch(Dispatchers.IO) {
                        sharedTextRepository.insert(sharedText)
                    }

                    snackbarPlacementProvider.createSnackbar(R.string.mesg_textStreamSaved)?.show()

                    startActivity(
                        Intent(context, TextEditorActivity::class.java)
                            .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                            .putExtra(TextEditorActivity.EXTRA_TEXT_MODEL, sharedText)
                    )
                }
                .setNeutralButton(android.R.string.copy) { _: DialogInterface?, _: Int ->
                    (context?.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager?)?.let {
                        it.setPrimaryClip(ClipData.newPlainText("copiedText", code))
                        snackbarPlacementProvider.createSnackbar(R.string.mesg_textCopiedToClipboard)?.show()
                    }
                }
                .setOnDismissListener {
                    resumeIfPossible()
                }
                .show()

            scanner.stopPreview()
        }
    }

    private fun resumeIfPossible() {
        state.value?.let {
            if (it.camera && it.location && it.wifi) {
                scanner.startPreview()
            }
        }
    }

    companion object {
        val TAG = BarcodeScannerFragment::class.simpleName
    }
}

@HiltViewModel
class BarcodeScannerViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel() {
    private val connections = Connections(context)

    private val _state = MutableLiveData<State>(State.Scan())

    private var _job: Job? = null
        set(value) {
            field = value
            running.set(value != null)
        }

    val clickListener = View.OnClickListener {
        _job?.cancel() ?: requestPermissionsClickListener?.onClick(it)
    }

    val needsAccess = ObservableBoolean(false)

    var requestPermissionsClickListener: View.OnClickListener? = null

    val running = ObservableBoolean(false)

    val state = liveData {
        emitSource(_state)
    }

    val stateButtonText = ObservableField<String>()

    val stateImage = ObservableInt()

    val stateText = ObservableField<String>()

    fun consume(networkDescription: NetworkDescription) = _job ?: viewModelScope.launch(Dispatchers.IO) {
        try {
            _state.postValue(State.Running())
            connections.establishHotspotConnection(networkDescription)

        } catch (e: Exception) {
            _state.postValue(State.Error(e))
        } finally {
            _job = null
        }
    }.also { _job = it }
}

sealed class State(val running: Boolean) {
    class Scan : State(false)

    class Running : State(true)

    class Error(val e: Exception) : State(false)

    class Result(val clientRoute: ClientRoute) : State(false)
}

data class Change(val location: Boolean, val wifi: Boolean, val camera: Boolean)