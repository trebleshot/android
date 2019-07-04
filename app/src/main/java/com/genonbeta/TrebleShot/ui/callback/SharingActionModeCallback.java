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

package com.genonbeta.TrebleShot.ui.callback;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.app.EditableListFragmentImpl;
import com.genonbeta.TrebleShot.app.ShareableListFragment;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.widget.EditableListAdapterImpl;
import com.genonbeta.android.framework.widget.PowerfulActionMode;

import java.util.ArrayList;
import java.util.List;

/**
 * created by: veli
 * date: 14/04/18 15:59
 */
public class SharingActionModeCallback<T extends Shareable> extends EditableListFragment.SelectionCallback<T>
{
    public SharingActionModeCallback(EditableListFragmentImpl<T> fragment)
    {
        super(fragment);
    }

    @Override
    public boolean onPrepareActionMenu(Context context, PowerfulActionMode actionMode)
    {
        super.onPrepareActionMenu(context, actionMode);
        return true;
    }

    @Override
    public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
    {
        super.onCreateActionMenu(context, actionMode, menu);
        actionMode.getMenuInflater().inflate(R.menu.action_mode_share, menu);
        return true;
    }

    @Override
    public boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item)
    {
        int id = item.getItemId();

        List<T> selectedItemList = new ArrayList<>(getFragment().getSelectionConnection().getSelectedItemList());

        if (selectedItemList.size() > 0
                && (id == R.id.action_mode_share_trebleshot || id == R.id.action_mode_share_all_apps)) {
            Intent shareIntent = new Intent()
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setAction((item.getItemId() == R.id.action_mode_share_all_apps)
                            ? (selectedItemList.size() > 1 ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND)
                            : (selectedItemList.size() > 1 ? ShareActivity.ACTION_SEND_MULTIPLE : ShareActivity.ACTION_SEND));

            if (selectedItemList.size() > 1) {
                ShareableListFragment.MIMEGrouper mimeGrouper = new ShareableListFragment.MIMEGrouper();
                ArrayList<Uri> uriList = new ArrayList<>();
                ArrayList<CharSequence> nameList = new ArrayList<>();

                for (T sharedItem : selectedItemList) {
                    uriList.add(sharedItem.uri);
                    nameList.add(sharedItem.fileName);

                    if (!mimeGrouper.isLocked())
                        mimeGrouper.process(sharedItem.mimeType);
                }

                shareIntent.setType(mimeGrouper.toString())
                        .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                        .putCharSequenceArrayListExtra(ShareActivity.EXTRA_FILENAME_LIST, nameList);
            } else if (selectedItemList.size() == 1) {
                T sharedItem = selectedItemList.get(0);

                shareIntent.setType(sharedItem.mimeType)
                        .putExtra(Intent.EXTRA_STREAM, sharedItem.uri)
                        .putExtra(ShareActivity.EXTRA_FILENAME_LIST, sharedItem.fileName);
            }

            try {
                getFragment().getContext().startActivity(item.getItemId() == R.id.action_mode_share_all_apps
                        ? Intent.createChooser(shareIntent, getFragment().getContext().getString(R.string.text_fileShareAppChoose))
                        : shareIntent);
            } catch (Throwable e) {
                e.printStackTrace();
                Toast.makeText(getFragment().getActivity(), R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show();

                return false;
            }
        } else
            return super.onActionMenuItemSelected(context, actionMode, item);

        return true;
    }

    public static class SelectionDuo<T extends Shareable>
    {
        private EditableListFragmentImpl<T> mFragment;
        private EditableListAdapterImpl<T> mAdapter;

        public SelectionDuo(EditableListFragmentImpl<T> fragment, EditableListAdapterImpl<T> adapter)
        {
            mFragment = fragment;
            mAdapter = adapter;
        }

        public EditableListAdapterImpl<T> getAdapter()
        {
            return mAdapter;
        }

        public EditableListFragmentImpl<T> getFragment()
        {
            return mFragment;
        }
    }
}
