package com.genonbeta.TrebleShot.config;

public class AppConfig
{
    public final static int
            SERVER_PORT_COMMUNICATION = 1128,
            SERVER_PORT_SEAMLESS = 58762,
            SERVER_PORT_WEBSHARE = 58732,
            SERVER_PORT_UPDATE_CHANNEL = 58765,
            DEFAULT_SOCKET_TIMEOUT = 5000,
            DEFAULT_SOCKET_TIMEOUT_LARGE = 40000,
            DEFAULT_NOTIFICATION_DELAY = 2000,
            SUPPORTED_MIN_VERSION = 62,
            NICKNAME_LENGTH_MAX = 32,
            BUFFER_LENGTH_DEFAULT = 8096,
            BUFFER_LENGTH_SMALL = 1024,
            DELAY_CHECK_FOR_UPDATES = 21600,
            PHOTO_SCALE_FACTOR = 100,
            WEB_SHARE_CONNECTION_MAX = 20;

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
            NETWORK_INTERFACE_WIFI = "wlan0",
            NDS_COMM_SERVICE_NAME = "TSComm",
            NDS_COMM_SERVICE_TYPE = "_tscomm._tcp.",
            KEY_GOOGLE_PUBLIC = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk1peq7MhNms9ynhnoRtwxnb"
                    + "izdEr3TKifUGlUPB3r33WkvPWjwowRvYeuY36+CkBmtjc46Xg/6/jrhPY+L0a+wd58lsNxLUMpo7"
                    + "tN2std0TGrsMmmlihb4Bsxcu/6ThsY4CIQx0bdze2v8Zle3e4EoHuXcqQnpwkb+3wMx2rR2E9ih+"
                    + "6utqrYAop9NdAbsRZ6BDXDUgJEuiHnRKwDZGDjU5PD4TCiR1jz2YJPYiRuI1QytJM6LirJu6YwE/"
                    + "o6pfzSQ3xXlK4yGpGUhzLdTmSNQNIJTWRqZoM7qNgp+0ocmfQRJ32/6E+BxbJaVbHdTINhbVAvLR"
                    + "+UFyQ2FldecfuQQIDAQAB";

    public final static String[] DEFAULT_DISABLED_INTERFACES = new String[]{"rmnet"};

}
