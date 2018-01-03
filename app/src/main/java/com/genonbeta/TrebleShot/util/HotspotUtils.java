package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class HotspotUtils
{
	private static final String TAG = "HotspotUtils";

	private static Method getWifiApConfiguration;

	private static Method getWifiApState;
	private static Method isWifiApEnabled;
	private static Method setWifiApEnabled;
	private static Method setWifiApConfiguration;
	private static HotspotUtils mInstance = null;

	private WifiConfiguration mPreviousConfig;
	private WifiManager mWifiManager;

	static {
		for (Method method : WifiManager.class.getDeclaredMethods()) {
			switch (method.getName()) {
				case "getWifiApConfiguration":
					getWifiApConfiguration = method;
					break;
				case "getWifiApState":
					getWifiApState = method;
					break;
				case "isWifiApEnabled":
					isWifiApEnabled = method;
					break;
				case "setWifiApEnabled":
					setWifiApEnabled = method;
					break;
				case "setWifiApConfiguration":
					setWifiApConfiguration = method;
					break;
			}
		}
	}

	private HotspotUtils(Context context)
	{
		mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
	}

	public static HotspotUtils getInstance(Context context)
	{
		if (mInstance == null)
			mInstance = new HotspotUtils(context);

		return mInstance;
	}

	private static Object invokeSilently(Method method, Object receiver, Object... args)
	{
		try {
			return method.invoke(receiver, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			Log.e(TAG, "exception in invoking methods: " + e.getMessage());
		}

		return null;
	}

	public static boolean isSupported()
	{
		return getWifiApState != null
				&& isWifiApEnabled != null
				&& setWifiApEnabled != null
				&& getWifiApConfiguration != null;
	}

	public boolean disable()
	{
		unloadPreviousConfig();
		return setHotspotEnabled(mPreviousConfig, false);
	}

	public boolean enable()
	{
		mWifiManager.setWifiEnabled(false);
		return setHotspotEnabled(getConfiguration(), true);
	}

	public boolean enableConfigured(String apName, String passKeyWPA2)
	{
		mWifiManager.setWifiEnabled(false);

		if (mPreviousConfig == null)
			mPreviousConfig = getConfiguration();

		WifiConfiguration wifiConfiguration = new WifiConfiguration();

		wifiConfiguration.SSID = apName;

		if (passKeyWPA2 != null && passKeyWPA2.length() >= 8) {
			wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
			wifiConfiguration.preSharedKey = passKeyWPA2;
		} else
			wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

		return setHotspotEnabled(wifiConfiguration, true);
	}

	public List<HotspotClient> getClientList(final int timeout,
											 final WifiClientConnectionListener listener)
	{
		List<HotspotClient> resultList = getWifiClients();

		if (resultList == null) {
			listener.onWifiClientsScanComplete();
			return null;
		}

		final CountDownLatch latch = new CountDownLatch(resultList.size());
		ExecutorService executorService = Executors.newCachedThreadPool();

		for (final HotspotClient c : resultList) {
			executorService.submit(new Runnable()
			{
				public void run()
				{
					try {
						InetAddress inetAddress = InetAddress.getByName(c.ipAddress);

						if (inetAddress.isReachable(timeout)) {
							listener.onClientConnectionAlive(c);
							Thread.sleep(1000);
						} else listener.onClientConnectionDead(c);
					} catch (IOException e) {
						Log.e(TAG, "io exception while trying to reach client, ipAddress: " + (c.ipAddress));
					} catch (InterruptedException ire) {
						Log.e(TAG, "InterruptedException: " + ire.getMessage());
					}

					latch.countDown();
				}
			});
		}

		new Thread()
		{
			public void run()
			{
				try {
					latch.await();
				} catch (InterruptedException e) {
					Log.e(TAG, "listing clients countdown interrupted", e);
				}

				listener.onWifiClientsScanComplete();
			}
		}.start();

		return resultList;

	}

	public WifiConfiguration getConfiguration()
	{
		return (WifiConfiguration) invokeSilently(getWifiApConfiguration, mWifiManager);
	}

	public boolean isEnabled()
	{
		Object result = invokeSilently(isWifiApEnabled, mWifiManager);

		if (result == null)
			return false;

		return (Boolean) result;
	}

	public List<HotspotClient> getWifiClients()
	{
		if (!isEnabled())
			return null;

		List<HotspotClient> result = new ArrayList<>();
		Pattern macPattern = Pattern.compile("..:..:..:..:..:..");
		BufferedReader bufferedReader = null;

		try {
			bufferedReader = new BufferedReader(new FileReader("/proc/net/arp"));
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				String[] parts = line.split(" +");
				if (parts.length < 4 || !macPattern.matcher(parts[3]).find())
					continue;

				result.add(new HotspotClient(parts[0]));
			}
		} catch (IOException e) {
			Log.e(TAG, "exception in getting clients", e);
		} finally {
			try {
				if (bufferedReader != null) {
					bufferedReader.close();
				}
			} catch (IOException e) {
				Log.e(TAG, "", e);
			}
		}

		return result;
	}

	public WifiConfiguration getPreviousConfig()
	{
		return mPreviousConfig;
	}

	public WifiManager getWifiManager()
	{
		return mWifiManager;
	}

	private boolean setHotspotConfig(WifiConfiguration config)
	{
		Object result = invokeSilently(setWifiApConfiguration, mWifiManager, config);

		if (result == null)
			return false;

		return (Boolean) result;
	}

	private boolean setHotspotEnabled(WifiConfiguration config, boolean enabled)
	{
		Object result = invokeSilently(setWifiApEnabled, mWifiManager, config, enabled);

		if (result == null)
			return false;

		return (Boolean) result;
	}

	public boolean unloadPreviousConfig()
	{
		if (mPreviousConfig == null)
			return false;

		setHotspotConfig(mPreviousConfig);

		mPreviousConfig = null;

		return true;
	}

	public static class HotspotClient
	{
		public String ipAddress;

		public HotspotClient(String ipAddress)
		{
			this.ipAddress = ipAddress;
		}

		@Override
		public boolean equals(Object object)
		{
			return object == this
					|| (object instanceof HotspotClient && ((HotspotClient) object).ipAddress.equals(this.ipAddress));
		}
	}

	public interface WifiClientConnectionListener
	{
		void onClientConnectionAlive(HotspotClient c);

		void onClientConnectionDead(HotspotClient c);

		void onWifiClientsScanComplete();
	}
}
