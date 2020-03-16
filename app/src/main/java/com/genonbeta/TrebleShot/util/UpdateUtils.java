/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.util.Stoppable;
import com.genonbeta.android.updatewithgithub.GitHubUpdater;

import java.io.FileInputStream;
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
    public static void checkForUpdates(final Context context, GitHubUpdater updater, boolean popupDialog,
                                       final GitHubUpdater.OnInfoAvailableListener listener)
    {
        updater.checkForUpdates(popupDialog, (newVersion, versionName, title, description, releaseDate) -> {
            SharedPreferences sharedPreferences = AppUtils.getDefaultPreferences(context);

            sharedPreferences.edit()
                    .putString("availableVersion", versionName)
                    .putLong("checkedForUpdatesTime", System.currentTimeMillis())
                    .apply();

            if (listener != null)
                listener.onInfoAvailable(newVersion, versionName, title, description, releaseDate);
        });
    }

    public static String getAvailableVersion(Context context)
    {
        return AppUtils.getDefaultPreferences(context).getString("availableVersion", null);
    }

    public static GitHubUpdater getDefaultUpdater(Context context)
    {
        return new GitHubUpdater(context, AppConfig.URI_REPO_APP_UPDATE, R.style.Theme_TrebleShot, false);
    }

    public static long getLastTimeCheckedForUpdates(Context context)
    {
        return AppUtils.getDefaultPreferences(context).getLong("checkedForUpdatesTime", 0);
    }

    public static boolean hasNewVersion(Context context)
    {
        String availableVersion = getAvailableVersion(context);
        return availableVersion != null && GitHubUpdater.isNewVersion(context, availableVersion);
    }

    public static void sendUpdate(Context context, String toIp) throws IOException
    {
        Socket socket = new Socket();

        socket.bind(null);
        socket.connect(new InetSocketAddress(toIp, AppConfig.SERVER_PORT_UPDATE_CHANNEL));

        FileInputStream fileInputStream = new FileInputStream(context.getApplicationInfo().sourceDir);
        OutputStream outputStream = socket.getOutputStream();

        byte[] buffer = new byte[AppConfig.BUFFER_LENGTH_DEFAULT];
        int len;
        long lastRead = System.currentTimeMillis();

        while ((len = fileInputStream.read(buffer)) != -1) {
            if (len > 0) {
                outputStream.write(buffer, 0, len);
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

    public static DocumentFile receiveUpdate(Context context, NetworkDevice device, Stoppable stoppable,
                                             OnConnectionReadyListener readyListener) throws IOException
    {
        ServerSocket serverSocket = null;
        Socket socket = null;
        DocumentFile updateFile = null;

        try {
            serverSocket = new ServerSocket(AppConfig.SERVER_PORT_UPDATE_CHANNEL);
            updateFile = FileUtils.getApplicationDirectory(context)
                    .createFile(null, device.versionName + "_" + System.currentTimeMillis() + ".apk");

            final ServerSocket finalServer = serverSocket;

            stoppable.addCloser(userAction -> {
                try {
                    if (!finalServer.isClosed())
                        finalServer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            if (readyListener != null)
                readyListener.onConnectionReady(serverSocket);

            serverSocket.setSoTimeout(AppConfig.DEFAULT_SOCKET_TIMEOUT);

            socket = serverSocket.accept();

            socket.setSoTimeout(AppConfig.DEFAULT_SOCKET_TIMEOUT);

            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = context.getContentResolver().openOutputStream(updateFile.getUri());

            if (outputStream == null)
                throw new IOException("Could not open the output stream to write to.");

            byte[] buffer = new byte[AppConfig.BUFFER_LENGTH_DEFAULT];
            int len = 0;
            long lastRead = 0;

            while (len != -1) {
                long current = System.nanoTime();

                if ((len = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                    outputStream.flush();

                    lastRead = current;
                }

                if ((current - lastRead) > AppConfig.DEFAULT_SOCKET_TIMEOUT * 1e6 || stoppable.isInterrupted())
                    throw new Exception("Timed out or interrupted. Exiting!");
            }

            outputStream.close();
            inputStream.close();

        } catch (Exception e) {
            e.printStackTrace();

            if (updateFile != null && updateFile.isFile())
                updateFile.delete();

            return null;
        } finally {
            if (socket != null && !socket.isClosed())
                socket.close();

            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
        }

        return updateFile;
    }

    public interface OnConnectionReadyListener
    {
        void onConnectionReady(ServerSocket socket);
    }
}
