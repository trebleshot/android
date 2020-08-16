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

package com.genonbeta.TrebleShot.config;

public class AppConfig
{
    public final static int
            SERVER_PORT_COMMUNICATION = 1128,
            SERVER_PORT_WEBSHARE = 58732,
            SERVER_PORT_UPDATE_CHANNEL = 58765,
            DEFAULT_SOCKET_TIMEOUT = 5000, // ms
            DEFAULT_SOCKET_TIMEOUT_LARGE = 20000, // ms
            DELAY_DEFAULT_NOTIFICATION = 1000, // ms
            NICKNAME_LENGTH_MAX = 32,
            BUFFER_LENGTH_DEFAULT = 8096,
            DELAY_CHECK_FOR_UPDATES = 21600,
            PHOTO_SCALE_FACTOR = 100,
            WEB_SHARE_CONNECTION_MAX = 20,
            ID_GROUP_WEB_SHARE = 10;

    public final static String
            EMAIL_DEVELOPER = "genonbeta@gmail.com",
            URI_REPO_APP_UPDATE = "https://api.github.com/repos/genonbeta/TrebleShot/releases",
            URI_REPO_APP_CONTRIBUTORS = "https://api.github.com/repos/genonbeta/TrebleShot/contributors",
            URI_GOOGLE_PLAY = "https://play.google.com/store/apps/details?id=com.genonbeta.TrebleShot",
            URI_REPO_APP = "http://github.com/genonbeta/TrebleShot",
            URI_REPO_ORG = "http://github.com/genonbeta",
            URI_GITHUB_PROFILE = "https://github.com/%s",
            URI_TRANSLATE = "https://github.com/genonbeta/TrebleShot/wiki/Language-contribution",
            URI_TELEGRAM_CHANNEL = "https://t.me/trebleshot",
            PREFIX_ACCESS_POINT = "TS_",
            EXT_FILE_PART = "tshare",
            NSD_SERVICE_TYPE = "_tscomm._tcp.",
            KEY_GOOGLE_PUBLIC = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk1peq7MhNms9ynhnoRtwxnb"
                    + "izdEr3TKifUGlUPB3r33WkvPWjwowRvYeuY36+CkBmtjc46Xg/6/jrhPY+L0a+wd58lsNxLUMpo7"
                    + "tN2std0TGrsMmmlihb4Bsxcu/6ThsY4CIQx0bdze2v8Zle3e4EoHuXcqQnpwkb+3wMx2rR2E9ih+"
                    + "6utqrYAop9NdAbsRZ6BDXDUgJEuiHnRKwDZGDjU5PD4TCiR1jz2YJPYiRuI1QytJM6LirJu6YwE/"
                    + "o6pfzSQ3xXlK4yGpGUhzLdTmSNQNIJTWRqZoM7qNgp+0ocmfQRJ32/6E+BxbJaVbHdTINhbVAvLR"
                    + "+UFyQ2FldecfuQQIDAQAB";

    public final static String[] DEFAULT_DISABLED_INTERFACES = new String[]{"rmnet"};

}
