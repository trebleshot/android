package com.genonbeta.TrebleShot.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

/**
 * created by: Veli
 * date: 23.02.2018 19:50
 */

public class QRScannerActivity extends AppCompatActivity
{
	private CaptureManager mManager;
	private DecoratedBarcodeView mScannerView;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mScannerView = initializeContent();

		mManager = new CaptureManager(this, mScannerView);
		mManager.initializeFromIntent(getIntent(), savedInstanceState);
		mManager.decode();
	}

	protected DecoratedBarcodeView initializeContent()
	{
		setContentView(com.google.zxing.client.android.R.layout.zxing_capture);
		return (DecoratedBarcodeView) findViewById(com.google.zxing.client.android.R.id.zxing_barcode_scanner);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mManager.onResume();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		mManager.onPause();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		mManager.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		mManager.onSaveInstanceState(outState);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
	{
		mManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		return mScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
	}
}
