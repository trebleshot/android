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

/**
 * Created by: veli
 * Date: 4/28/17 8:29 PM
 */

public class Keyword
{
    public static final String
            RESULT = "result",
            REQUEST = "request",
            REQUEST_TRANSFER = "transfer",
            REQUEST_NOTIFY_TRANSFER_STATE = "transferState",
            REQUEST_ACQUAINTANCE = "acquaintance",
            REQUEST_CLIPBOARD = "text",
            REQUEST_TRANSFER_JOB = "transferJob", // Introduced in 99
            TRANSFER_TYPE = "type", // Introduced in 99
            TRANSFER_REQUEST_ID = "requestId",
            TRANSFER_ID = "transferId",
            TRANSFER_IS_ACCEPTED = "isAccepted",
            TRANSFER_TEXT = "text",
            INDEX = "index",
            INDEX_FILE_NAME = "name",
            INDEX_FILE_SIZE = "size",
            INDEX_FILE_MIME = "mime",
            INDEX_DIRECTORY = "directory",
            DEVICE_UID = "deviceId",
            DEVICE_BRAND = "brand",
            DEVICE_MODEL = "model",
            DEVICE_USERNAME = "user",
            DEVICE_KEY = "key", // Introduced in 99
            DEVICE_AVATAR = "avatar",
            DEVICE_PIN = "pin", // Introduced in 99
            DEVICE_VERSION_NAME = "versionName",
            DEVICE_VERSION_CODE = "versionCode",
            DEVICE_PROTOCOL_VERSION = "protocolVersion", // Introduced in 99
            DEVICE_PROTOCOL_VERSION_MIN = "minimumProtocolVersion", // Introduced in 99
            SKIPPED_BYTES = "skippedBytes",
            ERROR = "error",
            ERROR_NOT_ALLOWED = "notAllowed",
            ERROR_NOT_FOUND = "notFound",
            ERROR_UNKNOWN = "unknown",
            ERROR_NOT_ACCESSIBLE = "notAccessible",
            ERROR_NOT_TRUSTED = "notTrusted",
            ERROR_ALREADY_EXISTS = "alreadyExists",
            NETWORK_SSID = "nwName",
            NETWORK_PIN = "pin",
            NETWORK_PASSWORD = "nwPwd",
            NETWORK_KEYMGMT = "ntKeyMgmt",
            NETWORK_BSSID = "bsid",
            NETWORK_ADDRESS_IP = "ipAdr";

    public enum Flavor
    {
        unknown,
        fossReliant,
        googlePlay
    }

    public static class Local
    {
        public static final String
                FILENAME_UNHANDLED_CRASH_LOG = "unhandled_crash_log.txt",
                SETTINGS_VIEWING = "sorting_settings";
    }
}
