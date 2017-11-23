package com.genonbeta.TrebleShot.util;

import com.genonbeta.TrebleShot.R;

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
		associatedNames.put("unk", R.string.text_interfacUnknown);

		for (String displayName : associatedNames.keySet())
			if (connection.adapterName.startsWith(displayName))
				return associatedNames.get(displayName);

		return -1;
	}

	public static String getFirstLetters(String text, int breakAfter)
	{
		StringBuilder stringBuilder = new StringBuilder();

		for (String letter : text.split(" ")) {
			if (stringBuilder.length() > breakAfter)
				break;

			stringBuilder.append(letter.charAt(0));
		}

		return stringBuilder.toString().toUpperCase();
	}

	public static int getTransactionFlagString(TransactionObject.Flag flag)
	{
		switch (flag) {
			case PENDING:
				return R.string.text_flagPending;
			case RUNNING:
				return R.string.text_flagRunning;
			case INTERRUPTED:
				return R.string.text_flagInterrupted;
			case RESUME:
				return R.string.text_flagResume;
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
}
