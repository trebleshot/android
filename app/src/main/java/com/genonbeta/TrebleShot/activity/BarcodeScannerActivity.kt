/*
 * Copyright (C) 2019 Veli Tasalı
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
package com.genonbeta.TrebleShot.activity

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.adapter.DeviceListAdapter.NetworkDescription
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.TrebleShot.dataobject.DeviceRoute
import com.genonbeta.TrebleShot.dataobject.TextStreamObject
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask.ResultListener
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.Connections
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import java.net.InetAddress
import java.net.UnknownHostException

class BarcodeScannerActivity : Activity(), ResultListener, SnackbarPlacementProvider {
    private val dismissListener = DialogInterface.OnDismissListener { dialog: DialogInterface? -> updateState() }

    private lateinit var barcodeView: DecoratedBarcodeView

    private lateinit var connections: Connections

    private lateinit var conductContainer: ViewGroup

    private lateinit var conductText: TextView

    private lateinit var conductImage: ImageView

    private lateinit var textModeIndicator: ImageView

    private lateinit var conductButton: Button

    private lateinit var taskInterruptButton: Button

    private lateinit var taskContainer: View

    private val intentFilter = IntentFilter()

    private var permissionRequestedCamera = false

    private var permissionRequestedLocation = false

    private var showAsText = false

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION == intent.action
                || ConnectivityManager.CONNECTIVITY_ACTION == intent.action
                || LocationManager.PROVIDERS_CHANGED_ACTION == intent.action
            )
                updateState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        setResult(RESULT_CANCELED)
        connections = Connections(this)
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        intentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        conductContainer = findViewById(R.id.layout_barcode_connect_conduct_container)
        textModeIndicator = findViewById(R.id.layout_barcode_connect_mode_text_indicator)
        conductButton = findViewById(R.id.layout_barcode_connect_conduct_button)
        barcodeView = findViewById(R.id.layout_barcode_connect_barcode_view)
        conductText = findViewById(R.id.layout_barcode_connect_conduct_text)
        conductImage = findViewById(R.id.layout_barcode_connect_conduct_image)
        taskContainer = findViewById(R.id.container_task)
        taskInterruptButton = findViewById(R.id.task_interrupter_button)
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                handleBarcode(result.result.text)
            }

            override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
        })

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
        updateState()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
        barcodeView.pauseAndWait()
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeView.pause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.isNotEmpty()) {
            for (i in permissions.indices) {
                if (Manifest.permission.CAMERA == permissions[i] &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED
                ) {
                    updateState()
                    permissionRequestedCamera = false
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.actions_barcode_scanner, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            android.R.id.home -> onBackPressed()
            R.id.show_help -> AlertDialog.Builder(this)
                .setMessage(R.string.help_scanQRCode)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            R.id.change_mode -> {
                showAsText = !showAsText
                textModeIndicator.visibility = if (showAsText) View.VISIBLE else View.GONE
                item.setIcon(if (showAsText) R.drawable.ic_qrcode_white_24dp else R.drawable.ic_short_text_white_24dp)
                createSnackbar(if (showAsText) R.string.mesg_qrScannerTextMode else R.string.mesg_qrScannerDefaultMode).show()
                updateState()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onAttachTasks(taskList: List<BaseAttachableAsyncTask>) {
        super.onAttachTasks(taskList)
        for (task in taskList)
            if (task is DeviceIntroductionTask)
                task.anchor = this

        updateState(hasTaskOf(DeviceIntroductionTask::class.java))
    }

    override fun createSnackbar(resId: Int, vararg objects: Any?): Snackbar {
        return Snackbar.make(taskInterruptButton, getString(resId, *objects), Snackbar.LENGTH_LONG)
    }

    protected fun handleBarcode(code: String) {
        try {
            if (showAsText)
                throw Exception("Showing as text.")

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
                        run(NetworkDescription(ssid, bssid, password), pin)
                    }
                    Keyword.QR_CODE_TYPE_WIFI -> {
                        val pin = values[1].toInt()
                        val ssid = values[2]
                        val bssid = values[3]
                        val ip = values[4]
                        run(InetAddress.getByName(ip), bssid, pin)
                    }
                    else -> throw Exception("Request is unknown")
                }
        } catch (e: UnknownHostException) {
            showDialog(
                AlertDialog.Builder(this)
                    .setMessage(R.string.mesg_unknownHostError)
                    .setNeutralButton(R.string.butn_close, null)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            showDialog(
                AlertDialog.Builder(this)
                    .setTitle(R.string.text_unrecognizedQrCode)
                    .setMessage(code)
                    .setNegativeButton(R.string.butn_close, null)
                    .setPositiveButton(R.string.butn_show) { dialog: DialogInterface?, which: Int ->
                        val textObject = TextStreamObject(AppUtils.uniqueNumber.toLong(), code)
                        database.publish(textObject)
                        database.broadcast()
                        Toast.makeText(this, R.string.mesg_textStreamSaved, Toast.LENGTH_SHORT).show()
                        startActivity(
                            Intent(this, TextEditorActivity::class.java)
                                .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                                .putExtra(TextEditorActivity.EXTRA_CLIPBOARD_ID, textObject.id)
                        )
                    }
                    .setNeutralButton(R.string.butn_copyToClipboard) { dialog: DialogInterface?, which: Int ->
                        val manager = applicationContext.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        manager.setPrimaryClip(ClipData.newPlainText("copiedText", code))
                        Toast.makeText(this, R.string.mesg_textCopiedToClipboard, Toast.LENGTH_SHORT).show()
                    })
        }
    }

    private fun run(description: NetworkDescription, pin: Int) {
        run(DeviceIntroductionTask(description, pin))
    }

    private fun run(address: InetAddress, bssid: String?, pin: Int) {
        val runnable = Runnable { run(DeviceIntroductionTask(address, pin)) }
        val wifiInfo = connections.wifiManager.connectionInfo
        if (wifiInfo?.bssid != null && wifiInfo.bssid == bssid) {
            runnable.run()
        } else {
            showDialog(
                AlertDialog.Builder(this)
                    .setMessage(R.string.mesg_errorNotSameNetwork)
                    .setNegativeButton(R.string.butn_cancel, null)
                    .setPositiveButton(R.string.butn_gotIt) { dialog: DialogInterface?, which: Int -> runnable.run() }
                    .setOnDismissListener(dismissListener)
            )
        }
    }

    private fun run(task: DeviceIntroductionTask) {
        runUiTask(task, this)
    }

    private fun showDialog(builder: AlertDialog.Builder) {
        barcodeView.pauseAndWait()
        builder.setOnDismissListener(dismissListener).show()
    }

    fun updateState(connecting: Boolean) {
        if (connecting) {
            // Keep showing barcode view
            barcodeView.pauseAndWait()
            setConductItemsShowing(false)
        } else {
            barcodeView.resume()
            updateState()
        }
        taskContainer.visibility = if (connecting) View.VISIBLE else View.GONE
        taskInterruptButton.setOnClickListener(if (connecting)
            View.OnClickListener { v: View? ->
                getTaskListOf(DeviceIntroductionTask::class.java).forEach {
                    it.interrupt(true)
                }
            } else null)
    }

    fun updateState() {
        val wifiEnabled = connections.wifiManager.isWifiEnabled
        val hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
        // With Android Oreo, to gather Wi-Fi information, minimal access to location is needed
        val hasLocationPermission = Build.VERSION.SDK_INT < 23 || connections.canAccessLocation()
        val state = hasCameraPermission && (showAsText || wifiEnabled && hasLocationPermission)
        if (hasTaskOf(DeviceIntroductionTask::class.java)) return
        if (!state) {
            barcodeView.pauseAndWait()
            if (!hasCameraPermission) {
                conductImage.setImageResource(R.drawable.ic_camera_white_144dp)
                conductText.setText(R.string.text_cameraPermissionRequired)
                conductButton.setText(R.string.butn_ask)
                conductButton.setOnClickListener { v: View? ->
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISSION_CAMERA
                    )
                }
                if (!permissionRequestedCamera) ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.CAMERA),
                    REQUEST_PERMISSION_CAMERA
                )
                permissionRequestedCamera = true
            } else if (!hasLocationPermission) {
                conductImage.setImageResource(R.drawable.ic_perm_device_information_white_144dp)
                conductText.setText(R.string.mesg_locationPermissionRequiredAny)
                conductButton.setText(R.string.butn_enable)
                conductButton.setOnClickListener { v: View? ->
                    connections.validateLocationPermission(
                        this,
                        REQUEST_PERMISSION_LOCATION
                    )
                }
                if (!permissionRequestedLocation) ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ), REQUEST_PERMISSION_CAMERA
                )
                permissionRequestedLocation = true
            } else {
                conductImage.setImageResource(R.drawable.ic_signal_wifi_off_white_144dp)
                conductText.setText(R.string.text_scanQRWifiRequired)
                conductButton.setText(R.string.butn_enable)
                conductButton.setOnClickListener { v: View? -> connections.turnOnWiFi(this, this) }
            }
        } else {
            barcodeView.resume()
            conductText.setText(R.string.help_scanQRCode)
        }
        setConductItemsShowing(!state)
        barcodeView.setVisibility(if (state) View.VISIBLE else View.GONE)
    }

    protected fun setConductItemsShowing(showing: Boolean) {
        conductContainer.visibility = if (showing) View.VISIBLE else View.GONE
    }

    override fun onDeviceReached(deviceRoute: DeviceRoute) {
        setResult(
            RESULT_OK, Intent()
                .putExtra(EXTRA_DEVICE, deviceRoute.device)
                .putExtra(EXTRA_DEVICE_ADDRESS, deviceRoute.address)
        )
        finish()
    }

    override fun onTaskStateChange(task: BaseAttachableAsyncTask, state: AsyncTask.State) {
        if (task is DeviceIntroductionTask) {
            when (state) {
                AsyncTask.State.Starting -> updateState(true)
                AsyncTask.State.Finished -> updateState(false)
            }
        }
        updateState(!task.isFinished)
    }

    override fun onTaskMessage(taskMessage: TaskMessage): Boolean {
        if (taskMessage.sizeOfActions() > 1) runOnUiThread {
            taskMessage.toDialogBuilder(this).show()
        } else if (taskMessage.sizeOfActions() <= 1) runOnUiThread {
            taskMessage.toSnackbar(taskInterruptButton).show()
        } else return false
        return true
    }

    companion object {
        const val EXTRA_DEVICE = "extraDevice"
        const val EXTRA_DEVICE_ADDRESS = "extraDeviceAddress"
        const val REQUEST_PERMISSION_CAMERA = 1
        const val REQUEST_PERMISSION_LOCATION = 2
    }
}