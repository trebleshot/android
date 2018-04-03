package com.genonbeta.TrebleShot.util;

import android.content.Context;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransactionObject;

import java.util.HashMap;

/**
 * created by: Veli
 * date: 12.11.2017 11:14
 */

public class TextUtils
{
	public static int getAdapterName(NetworkDevice.Connection connection)
	{
		HashMap<String, Integer> associatedNames = new HashMap<>();

		associatedNames.put("wlan", R.string.text_interfaceWireless);
		associatedNames.put("p2p", R.string.text_interfaceWifiDirect);
		associatedNames.put("bt-pan", R.string.text_interfaceBluetooth);
		associatedNames.put("eth", R.string.text_interfaceEthernet);
		associatedNames.put("unk", R.string.text_interfaceUnknown);

		for (String displayName : associatedNames.keySet())
			if (connection.adapterName.startsWith(displayName))
				return associatedNames.get(displayName);

		return -1;
	}

	public static String getAdapterName(Context context, NetworkDevice.Connection connection)
	{
		int adapterNameResource = getAdapterName(connection);

		if (adapterNameResource == -1)
			return connection.adapterName;

		return context.getString(adapterNameResource);
	}

	public static String getLetters(String text, int breakAfter)
	{
		if (text == null || text.length() == 0)
			text = "?";

		StringBuilder stringBuilder = new StringBuilder();

		for (String letter : text.split(" ")) {
			if (stringBuilder.length() > breakAfter)
				break;

			if (letter.length() == 0)
				continue;

			stringBuilder.append(letter.charAt(0));
		}

		return stringBuilder.toString().toUpperCase();
	}

	public static int getTransactionFlagString(TransactionObject.Flag flag)
	{
		switch (flag) {
			case PENDING:
				return R.string.text_flagPending;
			case INTERRUPTED:
				return R.string.text_flagInterrupted;
				/*
			case RUNNING:
				return R.string.text_flagRunning;
			case RESUME:
				return R.string.text_flagResume; */
			case REMOVED:
				return R.string.text_flagRemoved;
			default:
				return R.string.text_unknown;
		}
	}

	public static boolean searchWord(String word, String searchThis)
	{
		return searchThis == null
				|| searchThis.length() == 0
				|| word.toLowerCase().contains(searchThis.toLowerCase());
	}

	public static String trimText(String text, int length)
	{
		if (text == null || text.length() <= length)
			return text;

		return text.substring(0, length);
	}
}
