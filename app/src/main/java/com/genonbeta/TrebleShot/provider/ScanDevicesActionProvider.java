package com.genonbeta.TrebleShot.provider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.receiver.DeviceScannerProvider;

/**
 * Created by: veli
 * Date: 2/5/17 8:12 PM
 */

public class ScanDevicesActionProvider extends ActionProvider
{
	private static final String TAG = ScanDevicesActionProvider.class.getSimpleName();

	private ImageView mReloadImage;

	/**
	 * Creates a new instance.
	 *
	 * @param context Context for accessing resources.
	 */
	public ScanDevicesActionProvider(Context context)
	{
		super(context);
	}

	@Override
	public View onCreateActionView()
	{
		@SuppressLint("InflateParams")
		View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_running_process, null);

		mReloadImage = (ImageView) view.findViewById(R.id.layout_running_process_reload_image);

		checkScanStatus();

		return view;
	}

	private void checkScanStatus()
	{
		if (mReloadImage == null)
			return;

		mReloadImage.setOnClickListener(
				new View.OnClickListener()
				{
					@Override
					public void onClick(View view)
					{
						boolean isAvailable = ApplicationHelper.getNetworkDeviceScanner().isScannerAvailable();

						if (isAvailable)
							getContext().sendBroadcast(new Intent(DeviceScannerProvider.ACTION_SCAN_DEVICES));
						else
						{
							Toast.makeText(getContext(), R.string.mesg_stopping, Toast.LENGTH_SHORT).show();
							ApplicationHelper.getNetworkDeviceScanner().interrupt();
						}
					}
				}
		);

		if (!ApplicationHelper.getNetworkDeviceScanner().isScannerAvailable())
		{
			RotateAnimation anim = new RotateAnimation(0.0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

			anim.setInterpolator(new LinearInterpolator());
			anim.setRepeatCount(Animation.INFINITE);
			anim.setDuration(700);

			mReloadImage.startAnimation(anim);
		} else
			mReloadImage.setAnimation(null);
	}

	public void refreshStatus()
	{
		checkScanStatus();
	}
}