package com.genonbeta.core.util;

import android.content.*;
import android.os.*;
import java.util.*;

public class BackgroundUpdateAsyncTask extends AsyncTask<Object, Object, Object>
{

	@Override
	protected Object doInBackground(Object[] p1)
	{
		// TODO: Implement this method
		return null;
	}
	
	public static void doUpdate(Context context, UpdateInstance instance)
	{
		new BackgroundUpdateAsyncTask();
	}
	
	public abstract static class UpdateInstance
	{
		public abstract void onStartUpdate();
		public abstract void onFinishUpdate();
	}
}
