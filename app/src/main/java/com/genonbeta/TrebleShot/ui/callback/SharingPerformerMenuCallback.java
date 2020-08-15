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

package com.genonbeta.TrebleShot.ui.callback;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.genonbeta.TrebleShot.App;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog;
import com.genonbeta.TrebleShot.object.MappedSelectable;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask;
import com.genonbeta.android.framework.ui.PerformerMenu;
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine;
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider;

import java.util.ArrayList;
import java.util.List;

public class SharingPerformerMenuCallback extends EditableListFragment.SelectionCallback
{
    private LocalSharingCallback mLocalSharingCallback;

    public SharingPerformerMenuCallback(Activity activity, PerformerEngineProvider provider)
    {
        super(activity, provider);
    }

    @Override
    public boolean onPerformerMenuList(PerformerMenu performerMenu, MenuInflater inflater, Menu targetMenu)
    {
        super.onPerformerMenuList(performerMenu, inflater, targetMenu);
        inflater.inflate(R.menu.action_mode_share, targetMenu);
        return true;
    }

    @Override
    public boolean onPerformerMenuSelected(PerformerMenu performerMenu, MenuItem item)
    {
        int id = item.getItemId();
        IPerformerEngine performerEngine = getPerformerEngine();

        if (performerEngine == null)
            return false;

        List<Shareable> shareableList = compileShareableListFrom(MappedSelectable.compileFrom(performerEngine));

        if (id == R.id.action_mode_share_trebleshot) {
            if (shareableList.size() > 0) {
                if (mLocalSharingCallback != null)
                    mLocalSharingCallback.onShareLocal(shareableList);
                else
                    new ChooseSharingMethodDialog(getActivity(), (method) -> {
                        OrganizeLocalSharingTask task = ChooseSharingMethodDialog.createLocalShareOrganizingTask(
                                method, new ArrayList<>(shareableList));
                        App.run(getActivity(), task);
                    }).show();
            }
        } else
            return super.onPerformerMenuSelected(performerMenu, item);

        // I want the menus to keep showing because sharing does not alter data. If it is so descendants should
        // check and return 'true'.
        return false;
    }

    private static List<Shareable> compileShareableListFrom(List<MappedSelectable<?>> mappedSelectableList)
    {
        List<Shareable> shareableList = new ArrayList<>();

        for (MappedSelectable<?> mappedSelectable : mappedSelectableList)
            if (mappedSelectable.selectable instanceof Shareable)
                shareableList.add((Shareable) mappedSelectable.selectable);

        return shareableList;
    }

    public void setLocalSharingCallback(LocalSharingCallback callback)
    {
        mLocalSharingCallback = callback;
    }
}
