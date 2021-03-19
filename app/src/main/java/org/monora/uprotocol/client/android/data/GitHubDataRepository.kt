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

package org.monora.uprotocol.client.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.monora.uprotocol.client.android.remote.GitHubService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubDataRepository @Inject constructor(
    private val gitHubService: GitHubService,
) {
    suspend fun getContributors() = withContext(Dispatchers.IO) { gitHubService.contributors() }

    suspend fun getReleases() = withContext(Dispatchers.IO) { gitHubService.releases() }
}