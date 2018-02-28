package com.genonbeta.TrebleShot.config;

public class AppConfig
{
	public final static int
			SERVER_PORT_COMMUNICATION = 1128,
			SERVER_PORT_SEAMLESS = 58762,
			SERVER_PORT_UPDATE_CHANNEL = 58765,
			DEFAULT_SOCKET_TIMEOUT = 5000,
			DEFAULT_SOCKET_TIMEOUT_LARGE = 40000,
			DEFAULT_NOTIFICATION_DELAY = 2000,
			SUPPORTED_MIN_VERSION = 62,
			NICKNAME_LENGTH_MAX = 32,
			BUFFER_LENGTH_DEFAULT = 8096,
			BUFFER_LENGTH_SMALL = 1024;

	public final static String
			REPO_APP_UPDATE = "https://api.github.com/repos/genonbeta/TrebleShot/releases",
			REPO_APP = "http://github.com/genonbeta/TrebleShot",
			PREFIX_ACCESS_POINT = "TS_",
			NETWORK_INTERFACE_WIFI = "wlan0",
			NDS_COMM_SERVICE_NAME = "TSComm",
			NDS_COMM_SERVICE_TYPE = "_tscomm._tcp.";

	public final static String[] DEFAULT_DISABLED_INTERFACES = new String[]{"rmnet"};

}
