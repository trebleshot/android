package com.genonbeta.TrebleShot.config;

public class AppConfig
{
	public final static int COMMUNICATION_SERVER_PORT = 1128;
	public final static byte[] DEFAULT_BUFFER_SIZE = new byte[8096];
	public final static byte[] SMALL_BUFFER_SIZE = new byte[1024];
	public final static int DEFAULT_SOCKET_TIMEOUT = 5000;
	public final static int DEFAULT_SOCKET_LARGE_TIMEOUT = 40000;
	public final static String[] DEFAULT_DISABLED_INTERFACES = new String[]{"rmnet"};
	public final static String APP_UPDATE_REPO = "https://api.github.com/repos/genonbeta/TrebleShot/releases";
	public final static String APPLICATION_REPO = "http://github.com/genonbeta/TrebleShot";
}
