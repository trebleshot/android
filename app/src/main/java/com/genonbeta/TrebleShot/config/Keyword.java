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
 * Created by: veli
 * Date: 4/28/17 8:29 PM
 */

public class Keyword
{
    public static final String
            REQUEST = "request",
            RESULT = "result",
            REQUEST_TRANSFER = "requestTransfer",
            REQUEST_RESPONSE = "requestResponse",
            REQUEST_ACQUAINTANCE = "requestAcquaintance",
            REQUEST_CLIPBOARD = "requestClipboard",
            REQUEST_HANDSHAKE = "requestHandshake",
            REQUEST_START_TRANSFER = "requestStartTransfer",
            BACK_COMP_REQUEST_SEND_UPDATE = "backCompRequestSendUpdate",
            TRANSFER_REQUEST_ID = "requestId",
            TRANSFER_GROUP_ID = "groupId",
            TRANSFER_DEVICE_ID = "deviceId", // Introduced in 91
            TRANSFER_SOCKET_PORT = "socketPort",
            TRANSFER_IS_ACCEPTED = "isAccepted",
            TRANSFER_CLIPBOARD_TEXT = "clipboardText",
            TRANSFER_JOB_DONE = "jobDone", // any exit situation will be referred by this
            FLAG = "flag",
            FLAG_GROUP_EXISTS = "flagGroupExists",
            FILES_INDEX = "filesIndex",
            INDEX_FILE_NAME = "file",
            INDEX_FILE_SIZE = "fileSize",
            INDEX_FILE_MIME = "fileMime",
            INDEX_DIRECTORY = "directory",
            DEVICE_INFO = "deviceInfo",
            DEVICE_INFO_BRAND = "brand",
            DEVICE_INFO_MODEL = "model",
            DEVICE_INFO_USER = "user",
            DEVICE_INFO_SERIAL = "deviceId",
            DEVICE_INFO_PICTURE = "devicePicture",
            DEVICE_SECURE_KEY = "secureKey",
            APP_INFO = "appInfo",
            APP_INFO_VERSION_NAME = "versionName",
            APP_INFO_VERSION_CODE = "versionCode",
            SKIPPED_BYTES = "skippedBytes",
            SIZE_CHANGED = "sizeChanged",
            ERROR = "error",
            ERROR_NOT_ALLOWED = "notAllowed",
            ERROR_NOT_FOUND = "notFound",
            ERROR_UNKNOWN = "errorUnknown",
            ERROR_NOT_ACCESSIBLE = "notAccessible",
            ERROR_REQUIRE_TRUSTZONE = "errorRequireTrustZone",
            HANDSHAKE_REQUIRED = "handshakeRequired",
            HANDSHAKE_ONLY = "handshakeOnly",
            NETWORK_NAME = "nwName",
            NETWORK_PIN = "pin",
            NETWORK_PASSWORD = "nwPwd",
            NETWORK_KEYMGMT = "ntKeyMgmt",
            NETWORK_ADDRESS_BSSID = "bsid",
            NETWORK_ADDRESS_IP = "ipAdr",
            FLAG_TRANSFER_QR_CONNECTION = "flagTransferQRConnection";

    public enum Flavor
    {
        unknown,
        fossReliant,
        googlePlay
    }

    public static class Local
    {
        public static final String
                NETWORK_INTERFACE_UNKNOWN = "unk0",
                SETTINGS_VIEWING = "sorting_settings";
    }
}
