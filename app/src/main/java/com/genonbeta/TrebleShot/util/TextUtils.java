package com.genonbeta.TrebleShot.util;

import android.content.Context;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferObject;

import java.text.NumberFormat;
import java.util.HashMap;

/**
 * created by: Veli
 * date: 12.11.2017 11:14
 */

public class TextUtils
{
    public static int getAdapterName(String adapterName)
    {
        HashMap<String, Integer> associatedNames = new HashMap<>();

        associatedNames.put("wlan", R.string.text_interfaceWireless);
        associatedNames.put("p2p", R.string.text_interfaceWifiDirect);
        associatedNames.put("bt-pan", R.string.text_interfaceBluetooth);
        associatedNames.put("eth", R.string.text_interfaceEthernet);
        associatedNames.put("tun", R.string.text_interfaceVPN);
        associatedNames.put("unk", R.string.text_interfaceUnknown);

        for (String displayName : associatedNames.keySet())
            if (adapterName.startsWith(displayName))
                return associatedNames.get(displayName);

        return -1;
    }

    public static String getAdapterName(Context context, NetworkDevice.Connection connection)
    {
        return getAdapterName(context, connection.adapterName);
    }

    public static String getAdapterName(Context context, AddressedInterface addressedInterface)
    {
        return getAdapterName(context, addressedInterface.getNetworkInterface().getDisplayName());
    }

    public static String getAdapterName(Context context, String adapterName)
    {
        int adapterNameResource = getAdapterName(adapterName);

        if (adapterNameResource == -1)
            return adapterName;

        return context.getString(adapterNameResource);
    }

    public static String getLetters(String text, int length)
    {
        if (text == null || text.length() == 0)
            text = "?";

        int breakAfter = --length;
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

    public static String getTransactionFlagString(Context context, TransferObject transferObject, NumberFormat percentFormat)
    {
        switch (transferObject.flag) {
            case DONE:
                return percentFormat.format(1.0);
            case IN_PROGRESS:
                return percentFormat.format(transferObject.fileSize == 0 || transferObject.flag.getBytesValue() == 0
                        ? 0
                        : Long.valueOf(transferObject.flag.getBytesValue()).doubleValue() / Long.valueOf(transferObject.fileSize).doubleValue());
            default:
                return context.getString(getTransactionFlagString(transferObject.flag));
        }
    }

    public static int getTransactionFlagString(TransferObject.Flag flag)
    {
        switch (flag) {
            case PENDING:
                return R.string.text_flagPending;
            case DONE:
                return R.string.text_taskCompleted;
            case INTERRUPTED:
                return R.string.text_flagInterrupted;
            case IN_PROGRESS:
                return R.string.text_flagRunning;
				/*
			case RESUME:
				return R.string.text_flagResume; */
            case REMOVED:
                return R.string.text_flagRemoved;
            default:
                return R.string.text_unknown;
        }
    }

    public static String makeWebShareLink(Context context, String address)
    {
        return context.getString(R.string.mode_webShareAddress, address, AppConfig
                .SERVER_PORT_WEBSHARE);
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
