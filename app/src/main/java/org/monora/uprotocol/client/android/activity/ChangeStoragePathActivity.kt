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
package org.monora.uprotocol.client.android.activity

import android.content.Intent
import android.os.Parcelable
import android.widget.Toast
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.util.Files

/**
 * Created by: veli
 * Date: 5/30/17 6:57 PM
 */
class ChangeStoragePathActivity : Activity() {
    companion object {
        const val REQUEST_CHOOSE_FOLDER = 1
    }
}