package com.genonbeta.TrebleShot.dialog;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ActiveConnectionListAdapter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

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
