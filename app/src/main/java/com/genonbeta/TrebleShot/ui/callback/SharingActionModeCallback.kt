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
package com.genonbeta.TrebleShot.ui.callbackimport

import com.genonbeta.TrebleShot.dataobject.Shareable

com.genonbeta.TrebleShot.dataobject.MappedSelectable.Companion.compileFrom
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withORs
import com.genonbeta.TrebleShot.dataobject.Identifier.Companion.from
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesPending
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.bytesValue
import com.genonbeta.TrebleShot.dataobject.TransferItem.flag
import com.genonbeta.TrebleShot.dataobject.TransferItem.putFlag
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withANDs
import com.genonbeta.TrebleShot.dataobject.TransferItem.Companion.from
import com.genonbeta.TrebleShot.dataobject.DeviceAddress.hostAddress
import com.genonbeta.TrebleShot.dataobject.Container.expand
import com.genonbeta.TrebleShot.dataobject.Device.equals
import com.genonbeta.TrebleShot.dataobject.TransferItem.flags
import com.genonbeta.TrebleShot.dataobject.TransferItem.getFlag
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.toString
import com.genonbeta.TrebleShot.dataobject.TransferItem.reconstruct
import com.genonbeta.TrebleShot.dataobject.Device.generatePictureId
import com.genonbeta.TrebleShot.dataobject.TransferItem.setDeleteOnRemoval
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.selectableTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasOutgoing
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasIncoming
import com.genonbeta.TrebleShot.dataobject.Comparable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableDate
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableName
import com.genonbeta.TrebleShot.dataobject.Editable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Editable.id
import com.genonbeta.TrebleShot.dataobject.Shareable.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.initialize
import com.genonbeta.TrebleShot.dataobject.Shareable.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Shareable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Shareable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Device.hashCode
import com.genonbeta.TrebleShot.dataobject.TransferIndex.percentage
import com.genonbeta.TrebleShot.dataobject.TransferIndex.getMemberAsTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfCompleted
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfTotal
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesTotal
import com.genonbeta.TrebleShot.dataobject.TransferItem.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.senderFlagList
import com.genonbeta.TrebleShot.dataobject.TransferItem.getPercentage
import com.genonbeta.TrebleShot.dataobject.TransferItem.setId
import com.genonbeta.TrebleShot.dataobject.TransferItem.comparableDate
import com.genonbeta.TrebleShot.dataobject.Identity.equals
import com.genonbeta.TrebleShot.dataobject.Transfer.equals
import com.genonbeta.TrebleShot.dataobject.TransferMember.reconstruct
import android.os.Parcelable
import android.os.Parcel
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import android.content.DialogInterface
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import android.content.Intent
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import androidx.annotation.DrawableRes
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.android.framework.ui.PerformerMenu
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.PickListener
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.Bundle
import androidx.annotation.StyleRes
import android.content.pm.PackageManager
import com.genonbeta.TrebleShot.activity.WelcomeActivity
import com.genonbeta.TrebleShot.GlideApp
import com.bumptech.glide.request.target.CustomTarget
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import com.genonbeta.TrebleShot.config.AppConfig
import kotlin.jvm.Synchronized
import com.genonbeta.TrebleShot.service.BackgroundService
import android.os.PowerManager
import android.graphics.BitmapFactory
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog
import android.widget.ProgressBar
import android.view.LayoutInflater
import kotlin.jvm.JvmOverloads
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.EngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.TrebleShot.app.EditableListFragment.FilteringDelegate
import android.database.ContentObserver
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.ViewGroup
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`

/**
 * created by: veli
 * date: 14/04/18 15:59
 */
class SharingActionModeCallback<T : Shareable?> { /*extends EditableListFragment.SelectionCallback<T>
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
        boolean isLocalShare = id == R.id.action_mode_share_trebleshot;
        boolean isSharing = isLocalShare || id == R.id.action_mode_share_all_apps;
        List<T> selectedItemList = new ArrayList<>(getFragment().getEngineConnection().getSelectedItemList());

        if (selectedItemList.size() > 0 && isSharing) {
            Intent shareIntent = (isLocalShare ? new Intent(context, ShareActivity.class) : new Intent())
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setAction(selectedItemList.size() > 1 ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND);

            ArrayList<Containable> containerList = new ArrayList<>();

            if (selectedItemList.size() > 1) {
                MIMEGrouper mimeGrouper = new MIMEGrouper();
                ArrayList<Uri> uriList = new ArrayList<>();
                ArrayList<CharSequence> nameList = new ArrayList<>();

                for (T sharedItem : selectedItemList) {
                    uriList.add(sharedItem.uri);
                    nameList.add(sharedItem.fileName);

                    addIfEligible(containerList, sharedItem);

                    if (!mimeGrouper.isLocked())
                        mimeGrouper.process(sharedItem.mimeType);
                }

                shareIntent.setType(mimeGrouper.toString())
                        .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                        .putCharSequenceArrayListExtra(ShareActivity.EXTRA_FILENAME_LIST, nameList);
            } else if (selectedItemList.size() == 1) {
                T sharedItem = selectedItemList.get(0);

                addIfEligible(containerList, sharedItem);

                shareIntent.setType(sharedItem.mimeType)
                        .putExtra(Intent.EXTRA_STREAM, sharedItem.uri)
                        .putExtra(ShareActivity.EXTRA_FILENAME_LIST, sharedItem.fileName);
            }

            if (containerList.size() > 0)
                shareIntent.putParcelableArrayListExtra(ShareActivity.EXTRA_FILE_CONTAINER, containerList);

            try {
                if (isLocalShare)
                    new ChooseSharingMethodDialog<T>(getFragment().getActivity(), shareIntent).show();
                else
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(
                            R.string.text_fileShareAppChoose)));
                return true;
            } catch (ActivityNotFoundException e) {
                Toast.makeText(context, R.string.mesg_noActivityFound, Toast.LENGTH_SHORT).show();
            } catch (Throwable e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show();
            }
        } else
            return super.onActionMenuItemSelected(context, actionMode, item);

        return false;
    }

    private void addIfEligible(ArrayList<Containable> list, T sharedItem)
    {
        if (sharedItem instanceof Container) {
            Containable containable = ((Container) sharedItem).expand();

            if (containable != null)
                list.add(containable);
        }
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
    }*/
}