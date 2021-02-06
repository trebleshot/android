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
package com.genonbeta.TrebleShot.util

import com.genonbeta.TrebleShot.service.backgroundserviceimport.TaskStoppedException
import com.genonbeta.android.framework.util.Stoppable

/**
 * created by: Veli
 * date: 11.02.2018 19:37
 */
abstract class StoppableJob {
    @Throws(TaskStoppedException::class)
    protected abstract fun onRun()

    @Throws(TaskStoppedException::class)
    protected fun run(stoppable: Stoppable) {
        try {
            onRun()
        } finally {
            stoppable.removeClosers()
        }
    }
}