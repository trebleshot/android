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
package org.monora.uprotocol.client.android.service.backgroundservice

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Job

class Task(val name: String, val params: Any, val job: Job, state: LiveData<State>) {
    val state = liveData {
        emitSource(state)
    }

    sealed class State(val running: Boolean = false) {
        object Pending : State()

        class Running(val message: String) : State(running = true)

        class Progress(val message: String, val total: Int, val progress: Int) : State(running = true)

        class Error(val error: Exception): State()

        object Finished : State()
    }

    data class Change<T>(val task: Task, val exported: T, val state: State)
}