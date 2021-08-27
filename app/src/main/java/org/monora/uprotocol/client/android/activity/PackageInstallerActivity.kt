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

package org.monora.uprotocol.client.android.activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller.*
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.widget.ContentLoadingProgressBar
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.transition.TransitionManager
import com.genonbeta.android.framework.io.OpenableContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.Activity
import javax.inject.Inject

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class PackageInstallerActivity : Activity() {
    @Inject
    lateinit var factory: PackageInstallerViewModel.Factory

    private val viewModel: PackageInstallerViewModel by viewModels {
        PackageInstallerViewModel.ModelFactory(factory, uri)
    }

    private lateinit var uri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_package_installer)

        uri = intent?.data ?: run {
            finish()
            return
        }

        val container = findViewById<ViewGroup>(R.id.container)
        val statusIcon = findViewById<ImageView>(R.id.statusIcon)
        val appInfoGroup = findViewById<Group>(R.id.appInfoGroup)
        val appIcon = findViewById<ImageView>(R.id.appIcon)
        val appLabel = findViewById<TextView>(R.id.appLabel)
        val statusText = findViewById<TextView>(R.id.statusText)
        val progress = findViewById<ContentLoadingProgressBar>(R.id.progressBar)

        viewModel.installationState.observe(this) {
            when (it) {
                is PreparationState.Loading -> statusText.setText(R.string.installing)
                is PreparationState.Error -> {
                    Toast.makeText(
                        this@PackageInstallerActivity, R.string.unknown_failure, Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                is PreparationState.Progress -> progress.progress = (it.progress * 100).toInt()
                is PreparationState.Finished -> {
                    if (viewModel.isAborted) finish()
                    statusIcon.setImageResource(
                        if (it.isSuccessful) R.drawable.round_done_24 else R.drawable.ic_error_outline_white_24dp
                    )
                    statusIcon.visibility = View.VISIBLE
                }
            }

            if (it.isInProgress) progress.show() else progress.hide()

            TransitionManager.beginDelayedTransition(container)
        }

        viewModel.installationStatusText.observe(this) {
            statusText.text = it
        }

        viewModel.installationDetails.observe(this) {
            appInfoGroup.visibility = View.VISIBLE
            appLabel.text = it.label

            if (it.icon != null) {
                GlideApp.with(appIcon)
                    .load(it.icon)
                    .into(appIcon)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (PACKAGE_INSTALLED_ACTION == intent.action) {
            viewModel.submitInstallationResult(this, intent)
        }
    }

    companion object {
        internal const val PACKAGE_INSTALLED_ACTION = "com.example.android.apis.content.SESSION_API_PACKAGE_INSTALLED"
    }
}

class PackageInstallerViewModel @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted private val uri: Uri,
) : ViewModel() {
    private val packageManager = context.packageManager

    private val packageInstaller = packageManager.packageInstaller

    private val _installationDetails = MutableLiveData<PackageDetails>()

    val installationDetails = liveData {
        emitSource(_installationDetails)
    }

    private val _installationState = MutableLiveData<PreparationState>()

    val installationState = liveData {
        emitSource(_installationState)
    }

    private val _installationStatusText = MutableLiveData<String>()

    val installationStatusText = liveData {
        emitSource(_installationStatusText)
    }

    val isAborted: Boolean
        get() = installationStatusText.value == null

    private val sessionCallback by lazy {
        MySessionCallback()
    }

    private val sessionId: Int = packageInstaller.createSession(SessionParams(SessionParams.MODE_FULL_INSTALL))

    override fun onCleared() {
        super.onCleared()
        packageInstaller.unregisterSessionCallback(sessionCallback)
    }

    init {
        packageInstaller.registerSessionCallback(sessionCallback)

        viewModelScope.launch(Dispatchers.IO) {
            _installationState.postValue(PreparationState.Loading)

            try {
                val content = OpenableContent.from(context, uri)

                packageInstaller.openSession(sessionId).use { session ->
                    try {
                        session.openWrite(content.name, 0, content.size).use { packageInSession ->
                            content.openInputStream(context).use { inputStream ->
                                val buffer = ByteArray(16384)
                                var length: Int
                                while (inputStream.read(buffer).also { length = it } >= 0) {
                                    packageInSession.write(buffer, 0, length)
                                }
                            }
                        }

                        val intent = Intent(context, PackageInstallerActivity::class.java)
                            .setAction(PackageInstallerActivity.PACKAGE_INSTALLED_ACTION)
                        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
                        val statusReceiver = pendingIntent.intentSender

                        session.commit(statusReceiver)
                    } catch (e: RuntimeException) {
                        session.abandon()
                        throw e
                    }
                }
            } catch (e: Exception) {
                _installationState.postValue(PreparationState.Error(e))
            }
        }
    }

    fun submitInstallationResult(context: Context, intent: Intent) {
        val extras = intent.extras ?: return
        val status = extras.getInt(EXTRA_STATUS)

        when (status) {
            STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = extras[Intent.EXTRA_INTENT] as Intent
                ContextCompat.startActivity(context, confirmIntent, null)
            }
            STATUS_SUCCESS -> _installationStatusText.postValue(context.getString(R.string.package_install_success))
            else -> {
                val errorMsgResId = when (status) {
                    STATUS_FAILURE -> R.string.package_install_failure
                    STATUS_FAILURE_ABORTED -> R.string.package_install_aborted
                    STATUS_FAILURE_BLOCKED -> R.string.package_install_blocked
                    STATUS_FAILURE_CONFLICT -> R.string.package_install_conflicted
                    STATUS_FAILURE_INCOMPATIBLE -> R.string.package_install_incompatible
                    STATUS_FAILURE_INVALID -> R.string.package_install_invalid
                    STATUS_FAILURE_STORAGE -> R.string.package_install_failure_storage
                    else -> R.string.package_install_failure_unknown
                }

                _installationStatusText.postValue(context.getString(errorMsgResId))
            }
        }
    }

    private inner class MySessionCallback : SessionCallback() {
        override fun onCreated(sessionId: Int) {}

        override fun onBadgingChanged(sessionId: Int) {
            if (sessionId == this@PackageInstallerViewModel.sessionId) {
                packageInstaller.getSessionInfo(sessionId)?.let {
                    _installationDetails.postValue(PackageDetails(it.appLabel, it.appPackageName, it.appIcon))
                }
            }
        }

        override fun onActiveChanged(sessionId: Int, active: Boolean) {}

        override fun onProgressChanged(sessionId: Int, progress: Float) {
            if (this@PackageInstallerViewModel.sessionId == sessionId) {
                _installationState.postValue(PreparationState.Progress(progress))
            }
        }

        override fun onFinished(sessionId: Int, success: Boolean) {
            if (this@PackageInstallerViewModel.sessionId == sessionId) {
                _installationState.postValue(PreparationState.Finished(success))
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(uri: Uri): PackageInstallerViewModel
    }

    class ModelFactory(
        private val factory: Factory,
        private val uri: Uri,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(PackageInstallerViewModel::class.java)) {
                "Requested unknown view model type"
            }

            return factory.create(uri) as T
        }
    }
}

sealed class PreparationState(val isInProgress: Boolean = false) {
    object Loading : PreparationState(isInProgress = true)

    class Progress(val progress: Float) : PreparationState(isInProgress = true)

    class Error(val exception: Exception) : PreparationState()

    class Finished(val isSuccessful: Boolean) : PreparationState()
}

data class PackageDetails(
    val label: CharSequence?,
    val name: String?,
    val icon: Bitmap?
)
