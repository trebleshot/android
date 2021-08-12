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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.config.AppConfig

class AboutFragment : Fragment(R.layout.layout_about) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.monoraHomeButton).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_ORG_HOME)))
        }
        view.findViewById<View>(R.id.githubButton).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_REPO_APP)))
        }
        view.findViewById<View>(R.id.localizeButton).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_TRANSLATE)))
        }
        view.findViewById<View>(R.id.telegramButton).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_TELEGRAM_CHANNEL)))
        }
        view.findViewById<View>(R.id.authorWeb).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://velitasali.com/")))
        }
        view.findViewById<View>(R.id.authorGit).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://github.com/velitasali")))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.about, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.licenses -> findNavController().navigate(
                AboutFragmentDirections.actionAboutFragmentToLicensesFragment()
            )
            R.id.changelog -> findNavController().navigate(
                AboutFragmentDirections.actionAboutFragmentToChangelogFragment2()
            )
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
