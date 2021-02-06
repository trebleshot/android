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
import android.content.*
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.Companion.compileFrom
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withORs
import com.genonbeta.TrebleShot.dataobject.Identifier.Companion.from
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesPending
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.bytesValue
import com.genonbeta.TrebleShot.dataobject.TransferItem.flag
import com.genonbeta.TrebleShot.dataobject.TransferItem.putFlag
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withANDs
import com.genonbeta.TrebleShot.dataobject.TransferItem.Companion.from
import com.genonbeta.TrebleShot.dataobject.DeviceAddress.hostAddress
import com.genonbeta.TrebleShot.dataobject.Container.expand
import com.genonbeta.TrebleShot.dataobject.Device.equals
import com.genonbeta.TrebleShot.dataobject.TransferItem.flags
import com.genonbeta.TrebleShot.dataobject.TransferItem.getFlag
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.toString
import com.genonbeta.TrebleShot.dataobject.TransferItem.reconstruct
import com.genonbeta.TrebleShot.dataobject.Device.generatePictureId
import com.genonbeta.TrebleShot.dataobject.TransferItem.setDeleteOnRemoval
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.selectableTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasOutgoing
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasIncoming
import com.genonbeta.TrebleShot.dataobject.Comparable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableDate
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableName
import com.genonbeta.TrebleShot.dataobject.Editable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Editable.id
import com.genonbeta.TrebleShot.dataobject.Shareable.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.initialize
import com.genonbeta.TrebleShot.dataobject.Shareable.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Shareable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Shareable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Device.hashCode
import com.genonbeta.TrebleShot.dataobject.TransferIndex.percentage
import com.genonbeta.TrebleShot.dataobject.TransferIndex.getMemberAsTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfCompleted
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfTotal
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesTotal
import com.genonbeta.TrebleShot.dataobject.TransferItem.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.senderFlagList
import com.genonbeta.TrebleShot.dataobject.TransferItem.getPercentage
import com.genonbeta.TrebleShot.dataobject.TransferItem.setId
import com.genonbeta.TrebleShot.dataobject.TransferItem.comparableDate
import com.genonbeta.TrebleShot.dataobject.Identity.equals
import com.genonbeta.TrebleShot.dataobject.Transfer.equals
import com.genonbeta.TrebleShot.dataobject.TransferMember.reconstruct
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import androidx.annotation.DrawableRes
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.android.framework.ui.PerformerMenu
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.PickListener
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import androidx.annotation.StyleRes
import android.content.pm.PackageManager
import com.genonbeta.TrebleShot.activity.WelcomeActivity
import com.genonbeta.TrebleShot.GlideApp
import com.bumptech.glide.request.target.CustomTarget
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import com.genonbeta.TrebleShot.config.AppConfig
import kotlin.jvm.Synchronized
import com.genonbeta.TrebleShot.service.BackgroundService
import android.graphics.BitmapFactory
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog
import kotlin.jvm.JvmOverloads
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.EngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.TrebleShot.app.EditableListFragment.FilteringDelegate
import android.database.ContentObserver
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.*
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.genonbeta.TrebleShot.adapter.DeviceListAdapter
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.TrebleShot.dataobject.TextStreamObject
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask.ResultListener
import com.genonbeta.TrebleShot.util.Connections
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import java.lang.Exception
import java.net.InetAddress
import java.net.UnknownHostException

class BarcodeScannerActivity : Activity(), ResultListener, SnackbarPlacementProvider {
    private val mDismissListener = DialogInterface.OnDismissListener { dialog: DialogInterface? -> updateState() }
    private lateinit var mBarcodeView: DecoratedBarcodeView
    private lateinit var mConnections: Connections
    private lateinit var mConductContainer: ViewGroup
    private lateinit var mConductText: TextView
    private lateinit var mConductImage: ImageView
    private lateinit var mTextModeIndicator: ImageView
    private lateinit var mConductButton: Button
    private lateinit var mTaskInterruptButton: Button
    private lateinit var mTaskContainer: View
    private val mIntentFilter = IntentFilter()
    private var mPermissionRequestedCamera = false
    private var mPermissionRequestedLocation = false
    private var mShowAsText = false
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION == intent.action
                || ConnectivityManager.CONNECTIVITY_ACTION == intent.action
                || LocationManager.PROVIDERS_CHANGED_ACTION == intent.action)
                    updateState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        setResult(RESULT_CANCELED)
        mConnections = Connections(this)
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        mIntentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        mConductContainer = findViewById(R.id.layout_barcode_connect_conduct_container)
        mTextModeIndicator = findViewById(R.id.layout_barcode_connect_mode_text_indicator)
        mConductButton = findViewById(R.id.layout_barcode_connect_conduct_button)
        mBarcodeView = findViewById<DecoratedBarcodeView>(R.id.layout_barcode_connect_barcode_view)
        mConductText = findViewById<TextView>(R.id.layout_barcode_connect_conduct_text)
        mConductImage = findViewById(R.id.layout_barcode_connect_conduct_image)
        mTaskContainer = findViewById(R.id.container_task)
        mTaskInterruptButton = findViewById(R.id.task_interrupter_button)
        mBarcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                handleBarcode(result.result.text)
            }

            override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
        })
        if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mReceiver, mIntentFilter)
        updateState()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver)
        mBarcodeView.pauseAndWait()
    }

    override fun onDestroy() {
        super.onDestroy()
        mBarcodeView.pause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.size > 0) {
            for (i in permissions.indices) {
                if (Manifest.permission.CAMERA == permissions[i] &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED
                ) {
                    updateState()
                    mPermissionRequestedCamera = false
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
        if (id == android.R.id.home) onBackPressed() else if (id == R.id.show_help) AlertDialog.Builder(this)
            .setMessage(R.string.help_scanQRCode)
            .setPositiveButton(android.R.string.ok, null)
            .show() else if (id == R.id.change_mode) {
            mShowAsText = !mShowAsText
            mTextModeIndicator!!.visibility = if (mShowAsText) View.VISIBLE else View.GONE
            item.setIcon(if (mShowAsText) R.drawable.ic_qrcode_white_24dp else R.drawable.ic_short_text_white_24dp)
            createSnackbar(if (mShowAsText) R.string.mesg_qrScannerTextMode else R.string.mesg_qrScannerDefaultMode).show()
            updateState()
        } else return super.onOptionsItemSelected(item)
        return true
    }

    override fun onAttachTasks(taskList: List<BaseAttachableAsyncTask>) {
        super.onAttachTasks(taskList)
        for (task in taskList) if (task is DeviceIntroductionTask) (task as DeviceIntroductionTask).setAnchor(this)
        updateState(hasTaskOf(DeviceIntroductionTask::class.java))
    }

    override fun createSnackbar(resId: Int, vararg objects: Any): Snackbar {
        return Snackbar.make(mTaskInterruptButton, getString(resId, *objects), Snackbar.LENGTH_LONG)
    }

    protected fun handleBarcode(code: String) {
        try {
            if (mShowAsText) throw Exception("Showing as text.")
            val values: Array<String?> = code.split(";".toRegex()).toTypedArray()
            val type = values[0]

            // empty-strings cause trouble and are harder to manage.
            for (i in values.indices) if (values[i]!!.length == 0) values[i] = null
            when (type) {
                Keyword.QR_CODE_TYPE_HOTSPOT -> {
                    val pin = values[1]!!.toInt()
                    val ssid = values[2]
                    val bssid = values[3]
                    val password = values[4]
                    run(DeviceListAdapter.NetworkDescription(ssid, bssid, password), pin)
                }
                Keyword.QR_CODE_TYPE_WIFI -> {
                    val pin = values[1]!!.toInt()
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
                        val textObject = TextStreamObject(AppUtils.uniqueNumber, code)
                        database.publish<Any, TextStreamObject>(textObject)
                        database.broadcast()
                        Toast.makeText(this, R.string.mesg_textStreamSaved, Toast.LENGTH_SHORT).show()
                        startActivity(
                            Intent(this, TextEditorActivity::class.java)
                                .setAction(TextEditorActivity.Companion.ACTION_EDIT_TEXT)
                                .putExtra(TextEditorActivity.Companion.EXTRA_CLIPBOARD_ID, textObject.id)
                        )
                    }
                    .setNeutralButton(R.string.butn_copyToClipboard) { dialog: DialogInterface?, which: Int ->
                        val manager = applicationContext.getSystemService(
                            CLIPBOARD_SERVICE
                        ) as ClipboardManager
                        if (manager != null) {
                            manager.setPrimaryClip(ClipData.newPlainText("copiedText", code))
                            Toast.makeText(this, R.string.mesg_textCopiedToClipboard, Toast.LENGTH_SHORT).show()
                        }
                    })
        }
    }

    private fun run(description: NetworkDescription, pin: Int) {
        run(DeviceIntroductionTask(description, pin))
    }

    private fun run(address: InetAddress, bssid: String?, pin: Int) {
        val runnable = Runnable { run(DeviceIntroductionTask(address, pin)) }
        val wifiInfo: WifiInfo? = mConnections.getWifiManager().connectionInfo
        if (wifiInfo != null && wifiInfo.getBSSID() != null && wifiInfo.getBSSID() == bssid) {
            runnable.run()
        } else {
            showDialog(
                AlertDialog.Builder(this)
                    .setMessage(R.string.mesg_errorNotSameNetwork)
                    .setNegativeButton(R.string.butn_cancel, null)
                    .setPositiveButton(R.string.butn_gotIt) { dialog: DialogInterface?, which: Int -> runnable.run() }
                    .setOnDismissListener(mDismissListener))
        }
    }

    private override fun run(task: DeviceIntroductionTask) {
        runUiTask(task, this)
    }

    private fun showDialog(builder: AlertDialog.Builder) {
        mBarcodeView.pauseAndWait()
        builder.setOnDismissListener(mDismissListener).show()
    }

    fun updateState(connecting: Boolean) {
        if (connecting) {
            // Keep showing barcode view
            mBarcodeView.pauseAndWait()
            setConductItemsShowing(false)
        } else {
            mBarcodeView.resume()
            updateState()
        }
        mTaskContainer!!.visibility = if (connecting) View.VISIBLE else View.GONE
        mTaskInterruptButton!!.setOnClickListener(if (connecting) View.OnClickListener { v: View? ->
            val tasks: List<DeviceIntroductionTask?> = getTaskListOf<DeviceIntroductionTask>(
                DeviceIntroductionTask::class.java
            )
            for (task in tasks) task.interrupt(true)
        } else null)
    }

    fun updateState() {
        val wifiEnabled = mConnections.getWifiManager().isWifiEnabled
        val hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
        // With Android Oreo, to gather Wi-Fi information, minimal access to location is needed
        val hasLocationPermission = Build.VERSION.SDK_INT < 23 || mConnections!!.canAccessLocation()
        val state = hasCameraPermission && (mShowAsText || wifiEnabled && hasLocationPermission)
        if (hasTaskOf(DeviceIntroductionTask::class.java)) return
        if (!state) {
            mBarcodeView.pauseAndWait()
            if (!hasCameraPermission) {
                mConductImage!!.setImageResource(R.drawable.ic_camera_white_144dp)
                mConductText.setText(R.string.text_cameraPermissionRequired)
                mConductButton!!.setText(R.string.butn_ask)
                mConductButton!!.setOnClickListener { v: View? ->
                    ActivityCompat.requestPermissions(
                        this, arrayOf(
                            Manifest.permission.CAMERA
                        ), REQUEST_PERMISSION_CAMERA
                    )
                }
                if (!mPermissionRequestedCamera) ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.CAMERA),
                    REQUEST_PERMISSION_CAMERA
                )
                mPermissionRequestedCamera = true
            } else if (!hasLocationPermission) {
                mConductImage!!.setImageResource(R.drawable.ic_perm_device_information_white_144dp)
                mConductText.setText(R.string.mesg_locationPermissionRequiredAny)
                mConductButton!!.setText(R.string.butn_enable)
                mConductButton!!.setOnClickListener { v: View? ->
                    mConnections!!.validateLocationPermission(
                        this,
                        REQUEST_PERMISSION_LOCATION
                    )
                }
                if (!mPermissionRequestedLocation) ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ), REQUEST_PERMISSION_CAMERA
                )
                mPermissionRequestedLocation = true
            } else {
                mConductImage!!.setImageResource(R.drawable.ic_signal_wifi_off_white_144dp)
                mConductText.setText(R.string.text_scanQRWifiRequired)
                mConductButton!!.setText(R.string.butn_enable)
                mConductButton!!.setOnClickListener { v: View? -> mConnections!!.turnOnWiFi(this, this) }
            }
        } else {
            mBarcodeView.resume()
            mConductText.setText(R.string.help_scanQRCode)
        }
        setConductItemsShowing(!state)
        mBarcodeView.setVisibility(if (state) View.VISIBLE else View.GONE)
    }

    protected fun setConductItemsShowing(showing: Boolean) {
        mConductContainer!!.visibility = if (showing) View.VISIBLE else View.GONE
    }

    override fun onDeviceReached(deviceRoute: DeviceRoute?) {
        setResult(
            RESULT_OK, Intent()
                .putExtra(EXTRA_DEVICE, deviceRoute.device)
                .putExtra(EXTRA_DEVICE_ADDRESS, deviceRoute.address)
        )
        finish()
    }

    override fun onTaskStateChange(task: BaseAttachableAsyncTask, state: AsyncTask.State?) {
        if (task is DeviceIntroductionTask) {
            when (state) {
                AsyncTask.State.Starting -> updateState(true)
                AsyncTask.State.Finished -> updateState(false)
            }
        }
        updateState(!task.isFinished)
    }

    override fun onTaskMessage(message: TaskMessage): Boolean {
        if (message.sizeOfActions() > 1) runOnUiThread {
            message.toDialogBuilder(this).show()
        } else if (message.sizeOfActions() <= 1) runOnUiThread {
            message.toSnackbar(mTaskInterruptButton).show()
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