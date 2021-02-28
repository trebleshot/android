/*
 * Copyright (C) 2020 Veli Tasalı
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
package org.monora.uprotocol.client.android.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import java.net.InetAddress
import java.net.ProtocolException
import java.net.UnknownHostException
import javax.inject.Inject

@AndroidEntryPoint
class ManualConnectionActivity : Activity() {
    @Inject
    lateinit var connectionFactory: ConnectionFactory

    @Inject
    lateinit var persistenceProvider: PersistenceProvider

    @Inject
    lateinit var appDatabase: AppDatabase

    private val editText: AppCompatEditText by lazy {
        findViewById(R.id.editText)
    }

    private val button: Button by lazy {
        findViewById(R.id.confirm_button)
    }

    private val progressBar: ProgressBar by lazy {
        findViewById(R.id.progressBar)
    }

    private val layoutMain: ViewGroup by lazy {
        findViewById(R.id.layout_main)
    }

    private var progress: Boolean
        get() = progressBar.visibility == View.VISIBLE
        set(value) {
            if (progress != value) {
                progressBar.visibility = if (value) View.VISIBLE else View.GONE
                TransitionManager.beginDelayedTransition(layoutMain)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_address_connection)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<View>(R.id.confirm_button).setOnClickListener {
            val address = editText.text?.trim()?.toString()

            if (address.isNullOrEmpty())
                editText.error = getString(R.string.mesg_enterValidHostAddress)
            else {
                lifecycle.coroutineScope.launch {
                    button.isEnabled = false
                    progress = true

                    try {
                        val inetAddress = withContext(Dispatchers.IO) { InetAddress.getByName(address) }
                        val bridge = withContext(Dispatchers.IO) {
                            val bridge = CommunicationBridge.connect(
                                connectionFactory, persistenceProvider, inetAddress, null, 0
                            )

                            if (!bridge.requestAcquaintance()) {
                                throw ProtocolException("Should not have returned this")
                            }

                            bridge
                        }

                        val client = bridge.remoteClient
                        val clientAddress = bridge.remoteClientAddress

                        if (client !is UClient || clientAddress !is UClientAddress) {
                            throw UnsupportedOperationException("Hello dear")
                        }

                        setResult(
                            RESULT_OK, Intent()
                                .putExtra(EXTRA_CLIENT, client)
                                .putExtra(EXTRA_CLIENT_ADDRESS, clientAddress)
                        )
                        finish()
                    } catch (e: UnknownHostException) {
                        editText.error = getString(R.string.mesg_unknownHostError)
                    } catch (e: Exception) {
                        editText.error = e.message
                        e.printStackTrace()
                    } finally {
                        progress = false
                        button.isEnabled = true
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        editText.requestFocus()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home)
            onBackPressed()
        else
            return super.onOptionsItemSelected(item)
        return true
    }

    companion object {
        val TAG = ManualConnectionActivity::class.java.simpleName

        const val EXTRA_CLIENT = "extraClient"

        const val EXTRA_CLIENT_ADDRESS = "extraClientAddress"
    }
}