package com.genonbeta.TrebleShot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.genonbeta.TrebleShot.service.CommunicationService;

public class NetworkStatusReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent.hasExtra("networkInfo"))
			evaluateTheCondition((NetworkInfo) intent.getParcelableExtra("networkInfo"), context);
	}

	protected void evaluateTheCondition(NetworkInfo info, final Context context)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

		if (!preferences.getBoolean("serviceLock", false) && preferences.getBoolean("allow_autoconnect", true) && info.isConnected())
			context.startService(new Intent(context, CommunicationService.class));

		if (preferences.getBoolean("scan_devices_auto", false) && info.isConnected())
			new Thread()
			{
				@Override
				public void run()
				{
					super.run();
					try {
						// The interface may not be created properly yet and we should give some time
						Thread.sleep(1700);
						context.sendBroadcast(new Intent(DeviceScannerProvider.ACTION_SCAN_DEVICES));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}.start();
	}
}
