package com.genonbeta.TrebleShot.helper;

import android.support.v4.util.ArrayMap;

import java.io.File;
import java.net.Socket;

public class AwaitedFileSender
{
	public String ip;
	public int port;
	public int requestId;
	public boolean isCancelled = false;
	public File file;
	
	public AwaitedFileSender(String ip, File file, int requestId)
	{
		this.ip = ip;
		this.file = file;
		this.requestId = requestId;
	}
	
	public void setPort(int port)
	{
		this.port = port;
	}
}
