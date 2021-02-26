package org.monora.uprotocol.client.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.config.AppConfig

object Activities {
    fun startApplicationDetails(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun startFeedbackActivity(context: Context) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(AppConfig.EMAIL_DEVELOPER))
            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.text_appName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val logFile = Files.createLog(context)
        if (logFile != null) try {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(
                    Intent.EXTRA_STREAM,
                    com.genonbeta.android.framework.util.Files.getSecureUri(context, logFile)
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.butn_feedbackContact)))
    }
}