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

package com.genonbeta.TrebleShot.app;

import android.net.Uri;
import androidx.recyclerview.widget.RecyclerView;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.ui.callback.TitleProvider;
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase;
import com.genonbeta.android.framework.app.ListFragmentBase;
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection;
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider;

/**
 * created by: veli
 * date: 14/04/18 10:35
 */
public interface EditableListFragmentBase<T extends Editable> extends ListFragmentBase<T>, PerformerEngineProvider,
        IEngineConnection.SelectionListener<T>, TitleProvider
{
    void applyViewingChanges(int gridSize);

    void changeGridViewSize(int gridSize);

    void changeOrderingCriteria(int id);

    void changeSortingCriteria(int id);

    EditableListAdapterBase<T> getAdapterImpl();

    IEngineConnection<T> getEngineConnection();

    EditableListFragment.FilteringDelegate<T> getFilteringDelegate();

    RecyclerView getListView();

    int getOrderingCriteria();

    int getSortingCriteria();

    String getUniqueSettingKey(String setting);

    boolean isGridSupported();

    boolean isLocalSelectionActivated();

    boolean isRefreshLocked();

    boolean isRefreshRequested();

    boolean isSortingSupported();

    boolean isUsingLocalSelection();

    boolean loadIfRequested();

    boolean openUri(Uri uri);

    void setFilteringDelegate(EditableListFragment.FilteringDelegate<T> delegate);
}
