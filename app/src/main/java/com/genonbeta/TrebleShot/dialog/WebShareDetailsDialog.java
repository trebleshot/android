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

package com.genonbeta.TrebleShot.dialog;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * created by: veli
 * date: 4/8/19 9:16 AM
 */
public class WebShareDetailsDialog extends AlertDialog.Builder
{
    public WebShareDetailsDialog(@NonNull Context context, String address)
    {
        super(context);

        View view = LayoutInflater.from(context).inflate(R.layout.layout_web_share_details, null);
        ImageView qrImage = view.findViewById(R.id.image);
        TextView addressText = view.findViewById(R.id.text);

        //setTitle(R.string.text_webShare);
        setView(view);
        setPositiveButton(R.string.butn_close, null);

        addressText.setText(address);

        try {
            MultiFormatWriter formatWriter = new MultiFormatWriter();
            BitMatrix bitMatrix = formatWriter.encode(address, BarcodeFormat.QR_CODE, 400, 400);
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.createBitmap(bitMatrix);

            GlideApp.with(getContext())
                    .load(bitmap)
                    .into(qrImage);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}
