package com.genonbeta.TrebleShot;

import android.app.Application;
import android.content.Intent;
import android.content.UriPermission;
import android.os.Build;

/**
 * created by: Veli
 * date: 25.02.2018 01:23
 */

public class App extends Application
{
	@Override
	public void onCreate()
	{
		super.onCreate();

		// assures that permissions are accessible after reboot or in any normal state
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			for (UriPermission uriPermission : getContentResolver().getPersistedUriPermissions())
				getContentResolver().takePersistableUriPermission(uriPermission.getUri(),
						Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		}
	}
}
