package com.genonbeta.TrebleShot.config;

public class AppConfig
{
	public final static int
			COMMUNICATION_SERVER_PORT = 1128,
			SEAMLESS_SERVER_PORT = 58762,
			COMPATIBLE_UPDATE_CHANNEL_PORT = 58765,
			DEFAULT_SOCKET_TIMEOUT = 5000,
			DEFAULT_SOCKET_LARGE_TIMEOUT = 40000,
			SUPPORTED_MIN_VERSION = 55,
			NICKNAME_MAX_LENGHT = 14;

	public final static byte[]
			DEFAULT_BUFFER_SIZE = new byte[8096],
			SMALL_BUFFER_SIZE = new byte[1024];

	public final static String
			APP_UPDATE_REPO = "https://api.github.com/repos/genonbeta/TrebleShot/releases",
			APPLICATION_REPO = "http://github.com/genonbeta/TrebleShot",
			ACCESS_POINT_PREFIX = "TS_",
			NETWORK_INTERFACE_WIFI = "wlan0",
			NDS_COMM_SERVICE_NAME = "TSComm",
			NDS_COMM_SERVICE_TYPE = "_tscomm._tcp.";

	public final static String[] DEFAULT_DISABLED_INTERFACES = new String[]{"rmnet"};

}
