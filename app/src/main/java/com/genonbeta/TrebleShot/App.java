package com.genonbeta.TrebleShot;

import android.app.Application;
import android.content.Intent;
import android.os.Build;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.WritablePathObject;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;

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

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			AccessDatabase accessDatabase = new AccessDatabase(getApplicationContext());

			ArrayList<WritablePathObject> permittedUriList = accessDatabase
					.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_WRITABLEPATH), WritablePathObject.class);

			for (WritablePathObject pathObject : permittedUriList)
				getContentResolver().takePersistableUriPermission(pathObject.path,
						Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		}
	}
}
