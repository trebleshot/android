package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Fragment;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;

import java.util.List;

/**
 * created by: veli
 * date: 12/04/18 17:21
 */
public class CodeConnectFragment
		extends Fragment
		implements TitleSupport
{
	private BarcodeView mBarcodeView;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.layout_code_connect, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		mBarcodeView = view.findViewById(R.id.layout_code_connect_barcode_view);

		mBarcodeView.decodeSingle(new BarcodeCallback()
		{
			@Override
			public void barcodeResult(BarcodeResult result)
			{
				createSnackbar(R.string.text_appName)
						.setText(result.getResult().getText())
						.show();
			}

			@Override
			public void possibleResultPoints(List<ResultPoint> resultPoints)
			{

			}
		});
	}

	@Override
	public void onResume()
	{
		super.onResume();
		mBarcodeView.resume();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		mBarcodeView.pauseAndWait();
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_connect);
	}
}
