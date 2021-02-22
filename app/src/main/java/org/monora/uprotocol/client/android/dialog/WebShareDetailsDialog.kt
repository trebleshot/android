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
package org.monora.uprotocol.client.android.dialog

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.GlideApp
import com.genonbeta.TrebleShot.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder

/**
 * created by: veli
 * date: 4/8/19 9:16 AM
 */
class WebShareDetailsDialog(context: Context, address: String?) : AlertDialog.Builder(context) {
    init {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_web_share_details, null)
        val qrImage = view.findViewById<ImageView>(R.id.image)
        val addressText = view.findViewById<TextView>(R.id.text)

        //setTitle(R.string.text_webShare);
        setView(view)
        setPositiveButton(R.string.butn_close, null)
        addressText.text = address

        try {
            val formatWriter = MultiFormatWriter()
            val bitMatrix: BitMatrix = formatWriter.encode(address, BarcodeFormat.QR_CODE, 400, 400)
            val encoder = BarcodeEncoder()
            val bitmap: Bitmap = encoder.createBitmap(bitMatrix)
            GlideApp.with(getContext())
                .load(bitmap)
                .into(qrImage)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }
}