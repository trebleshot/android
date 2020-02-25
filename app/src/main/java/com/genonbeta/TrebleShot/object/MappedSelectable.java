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

package com.genonbeta.TrebleShot.object;

import androidx.annotation.Nullable;
import com.genonbeta.android.framework.object.Selectable;
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection;
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection;
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine;

import java.util.ArrayList;
import java.util.List;

public class MappedSelectable<T extends Selectable> implements Selectable
{
    public IEngineConnection<T> engineConnection;
    public T selectable;

    public MappedSelectable(T selectable, IEngineConnection<T> engineConnection)
    {
        this.selectable = selectable;
        this.engineConnection = engineConnection;
    }

    private static <T extends Selectable> void addToMappedObjectList(List<MappedSelectable<?>> list,
                                                                     IEngineConnection<T> connection)
    {
        for (T selectable : connection.getSelectedItemList())
            list.add(new MappedSelectable<>(selectable, connection));
    }

    public static List<MappedSelectable<?>> compileFrom(@Nullable IPerformerEngine engine)
    {
        List<MappedSelectable<?>> list = new ArrayList<>();

        if (engine != null)
            for (IBaseEngineConnection baseEngineConnection : engine.getConnectionList())
                if (baseEngineConnection instanceof IEngineConnection<?>)
                    addToMappedObjectList(list, (IEngineConnection<?>) baseEngineConnection);

        return list;
    }

    @Override
    public String getSelectableTitle()
    {
        return selectable.getSelectableTitle();
    }

    @Override
    public boolean isSelectableSelected()
    {
        return selectable.isSelectableSelected();
    }

    @Override
    public boolean setSelectableSelected(boolean selected)
    {
        return engineConnection.setSelected(selectable, selected);
    }
}