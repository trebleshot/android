package org.monora.uprotocol.client.android.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.dialog.PermissionRequests
import java.util.ArrayList

object Permissions {
    fun checkRunningConditions(context: Context): Boolean {
        for (request in getRequiredPermissions(context)) {
            if (ActivityCompat.checkSelfPermission(context, request.permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun getRequiredPermissions(context: Context): List<PermissionRequests.PermissionRequest> {
        val permissionRequests: MutableList<PermissionRequests.PermissionRequest> = ArrayList()
        if (Build.VERSION.SDK_INT >= 16) {
            permissionRequests.add(
                PermissionRequests.PermissionRequest(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    R.string.text_requestPermissionStorage,
                    R.string.text_requestPermissionStorageSummary
                )
            )
        }
        return permissionRequests
    }
}