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

package org.monora.uprotocol.client.android.backend

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiConfiguration
import android.util.Log
import androidx.collection.ArraySet
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.activity.HomeActivity
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.service.BackgroundService
import org.monora.uprotocol.client.android.service.backgroundservice.Task
import org.monora.uprotocol.client.android.util.DynamicNotification
import org.monora.uprotocol.client.android.util.Permissions
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

typealias TaskFilter = (Task) -> Boolean

typealias TaskRegistry<T> = (applicationScope: CoroutineScope, params: T, state: MutableLiveData<Task.State>) -> Job

typealias TaskSubscriber<T> = (Task) -> T?

// is this my new favorite font? I think it is!

@Singleton
class Backend @Inject constructor(
    @ApplicationContext val context: Context,
    services: Lazy<Services>,
) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val bgIntent = Intent(context, BackgroundService::class.java)

    private val bgStopIntent = Intent(context, BackgroundService::class.java).also {
        it.action = BackgroundService.ACTION_STOP_BG_SERVICE
    }

    val bgNotification
        get() = taskNotification?.takeIf { hasTasks() } ?: services.notifications.foregroundNotification

    private var foregroundActivitiesCount = 0

    private var foregroundActivity: Activity? = null

    val services: Services by lazy {
        services.get()
    }

    private val taskSet: MutableSet<Task> = ArraySet()

    private val _tasks = MutableLiveData<List<Task>>(emptyList())

    val tasks = liveData {
        emitSource(_tasks)
    }

    private var taskNotification: DynamicNotification? = null

    private var taskNotificationTime: Long = 0

    private var tileEnabled = false

    private var _tileState = MutableLiveData(false)

    var tileState = liveData {
        emitSource(_tileState)
    }

    fun cancelAllTasks() {
        val cancellationCause = CancellationException("Application exited")
        applicationScope.coroutineContext.cancelChildren(cancellationCause)

        synchronized(taskSet) {
            taskSet.forEach {
                if (!it.job.isCancelled) it.job.cancel(cancellationCause)
            }
        }
    }

    fun cancelMatchingTasks(filter: TaskFilter): Boolean {
        return synchronized(taskSet) {
            var cancelledAny = false

            taskSet.forEach {
                if (filter(it)) {
                    it.job.cancel()
                    cancelledAny = true
                }
            }

            cancelledAny
        }
    }

    private fun ensureStarted() = services.start()

    fun ensureStartedAfterWelcoming() {
        takeBgServiceFgIfNeeded(true)
    }

    private fun ensureStopped() {
        services.stop()
        notifyTileState(false)
        cancelAllTasks()
    }

    fun getHotspotConfig(): WifiConfiguration? {
        return services.hotspotManager.configuration
    }

    fun hasTasks(): Boolean = taskSet.isNotEmpty()

    fun hasTask(filter: TaskFilter): Boolean {
        return synchronized(taskSet) {
            taskSet.forEach {
                if (filter(it)) return@synchronized true
            }

            false
        }
    }

    @Synchronized
    fun notifyActivityInForeground(activity: Activity, inForeground: Boolean) {
        if (!inForeground && foregroundActivitiesCount == 0) return
        val wasInForeground = foregroundActivitiesCount > 0
        foregroundActivitiesCount += if (inForeground) 1 else -1
        val isInForeground = foregroundActivitiesCount > 0
        val newlySwitchedGrounds = isInForeground != wasInForeground

        if (Permissions.checkRunningConditions(context)) {
            takeBgServiceFgIfNeeded(newlySwitchedGrounds)
        }

        foregroundActivity = if (newlySwitchedGrounds) null else if (inForeground) activity else foregroundActivity
    }

    fun notifyFileRequest(client: UClient, transfer: Transfer, itemList: List<UTransferItem>) {
        val activity = foregroundActivity

        if (activity == null) {
            services.notifications.notifyTransferRequest(client, transfer, itemList)
        } else {
            // TODO: 7/25/21 Also insert the transfer details and navigate to transfer details fragment
            activity.startActivity(Intent(activity, HomeActivity::class.java))
        }
    }

    private fun notifyTileState(newState: Boolean) {
        tileEnabled = newState
        _tileState.value = newState
    }

    fun publishTaskNotifications(force: Boolean): Boolean {
        val notified = System.nanoTime()
        if (notified <= taskNotificationTime && !force) return false
        if (!hasTasks()) {
            takeBgServiceFgIfNeeded(newlySwitchedGrounds = false)
            return false
        }

        taskNotificationTime = System.nanoTime() + AppConfig.DELAY_DEFAULT_NOTIFICATION * 1e6.toLong()
        taskNotification = services.notifications.notifyTasksNotification(taskSet.toList(), taskNotification)

        return true
    }

    fun <T : Any> register(name: String, params: T, registry: TaskRegistry<T>): Task {
        val state = MutableLiveData<Task.State>(Task.State.Pending)
        val job = registry(applicationScope, params, state)
        val task = Task(name, params, job, state)

        registerInternal(task, state, true)

        return task
    }

    private fun registerInternal(task: Task, state: MutableLiveData<Task.State>, addition: Boolean) {
        // For observers to work correctly and to set values instead of posting them, we launch a coroutine on main
        // thread.
        applicationScope.launch {
            val result = synchronized(taskSet) {
                if (addition) taskSet.add(task) else taskSet.remove(task)
            }

            if (result) {
                Log.d(TAG, "registerInternal: ${if (addition) "Registered" else "Removed"} `${task.name}`")

                if (addition) {
                    task.state.observeForever(object : Observer<Task.State> {
                        override fun onChanged(t: Task.State) {
                            val changesPosted: Boolean

                            when (t) {
                                is Task.State.Pending, is Task.State.Finished -> {
                                    changesPosted = true
                                    publishTaskNotifications(true)
                                }
                                is Task.State.Running, is Task.State.Progress -> {
                                    changesPosted = publishTaskNotifications(false)
                                }
                                is Task.State.Error -> {
                                    changesPosted = false
                                }
                            }

                            if (t is Task.State.Finished) {
                                task.state.removeObserver(this)
                            }

                            if (changesPosted) {
                                _tasks.value = taskSet.toList()
                            }
                        }
                    })

                    task.job.invokeOnCompletion {
                        registerInternal(task, state, false)
                    }
                } else {
                    state.value = Task.State.Finished
                }
            }
        }
    }

    fun <T> subscribeToTask(condition: TaskSubscriber<T>): LiveData<Task.Change<T>?> {
        val dummyLiveData = liveData<Task.Change<T>?> {
            emit(null)
        }
        var previous: Pair<Task, LiveData<Task.Change<T>?>>? = null

        return Transformations.switchMap(tasks) { list ->
            if (previous == null || Task.State.Finished == previous?.first?.state?.value) {
                previous = null

                for (task in list) {
                    val exported = condition(task)
                    if (exported != null) {
                        previous = task to Transformations.map(task.state) {
                            Task.Change(task, exported, it)
                        }
                        break
                    }
                }
            }

            previous?.second ?: dummyLiveData
        }
    }

    fun <T> subscribeToTasks(condition: TaskSubscriber<T>): LiveData<List<Task.Change<T>>> {
        return Transformations.switchMap(tasks) { list ->
            liveData<List<Task.Change<T>>> {
                val filtered = mutableListOf<Task.Change<T>>()

                for (task in list) {
                    val exported = condition(task)
                    if (exported != null) {
                        filtered.add(Task.Change(task, exported, task.state.value ?: Task.State.Pending))
                    }
                }

                emit(filtered)
            }
        }
    }

    fun takeBgServiceFgIfNeeded(
        newlySwitchedGrounds: Boolean,
        forceStop: Boolean = false,
    ) {
        // Do not try to tweak this!!!
        val hasTasks = hasTasks()
        val hasServices = (services.hotspotManager.started || services.isServingAnything || tileEnabled)
        val inForeground = foregroundActivitiesCount > 0
        val newlyInForeground = newlySwitchedGrounds && inForeground
        val newlyInBackground = newlySwitchedGrounds && !inForeground
        val keepRunning = (hasServices || hasTasks) && !forceStop

        if (newlyInForeground || (tileEnabled && !forceStop)) {
            ensureStarted()
        } else if (!inForeground && !keepRunning) {
            ensureStopped()
        }

        if (newlyInBackground && keepRunning) {
            ContextCompat.startForegroundService(context, bgIntent)
        } else if (newlyInForeground || (!inForeground && !keepRunning)) {
            ContextCompat.startForegroundService(context, bgStopIntent)
        }

        if (!forceStop && !hasTasks) {
            if (hasServices && !inForeground) {
                services.notifications.foregroundNotification.show()
            } else {
                services.notifications.foregroundNotification.cancel()
            }
        }
    }

    fun takeBgServiceFgThroughTogglingTile() {
        notifyTileState(!tileEnabled)
        takeBgServiceFgIfNeeded(newlySwitchedGrounds = false)
    }

    fun toggleHotspot() = services.toggleHotspot()

    companion object {
        private const val TAG = "Backend"
    }
}
