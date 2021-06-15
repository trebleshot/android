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
package org.monora.uprotocol.client.android.config

object AppConfig {
    const val SERVER_PORT_WEBSHARE = 58732

    const val DEFAULT_TIMEOUT_SOCKET = 5000

    const val DEFAULT_TIMEOUT_HOTSPOT = 40000

    const val DELAY_DEFAULT_NOTIFICATION = 1000

    const val BUFFER_LENGTH_DEFAULT = 8096

    const val DELAY_UPDATE_CHECK = 21600

    const val WEB_SHARE_CONNECTION_MAX = 20

    const val ID_GROUP_WEB_SHARE = 10L

    const val EMAIL_DEVELOPER = "trebleshot@monora.org"

    const val URI_API_GITHUB = "https://api.github.com/"

    const val URI_REPO = "repos/trebleshot/android"

    const val URI_REPO_APP_UPDATE = "https://api.github.com/repos/trebleshot/android/releases"

    const val URI_REPO_APP_CONTRIBUTORS = "https://api.github.com/repos/trebleshot/android/contributors"

    const val URI_GOOGLE_PLAY = "https://play.google.com/store/apps/details?id=org.monora.uprotocol.client.android"

    const val URI_ORG_HOME = "https://monora.org"

    const val URI_REPO_APP = "https://github.com/trebleshot"

    const val URI_REPO_ORG = "https://github.com/monoraorg"

    const val URI_GITHUB_PROFILE = "https://github.com/%s"

    const val URI_TRANSLATE = "https://github.com/genonbeta/TrebleShot/wiki/Language-contribution"

    const val URI_TELEGRAM_CHANNEL = "https://t.me/trebleshot"

    const val PREFIX_ACCESS_POINT = "TS_"

    const val EXT_FILE_PART = "tshare"

    const val KEY_GOOGLE_PUBLIC = ("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk1peq7MhNms9ynhnoRtwxnb"
            + "izdEr3TKifUGlUPB3r33WkvPWjwowRvYeuY36+CkBmtjc46Xg/6/jrhPY+L0a+wd58lsNxLUMpo7"
            + "tN2std0TGrsMmmlihb4Bsxcu/6ThsY4CIQx0bdze2v8Zle3e4EoHuXcqQnpwkb+3wMx2rR2E9ih+"
            + "6utqrYAop9NdAbsRZ6BDXDUgJEuiHnRKwDZGDjU5PD4TCiR1jz2YJPYiRuI1QytJM6LirJu6YwE/"
            + "o6pfzSQ3xXlK4yGpGUhzLdTmSNQNIJTWRqZoM7qNgp+0ocmfQRJ32/6E+BxbJaVbHdTINhbVAvLR"
            + "+UFyQ2FldecfuQQIDAQAB")

    val DEFAULT_DISABLED_INTERFACES = arrayOf("rmnet")
}