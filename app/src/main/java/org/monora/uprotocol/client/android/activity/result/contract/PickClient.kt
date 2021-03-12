package org.monora.uprotocol.client.android.activity.result.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import org.monora.uprotocol.client.android.activity.BarcodeScannerActivity
import org.monora.uprotocol.client.android.activity.ManualConnectionActivity
import org.monora.uprotocol.client.android.activity.PickClientActivity
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.client.android.model.ClientRoute

class PickClient : ActivityResultContract<PickClient.ConnectionMode, ClientRoute?>() {
    override fun createIntent(context: Context, input: ConnectionMode): Intent = when (input) {
        ConnectionMode.Barcode -> Intent(context, BarcodeScannerActivity::class.java)
        ConnectionMode.Manual -> Intent(context, ManualConnectionActivity::class.java)
        else -> Intent(context, PickClientActivity::class.java).putExtra(EXTRA_CONNECTION_MODE, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ClientRoute? {
        if (resultCode != Activity.RESULT_OK) return null

        val client: UClient? = intent?.getParcelableExtra(EXTRA_CLIENT)
        val address: UClientAddress? = intent?.getParcelableExtra(EXTRA_CLIENT_ADDRESS)

        return if (client != null && address != null) ClientRoute(client, address) else null
    }

    enum class ConnectionMode {
        WaitForRequests, Return, Barcode, Manual
    }

    companion object {
        private const val EXTRA_CLIENT = "extraClient"

        private const val EXTRA_CLIENT_ADDRESS = "extraClient"

        const val EXTRA_CONNECTION_MODE = "extraConnectionMode"

        fun returnResult(activity: Activity, client: UClient?, address: UClientAddress?) {
            activity.setResult(
                AppCompatActivity.RESULT_OK, Intent()
                    .putExtra(EXTRA_CLIENT, client)
                    .putExtra(EXTRA_CLIENT_ADDRESS, address)
            )
            activity.finish()
        }
    }
}