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

package org.monora.uprotocol.client.android.activity.result.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import org.monora.uprotocol.client.android.activity.PickClientActivity
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.client.android.model.ClientRoute

class PickClient : ActivityResultContract<PickClient.ConnectionMode, ClientRoute?>() {
    override fun createIntent(context: Context, input: ConnectionMode): Intent = when (input) {
        else -> Intent(context, PickClientActivity::class.java).putExtra(EXTRA_CONNECTION_MODE, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ClientRoute? {
        if (resultCode != Activity.RESULT_OK) return null

        val client: UClient? = intent?.getParcelableExtra(EXTRA_CLIENT)
        val address: UClientAddress? = intent?.getParcelableExtra(EXTRA_CLIENT_ADDRESS)

        return if (client != null && address != null) ClientRoute(client, address) else null
    }

    enum class ConnectionMode {
        WaitForRequests, Return
    }

    companion object {
        private const val EXTRA_CLIENT = "extraClient"

        private const val EXTRA_CLIENT_ADDRESS = "extraClientAddress"

        const val EXTRA_CONNECTION_MODE = "extraConnectionMode"

        fun returnResult(activity: Activity, clientRoute: ClientRoute) {
            returnResult(activity, clientRoute.client, clientRoute.address)
        }

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