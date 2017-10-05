package com.genonbeta.core.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class NetworkUtils
{
	public static String bytesToHex(byte[] bytes)
	{
		StringBuilder sbuf = new StringBuilder();

		for (int idx = 0; idx < bytes.length; idx++)
		{
			int intVal = bytes[idx] & 0xff;

			if (intVal < 0x10)
				sbuf.append("0");

			sbuf.append(Integer.toHexString(intVal).toUpperCase());
		}
		return sbuf.toString();
	}

	public static byte[] getUTF8Bytes(String str)
	{
		try
		{
			return str.getBytes("UTF-8");
		} catch (Exception ex)
		{
			return null;
		}
	}

	public static String loadFileAsString(String filename) throws IOException
	{
		final int BUFLEN = 1024;

		BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename), BUFLEN);

		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
			byte[] bytes = new byte[BUFLEN];
			boolean isUTF8 = false;
			int read, count = 0;

			while ((read = is.read(bytes)) != -1)
			{
				if (count == 0 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF)
				{
					isUTF8 = true;
					baos.write(bytes, 3, read - 3); // drop UTF8 bom marker
				} else
				{
					baos.write(bytes, 0, read);
				}

				count += read;
			}
			return isUTF8 ? new String(baos.toByteArray(), "UTF-8") : new String(baos.toByteArray());
		} finally
		{
			try
			{
				is.close();
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public static ArrayList<String> getMACAddressList(String interfaceName)
	{
		ArrayList<String> macAddressList = new ArrayList<String>();

		try
		{
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

			for (NetworkInterface intf : interfaces)
			{
				if (interfaceName != null)
				{
					if (!intf.getName().equalsIgnoreCase(interfaceName))
						continue;
				}

				byte[] mac = intf.getHardwareAddress();

				if (mac == null)
					continue;

				StringBuilder buf = new StringBuilder();

				for (int idx = 0; idx < mac.length; idx++)
					buf.append(String.format("%02X:", mac[idx]));

				if (buf.length() > 0)
					buf.deleteCharAt(buf.length() - 1);

				macAddressList.add(buf.toString());
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		return macAddressList;
	}

	public static HashMap<NetworkInterface, String> getInterfaces(boolean useIPv4, String[] avoidedInterfaces)
	{
		HashMap<NetworkInterface, String> ipAddressList = new HashMap<NetworkInterface, String>();

		try
		{
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

			for (NetworkInterface intf : interfaces)
			{
				boolean breaker = false;

				for (String match : avoidedInterfaces)
				{
					if (intf.getDisplayName().contains(match))
						breaker = true;
				}

				if (breaker)
					continue;

				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());

				for (InetAddress addr : addrs)
				{
					if (!addr.isLoopbackAddress())
					{
						String sAddr = addr.getHostAddress().toUpperCase();
						boolean isIPv4 = addr instanceof Inet4Address;

						if (useIPv4 && isIPv4)
						{
							ipAddressList.put(intf, sAddr);
						} else if (!useIPv4)
						{
							int delim = sAddr.indexOf('%'); // drop ip6 port suffix
							ipAddressList.put(intf, (delim < 0 ? sAddr : sAddr.substring(0, delim)));
						}
					}
				}
			}
		} catch (Exception e)
		{
		}

		return ipAddressList;
	}

	public static ArrayList<String> getInterfacesWithOnlyIp(boolean useIPv4, String[] avoidedInterfaces)
	{
		ArrayList<String> list = new ArrayList<String>();

		list.addAll(getInterfaces(useIPv4, avoidedInterfaces).values());

		return list;
	}

	public static String getAddressPrefix(String ipv4Address)
	{
		return ipv4Address.substring(0, ipv4Address.lastIndexOf(".") + 1);
	}

	public static boolean testSocket(String ip, int port)
	{
		InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
		Socket socket = new Socket();

		try
		{
			socket.bind(null);
			socket.connect(socketAddress);
			socket.close();

			return true;
		} catch (IOException e)
		{
			return false;
		}
	}
}
