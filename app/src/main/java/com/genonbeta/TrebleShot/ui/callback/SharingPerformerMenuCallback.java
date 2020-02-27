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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog;
import com.genonbeta.TrebleShot.io.Containable;
import com.genonbeta.TrebleShot.object.Container;
import com.genonbeta.TrebleShot.object.MappedSelectable;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.util.MIMEGrouper;
import com.genonbeta.android.framework.ui.PerformerMenu;
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine;
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider;

import java.util.ArrayList;
import java.util.List;

public class SharingPerformerMenuCallback extends EditableListFragment.SelectionCallback
{
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
    public boolean onPerformerMenuClick(PerformerMenu performerMenu, MenuItem item)
    {
        int id = item.getItemId();
        IPerformerEngine performerEngine = getPerformerEngine();

        if (performerEngine == null)
            return false;

        List<Shareable> shareableList = compileShareableListFrom(MappedSelectable.compileFrom(performerEngine));

        if (id == R.id.action_mode_share_trebleshot) {
            if (shareableList.size() <= 0)
                return false;
            new ChooseSharingMethodDialog(getActivity(), shareableList).show();
        } else if (id == R.id.action_mode_share_all_apps) {
            if (shareableList.size() <= 0)
                return false;

            Intent intent = new Intent(shareableList.size() > 1 ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (shareableList.size() > 1) {
                MIMEGrouper mimeGrouper = new MIMEGrouper();
                ArrayList<Uri> uriList = new ArrayList<>();

                for (Shareable sharedItem : shareableList) {
                    uriList.add(sharedItem.uri);

                    if (!mimeGrouper.isLocked())
                        mimeGrouper.process(sharedItem.mimeType);
                }

                intent.setType(mimeGrouper.toString())
                        .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
            } else if (shareableList.size() == 1) {
                Shareable sharedItem = shareableList.get(0);

                intent.setType(sharedItem.mimeType)
                        .putExtra(Intent.EXTRA_STREAM, sharedItem.uri);
            }

            try {
                getActivity().startActivity(Intent.createChooser(intent, getActivity().getString(
                        R.string.text_fileShareAppChoose)));
                return true;
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getActivity(), R.string.mesg_noActivityFound, Toast.LENGTH_SHORT).show();
            } catch (Throwable e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show();
            }
        } else
            return super.onPerformerMenuClick(performerMenu, item);

        return true;
    }

    private List<Shareable> compileShareableListFrom(List<MappedSelectable<?>> mappedSelectableList)
    {
        List<Shareable> shareableList = new ArrayList<>();

        for (MappedSelectable<?> mappedSelectable : mappedSelectableList)
            if (mappedSelectable.selectable instanceof Shareable)
                shareableList.add((Shareable) mappedSelectable.selectable);

        return shareableList;
    }
}
