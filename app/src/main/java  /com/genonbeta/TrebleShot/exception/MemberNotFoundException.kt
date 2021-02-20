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
package com.genonbeta.TrebleShot.exception

import com.genonbeta.TrebleShot.model.TransferMember
import com.genonbeta.android.database.exception.ReconstructionFailedException

/**
 * created by: veli
 * date: 06.04.2018 11:20
 */
class MemberNotFoundException(val member: TransferMember) : ReconstructionFailedException(
    "Member with deviceId=${member.deviceId} and transferId=${member.transferId} is not valid"
)