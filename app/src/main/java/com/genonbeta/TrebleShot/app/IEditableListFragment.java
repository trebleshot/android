/*
 * Copyright (C) 2020 Veli Tasalı
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

package com.genonbeta.TrebleShot.app;

import com.genonbeta.TrebleShot.dataobject.Editable;
import com.genonbeta.TrebleShot.view.EditableListFragmentViewBase;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

public interface IEditableListFragment<T extends Editable, V extends RecyclerViewAdapter.ViewHolder>
        extends EditableListFragmentBase<T>, EditableListFragmentViewBase<V>
{
    boolean performLayoutClickOpen(V holder);

    boolean performLayoutClickOpen(V holder, T object);

    boolean performDefaultLayoutClick(V holder, T object);

    boolean performDefaultLayoutLongClick(V holder, T object);
}
