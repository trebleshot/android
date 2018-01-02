package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.genonbeta.TrebleShot.activity.HomeActivity;
import com.genonbeta.TrebleShot.config.AppConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * created by: Veli
 * date: 30.12.2017 17:08
 */

public class UpdateUtils
{
	public static void sendUpdate(Context context, String toIp) throws IOException
	{
		Socket socket = new Socket();

		socket.bind(null);
		socket.connect(new InetSocketAddress(toIp, AppConfig.COMPATIBLE_UPDATE_CHANNEL_PORT));

		FileInputStream fileInputStream = new FileInputStream(context.getApplicationInfo().sourceDir);
		OutputStream outputStream = socket.getOutputStream();

		int len;
		long lastRead = System.currentTimeMillis();

		while ((len = fileInputStream.read(AppConfig.DEFAULT_BUFFER_SIZE)) != -1) {
			if (len > 0) {
				outputStream.write(AppConfig.DEFAULT_BUFFER_SIZE, 0, len);
				outputStream.flush();

				lastRead = System.currentTimeMillis();
			}

			if ((System.currentTimeMillis() - lastRead) > AppConfig.DEFAULT_SOCKET_TIMEOUT) {
				System.out.println("CoolTransfer: Timed out... Exiting.");
				break;
			}
		}

		outputStream.close();
		fileInputStream.close();

		socket.close();
	}

	public static void receiveUpdate(Context context) throws IOException, PackageManager.NameNotFoundException
	{
		File defaultDirectory = FileUtils.getApplicationDirectory(context);
		File filePath = new File(defaultDirectory.getAbsolutePath() + File.separator + "update_" + System.currentTimeMillis() + ".apk");

		ServerSocket serverSocket = new ServerSocket(AppConfig.COMPATIBLE_UPDATE_CHANNEL_PORT);

		serverSocket.setSoTimeout(AppConfig.DEFAULT_SOCKET_TIMEOUT);

		Socket socket = serverSocket.accept();

		socket.setSoTimeout(AppConfig.DEFAULT_SOCKET_TIMEOUT);

		InputStream inputStream = socket.getInputStream();
		FileOutputStream outputStream = new FileOutputStream(filePath);

		int len = 0;
		long lastRead = System.currentTimeMillis();

		while (len != -1) {
			if ((len = inputStream.read(AppConfig.DEFAULT_BUFFER_SIZE)) > 0) {
				outputStream.write(AppConfig.DEFAULT_BUFFER_SIZE, 0, len);
				outputStream.flush();

				lastRead = System.currentTimeMillis();
			}

			if ((System.currentTimeMillis() - lastRead) > AppConfig.DEFAULT_SOCKET_TIMEOUT) {
				System.out.println("CoolTransfer: Timed out... Exiting.");
				break;
			}
		}

		context.startActivity(new Intent(context, HomeActivity.class)
				.setAction(HomeActivity.ACTION_OPEN_RECEIVED_FILES)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				.putExtra(HomeActivity.EXTRA_FILE_PATH, filePath.getParent()));

		outputStream.close();
		inputStream.close();

		socket.close();
		serverSocket.close();
	}
}
