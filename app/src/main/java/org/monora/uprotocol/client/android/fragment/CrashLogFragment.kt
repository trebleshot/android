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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.viewmodel.CrashLogViewModel

@AndroidEntryPoint
class CrashLogFragment : BottomSheetDialogFragment() {
    private val viewModel: CrashLogViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_crash_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val crashLogText = view.findViewById<TextView>(R.id.crashLog)
        val copyButton = view.findViewById<MaterialButton>(R.id.copyButton)

        viewModel.crashLog.observe(viewLifecycleOwner) { report ->
            crashLogText.text = report

            copyButton.setOnClickListener {
                val clipboardManager = it.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(ClipData.newPlainText(getString(R.string.crash_report), report))

                Toast.makeText(it.context, R.string.copy_text_to_clipboard_success, Toast.LENGTH_SHORT).show()

                findNavController().navigateUp()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.clearCrashLog()
    }

    companion object {
        private const val TAG = "CrashLogFragment"
    }
}
