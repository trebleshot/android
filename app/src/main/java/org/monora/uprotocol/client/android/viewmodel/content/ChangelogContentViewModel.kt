package org.monora.uprotocol.client.android.viewmodel.content

import org.monora.uprotocol.client.android.BuildConfig
import org.monora.uprotocol.client.android.remote.model.Release

class ChangelogContentViewModel(release: Release) {
    val name = release.name

    val changelog = release.changelog.trim()

    val versionBeingRun = BuildConfig.VERSION_NAME == release.tag
}