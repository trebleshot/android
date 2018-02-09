package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.pm.PackageManager;

import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.object.NetworkDevice;

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

	public static File receiveUpdate(Context context, NetworkDevice device, Interrupter interrupter, OnConnectionReadyListener readyListener) throws IOException, PackageManager.NameNotFoundException
	{
		ServerSocket serverSocket = null;
		Socket socket = null;
		File filePath;

		try {
			serverSocket = new ServerSocket(AppConfig.COMPATIBLE_UPDATE_CHANNEL_PORT);
			filePath = new File(FileUtils.getApplicationDirectory(context).getAbsolutePath() + File.separator + device.versionName + "_" + System.currentTimeMillis() + ".apk");

			final ServerSocket finalServer = serverSocket;

			interrupter.addCloser(new Interrupter.Closer()
			{
				@Override
				public void onClose()
				{
					try {
						if (!finalServer.isClosed())
							finalServer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});

			if (readyListener != null)
				readyListener.onConnectionReady(serverSocket);

			serverSocket.setSoTimeout(AppConfig.DEFAULT_SOCKET_TIMEOUT);

			socket = serverSocket.accept();

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

				if ((System.currentTimeMillis() - lastRead) > AppConfig.DEFAULT_SOCKET_TIMEOUT || interrupter.interrupted())
					throw new Exception("Timed out or interrupted. Exiting!");
			}

			outputStream.close();
			inputStream.close();
		} catch (Exception e) {
			return null;
		} finally {
			if (socket != null && !socket.isClosed())
				socket.close();

			if (serverSocket != null && !serverSocket.isClosed())
				serverSocket.close();
		}

		return filePath;
	}

	public interface OnConnectionReadyListener
	{
		void onConnectionReady(ServerSocket socket);
	}
}
