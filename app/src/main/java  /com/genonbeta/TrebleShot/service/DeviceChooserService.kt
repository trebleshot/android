/*
 * Copyright (C) 2019 Veli Tasalı
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
package com.genonbeta.TrebleShot.service

import android.content.ComponentName
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.*
import android.service.chooser.ChooserTarget
import android.service.chooser.ChooserTargetService
import androidx.annotation.RequiresApi
import com.genonbeta.TrebleShot.activity.ShareActivity
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.model.Device
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable.IShapeBuilder
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.android.database.SQLQuery
import java.util.*

/**
 * Created by: veli
 * Date: 5/23/17 5:16 PM
 */
@RequiresApi(api = Build.VERSION_CODES.M)
class DeviceChooserService : ChooserTargetService() {
    override fun onGetChooserTargets(
        targetActivityName: ComponentName,
        matchedFilter: IntentFilter
    ): List<ChooserTarget> {
        val kuick = AppUtils.getKuick(getApplicationContext())
        val list: MutableList<ChooserTarget> = ArrayList<ChooserTarget>()

        // use default accent color for light theme
        val iconBuilder: IShapeBuilder = AppUtils.getDefaultIconBuilder(getApplicationContext())
        for (device in kuick.castQuery(SQLQuery.Select(Kuick.TABLE_DEVICES), Device::class.java)) {
            if (device.isLocal) continue
            val bundle = Bundle()
            bundle.putString(ShareActivity.EXTRA_DEVICE_ID, device.uid)
            val textImage: TextDrawable = iconBuilder.buildRound(device.username)
            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            textImage.setBounds(0, 0, canvas.width, canvas.height)
            textImage.draw(canvas)
            val result = device.lastUsageTime.toFloat() / System.currentTimeMillis().toFloat()
            list.add(
                ChooserTarget(
                    device.username, Icon.createWithBitmap(bitmap), result, targetActivityName,
                    bundle
                )
            )
        }
        return list
    }
}