package com.genonbeta.android.framework.widget.recyclerview.fastscroll

import androidx.test.runner.AndroidJUnit4
import android.content.ContentResolver
import kotlin.Throws
import com.genonbeta.android.framework.io.StreamInfo.FolderStateException
import android.provider.OpenableColumns
import com.genonbeta.android.framework.io.StreamInfo
import com.genonbeta.android.framework.io.LocalDocumentFile
import com.genonbeta.android.framework.io.StreamDocumentFile
import androidx.annotation.RequiresApi
import android.provider.DocumentsContract
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import android.view.View
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`

/**
 * Created by mklimczak on 31/07/15.
 */
object Utils {
    fun getViewRawY(view: View?): Float {
        val location = IntArray(2)
        location[0] = 0
        location[1] = view.getY() as Int
        (view.getParent() as View).getLocationInWindow(location)
        return location[1]
    }

    fun getViewRawX(view: View?): Float {
        val location = IntArray(2)
        location[0] = view.getX() as Int
        location[1] = 0
        (view.getParent() as View).getLocationInWindow(location)
        return location[0]
    }

    fun getValueInRange(min: Float, max: Float, value: Float): Float {
        val minimum = Math.max(min, value)
        return Math.min(minimum, max)
    }

    fun setBackground(view: View?, drawable: Drawable?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(drawable)
        } else {
            view.setBackgroundDrawable(drawable)
        }
    }
}