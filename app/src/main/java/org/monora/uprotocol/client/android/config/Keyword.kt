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

/**
 * Created by: veli
 * Date: 4/28/17 8:29 PM
 */
object Keyword {
    const val RESULT = "result"
    const val REQUEST = "request"
    const val REQUEST_TRANSFER = "transfer"
    const val REQUEST_NOTIFY_TRANSFER_STATE = "transferState"
    const val REQUEST_ACQUAINTANCE = "acquaintance"
    const val REQUEST_CLIPBOARD = "text"
    const val REQUEST_TRANSFER_JOB = "transferJob"

    // Introduced in 99
    const val TRANSFER_TYPE = "type"

    // Introduced in 99
    const val TRANSFER_REQUEST_ID = "requestId"
    const val TRANSFER_ID = "transferId"
    const val TRANSFER_IS_ACCEPTED = "isAccepted"
    const val TRANSFER_TEXT = "text"
    const val INDEX = "index"
    const val INDEX_FILE_NAME = "name"
    const val INDEX_FILE_SIZE = "size"
    const val INDEX_FILE_MIME = "mime"
    const val INDEX_DIRECTORY = "directory"
    const val DEVICE_UID = "deviceId"
    const val DEVICE_BRAND = "brand"
    const val DEVICE_MODEL = "model"
    const val DEVICE_USERNAME = "user"
    const val DEVICE_KEY = "key"

    // Introduced in 99
    const val DEVICE_AVATAR = "avatar"
    const val DEVICE_PIN = "pin"

    // Introduced in 99
    const val DEVICE_VERSION_NAME = "versionName"
    const val DEVICE_VERSION_CODE = "versionCode"
    const val DEVICE_PROTOCOL_VERSION = "protocolVersion"

    // Introduced in 99
    const val DEVICE_PROTOCOL_VERSION_MIN = "minimumProtocolVersion"

    // Introduced in 99
    const val SKIPPED_BYTES = "skippedBytes"
    const val ERROR = "error"
    const val ERROR_NOT_ALLOWED = "notAllowed"
    const val ERROR_NOT_FOUND = "notFound"
    const val ERROR_UNKNOWN = "unknown"
    const val ERROR_NOT_ACCESSIBLE = "notAccessible"
    const val ERROR_NOT_TRUSTED = "notTrusted"
    const val ERROR_ALREADY_EXISTS = "alreadyExists"
    const val NETWORK_PIN = "networkPin"
    const val QR_CODE_TYPE_HOTSPOT = "hs"
    const val QR_CODE_TYPE_WIFI = "wf"

    enum class Flavor {
        unknown, fossReliant, googlePlay
    }

    object Local {
        const val FILENAME_UNHANDLED_CRASH_LOG = "unhandled_crash_log.txt"
        const val SETTINGS_VIEWING = "sorting_settings"
    }
}