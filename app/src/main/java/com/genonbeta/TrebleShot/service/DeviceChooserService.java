package com.genonbeta.TrebleShot.service;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;

import com.amulyakhare.textdrawable.TextDrawable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.database.DeviceRegistry;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.NetworkDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by: veli
 * Date: 5/23/17 5:16 PM
 */

@RequiresApi(api = Build.VERSION_CODES.M)
public class DeviceChooserService extends ChooserTargetService
{
	@Override
	public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName, IntentFilter matchedFilter)
	{
		DeviceRegistry deviceRegistry = new DeviceRegistry(getApplicationContext());
		ArrayList<ChooserTarget> list = new ArrayList<>();

		for (NetworkDevice device : deviceRegistry.getDeviceList()) {
			if (device.isLocalAddress)
				continue;

			Bundle bundle = new Bundle();

			bundle.putString(ShareActivity.EXTRA_DEVICE_ID, device.deviceId);

			String firstLetters = ApplicationHelper.getFirstLetters(device.user, 1);
			TextDrawable textImage = TextDrawable.builder().buildRoundRect(firstLetters.length() > 0 ? firstLetters : "?", ContextCompat.getColor(this, R.color.colorTextDrawable), 100);
			Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);

			textImage.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			textImage.draw(canvas);

			float result = (float) device.lastUsageTime / (float) System.currentTimeMillis();

			list.add(new ChooserTarget(
					device.user,
					Icon.createWithBitmap(bitmap),
					result,
					targetActivityName,
					bundle
			));
		}

		return list;
	}
}
