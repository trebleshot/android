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
package org.monora.uprotocol.client.android.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.result.contract.PickClient
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.model.ClientRoute
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel

@AndroidEntryPoint
class PickClientActivity : Activity() {
    private val clientPickerViewModel: ClientPickerViewModel by viewModels()

    private val connectionMode by lazy {
        intent?.getSerializableExtra(PickClient.EXTRA_CONNECTION_MODE) ?: PickClient.ConnectionMode.WaitForRequests
    }

    private val navController by lazy {
        navController(R.id.nav_host_fragment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_pick_client)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            title = destination.label
        }

        /*clientPickerViewModel.clientRoute.observe(this) {
            handleResult(it)
        }*/

        clientPickerViewModel.client.observe(this) {
            Snackbar.make(
                findViewById(android.R.id.content),
                it.nickname,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            return navController.popBackStack()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleResult(clientRoute: ClientRoute) {
        if (PickClient.ConnectionMode.Return == connectionMode) {
            PickClient.returnResult(this, clientRoute.client, clientRoute.address)
        } else if (PickClient.ConnectionMode.WaitForRequests == connectionMode) {
            Snackbar.make(findViewById(android.R.id.content), R.string.mesg_completing, Snackbar.LENGTH_LONG).show()
        }
    }
}

