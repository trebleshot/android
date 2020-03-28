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

package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.system.Os;
import android.system.StructStatVfs;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.ImageViewCompat;
import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.EditableListFragmentBase;
import com.genonbeta.TrebleShot.app.IEditableListFragment;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.ShowingAssignee;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.util.*;
import com.genonbeta.TrebleShot.view.EditableListFragmentViewBase;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.exception.ReconstructionFailedException;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.io.LocalDocumentFile;
import com.genonbeta.android.framework.io.TreeDocumentFile;
import com.genonbeta.android.framework.util.MathUtils;
import com.genonbeta.android.framework.util.listing.ComparableMerger;
import com.genonbeta.android.framework.util.listing.Merger;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by: veli
 * Date: 4/15/17 12:29 PM
 */

public class TransferListAdapter extends GroupEditableListAdapter<TransferListAdapter.GenericItem,
        GroupEditableListAdapter.GroupViewHolder> implements GroupEditableListAdapter.GroupLister.CustomGroupLister<
        TransferListAdapter.GenericItem>
{
    //public static final int MODE_SORT_BY_DEFAULT = MODE_SORT_BY_NAME - 1;
    public static final int MODE_GROUP_BY_DEFAULT = MODE_GROUP_BY_NOTHING + 1;

    private SQLQuery.Select mSelect;
    private String mPath;
    private ShowingAssignee mAssignee;
    private TransferGroup mGroup = new TransferGroup();
    private PathChangedListener mListener;
    private NumberFormat mPercentFormat = NumberFormat.getPercentInstance();

    @ColorInt
    private int mColorPending;
    private int mColorDone;
    private int mColorError;

    public TransferListAdapter(IEditableListFragment<GenericItem, GroupViewHolder> fragment)
    {
        super(fragment, MODE_GROUP_BY_DEFAULT);

        Context context = getContext();
        mColorPending = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorControlNormal));
        mColorDone = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorAccent));
        mColorError = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorError));

        setSelect(new SQLQuery.Select(Kuick.TABLE_TRANSFER));
    }

    @Override
    protected void onLoad(GroupLister<GenericItem> lister)
    {
        final boolean loadThumbnails = AppUtils.getDefaultPreferences(getContext())
                .getBoolean("load_thumbnails", true);

        try {
            AppUtils.getKuick(getContext()).reconstruct(mGroup);
        } catch (ReconstructionFailedException e) {
            e.printStackTrace();
            return;
        }

        boolean hasIncoming = false;
        String currentPath = getPath();
        currentPath = currentPath == null || currentPath.length() == 0 ? null : currentPath;

        Map<String, TransferFolder> folders = new ArrayMap<>();
        ShowingAssignee assignee = getAssignee();
        List<ShowingAssignee> assignees = TransferUtils.loadAssigneeList(getContext(), getGroupId(), null);
        ShowingAssignee[] assigneeArray = new ShowingAssignee[assignees.size()];

        assignees.toArray(assigneeArray);

        SQLQuery.Select transferSelect = new SQLQuery.Select(Kuick.TABLE_TRANSFER);
        StringBuilder transferWhere = new StringBuilder(Kuick.FIELD_TRANSFER_GROUPID + "=?");
        List<String> transferArgs = new ArrayList<>();
        transferArgs.add(String.valueOf(mGroup.id));

        if (currentPath != null) {
            transferWhere.append(" AND (" + Kuick.FIELD_TRANSFER_DIRECTORY + "=? OR "
                    + Kuick.FIELD_TRANSFER_DIRECTORY + " LIKE ?)");

            transferArgs.add(currentPath);
            transferArgs.add(currentPath + File.separator + "%");
        }

        if (assignee != null) {
            transferWhere.append(" AND " + Kuick.FIELD_TRANSFER_TYPE + "=?");
            transferArgs.add(assignee.type.toString());
        }

        if (getSortingCriteria() == MODE_GROUP_BY_DATE) {
            transferSelect.setOrderBy(Kuick.FIELD_TRANSFER_LASTCHANGETIME + " "
                    + (getSortingOrder() == MODE_SORT_ORDER_ASCENDING ? "ASC" : "DESC"));
        }

        transferSelect.where = transferWhere.toString();
        transferSelect.whereArgs = new String[transferArgs.size()];
        transferArgs.toArray(transferSelect.whereArgs);

        DetailsTransferFolder statusItem = new DetailsTransferFolder(mGroup.id, currentPath == null
                ? (assignee == null ? getContext().getString(R.string.text_home) : assignee.device.nickname) : currentPath.contains(
                File.separator) ? currentPath.substring(currentPath.lastIndexOf(File.separator) + 1)
                : currentPath, currentPath);
        lister.offerObliged(this, statusItem);

        List<GenericTransferItem> derivedList = AppUtils.getKuick(getContext()).castQuery(
                transferSelect, GenericTransferItem.class);

        // we first get the default files
        for (GenericTransferItem object : derivedList) {
            object.assignees = assigneeArray;
            object.directory = object.directory == null || object.directory.length() == 0 ? null : object.directory;

            if (currentPath != null && object.directory == null)
                continue;

            TransferFolder transferFolder = null;
            boolean isIncoming = TransferObject.Type.INCOMING.equals(object.type);
            boolean isOutgoing = TransferObject.Type.OUTGOING.equals(object.type);

            if ((currentPath == null && object.directory == null) || object.directory.equals(currentPath)) {
                try {
                    if (!loadThumbnails)
                        object.supportThumbnail = false;
                    else {
                        String[] format = object.mimeType.split(File.separator);

                        if (format.length > 0 && ("image".equals(format[0]) || "video".equals(format[0]))) {
                            DocumentFile documentFile = null;

                            if (isOutgoing)
                                documentFile = FileUtils.fromUri(getContext(), Uri.parse(object.file));
                            else if (TransferObject.Flag.DONE.equals(object.getFlag()))
                                documentFile = FileUtils.getIncomingPseudoFile(getContext(), object, mGroup,
                                        false);

                            if (documentFile != null && documentFile.exists()) {
                                object.documentFile = documentFile;
                                object.supportThumbnail = true;
                            }
                        } else
                            object.supportThumbnail = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                lister.offerObliged(this, object);
            } else if (currentPath == null || (object.directory.startsWith(currentPath))) {
                int pathToErase = currentPath == null ? 0 : currentPath.length() + File.separator.length();
                String cleanedPath = object.directory.substring(pathToErase);
                int slashPos = cleanedPath.indexOf(File.separator);

                if (slashPos != -1)
                    cleanedPath = cleanedPath.substring(0, slashPos);

                transferFolder = folders.get(cleanedPath);

                if (transferFolder == null) {
                    transferFolder = new TransferFolder(mGroup.id, cleanedPath, currentPath != null
                            ? currentPath + File.separator + cleanedPath : cleanedPath);

                    folders.put(cleanedPath, transferFolder);
                    lister.offerObliged(this, transferFolder);
                }
            }

            if (!hasIncoming && isIncoming)
                hasIncoming = true;

            mergeTransferInfo(statusItem, object, isIncoming, transferFolder);
        }

        if (currentPath == null && hasIncoming)
            try {
                TransferGroup group = new TransferGroup(mGroup.id);
                AppUtils.getKuick(getContext()).reconstruct(group);
                DocumentFile savePath = FileUtils.getSavePath(getContext(), group);

                StorageStatusItem storageItem = new StorageStatusItem();
                storageItem.directory = savePath.getUri().toString();
                storageItem.name = savePath.getName();
                storageItem.bytesRequired = statusItem.bytesTotal - statusItem.bytesReceived;

                if (savePath instanceof LocalDocumentFile) {
                    File saveFile = ((LocalDocumentFile) savePath).getFile();
                    storageItem.bytesTotal = saveFile.getTotalSpace();
                    storageItem.bytesFree = saveFile.getFreeSpace(); // return used space
                } else if (Build.VERSION.SDK_INT >= 21 && savePath instanceof TreeDocumentFile) {
                    try {
                        ParcelFileDescriptor descriptor = getContext().getContentResolver().openFileDescriptor(
                                savePath.getOriginalUri(), "r");
                        if (descriptor != null) {
                            StructStatVfs stats = Os.fstatvfs(descriptor.getFileDescriptor());
                            storageItem.bytesTotal = stats.f_blocks * stats.f_bsize;
                            storageItem.bytesFree = stats.f_bavail * stats.f_bsize;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    storageItem.bytesTotal = -1;
                    storageItem.bytesFree = -1;
                }

                lister.offerObliged(this, storageItem);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    @Override
    protected GenericTransferItem onGenerateRepresentative(String text, Merger<GenericItem> merger)
    {
        return new GenericTransferItem(text);
    }

    @Override
    public boolean onCustomGroupListing(GroupLister<GenericItem> lister, int mode, GenericItem object)
    {
        if (mode == MODE_GROUP_BY_DEFAULT)
            lister.offer(object, new GroupEditableTransferObjectMerger(object, this));
        else
            return false;

        return true;
    }

    @Override
    public int compareItems(int sortingCriteria, int sortingOrder, GenericItem objectOne,
                            GenericItem objectTwo)
    {
        //if (sortingCriteria == MODE_SORT_BY_DEFAULT)
        //    return MathUtils.compare(objectTwo.requestId, objectOne.requestId);

        return 1;
    }

    @Override
    public GroupLister<GenericItem> createLister(List<GenericItem> loadedList, int groupBy)
    {
        return super.createLister(loadedList, groupBy)
                .setCustomLister(this);
    }

    public ShowingAssignee getAssignee()
    {
        return mAssignee;
    }

    public String getDeviceId()
    {
        return getAssignee() == null ? null : getAssignee().deviceId;
    }

    public void mergeTransferInfo(DetailsTransferFolder details, GenericTransferItem object, boolean isIncoming,
                                  @Nullable TransferFolder folder)
    {
        if (isIncoming) {
            mergeTransferInfo(details, object, object.getFlag(), true, folder);
        } else {
            if (getAssignee() != null)
                mergeTransferInfo(details, object, object.getFlag(getDeviceId()), false, folder);
            else if (object.assignees.length < 1)
                mergeTransferInfo(details, object, TransferObject.Flag.PENDING, false, folder);
            else
                for (ShowingAssignee showingAssignee : object.assignees) {
                    if (!TransferObject.Type.OUTGOING.equals(showingAssignee.type))
                        continue;

                    mergeTransferInfo(details, object, object.getFlag(showingAssignee.deviceId),
                            false, folder);
                }
        }
    }

    public void mergeTransferInfo(DetailsTransferFolder details, TransferObject object, TransferObject.Flag flag,
                                  boolean isIncoming, @Nullable TransferFolder folder)
    {
        details.bytesTotal += object.size;
        details.numberOfTotal++;

        if (folder != null) {
            folder.bytesTotal += object.size;
            folder.numberOfTotal++;
        }

        if (TransferObject.Flag.DONE.equals(flag)) {
            details.numberOfCompleted++;
            details.bytesCompleted += object.size;

            if (folder != null) {
                folder.numberOfCompleted++;
                folder.bytesCompleted += object.size;
            }
        } else if (TransferUtils.isError(flag)) {
            details.hasIssues = true;

            if (folder != null)
                folder.hasIssues = true;
        } else if (TransferObject.Flag.IN_PROGRESS.equals(flag)) {
            long completed = flag.getBytesValue();

            details.bytesCompleted += completed;
            details.hasOngoing = true;

            if (folder != null) {
                folder.bytesCompleted += completed;
                folder.hasOngoing = true;
            }

            if (isIncoming) {
                details.bytesReceived += completed;
                if (folder != null)
                    folder.bytesReceived += completed;
            }
        }
    }

    public boolean setAssignee(ShowingAssignee assignee)
    {
        if (assignee == null) {
            mAssignee = null;
            return true;
        }

        try {
            AppUtils.getKuick(getContext()).reconstruct(assignee);
            mAssignee = assignee;
            return true;
        } catch (ReconstructionFailedException ignored) {
            return false;
        }
    }

    public long getGroupId()
    {
        return mGroup.id;
    }

    public void setGroupId(long groupId)
    {
        mGroup.id = groupId;
    }

    public String getPath()
    {
        return mPath;
    }

    public void setPath(String path)
    {
        mPath = path;

        if (mListener != null)
            mListener.onPathChange(path);
    }

    private NumberFormat getPercentFormat()
    {
        return mPercentFormat;
    }

    @Override
    public String getRepresentativeText(Merger<? extends GenericItem> merger)
    {
        if (merger instanceof GroupEditableTransferObjectMerger) {
            switch (((GroupEditableTransferObjectMerger) merger).getType()) {
                case STATUS:
                    return getContext().getString(R.string.text_transactionDetails);
                case FOLDER:
                    return getContext().getString(R.string.text_folder);
                case FILE_ERROR:
                    return getContext().getString(R.string.text_flagInterrupted);
                case FOLDER_ONGOING:
                case FILE_ONGOING:
                    return getContext().getString(R.string.text_taskOngoing);
                default:
                    return getContext().getString(R.string.text_file);
            }
        }

        return super.getRepresentativeText(merger);
    }

    public SQLQuery.Select getSelect()
    {
        return mSelect;
    }

    public TransferListAdapter setSelect(SQLQuery.Select select)
    {
        if (select != null)
            mSelect = select;

        return this;
    }

    public void setPathChangedListener(PathChangedListener listener)
    {
        mListener = listener;
    }

    @NonNull
    @Override
    public GroupEditableListAdapter.GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        GroupViewHolder holder = viewType == VIEW_TYPE_DEFAULT ? new GroupViewHolder(getInflater().inflate(
                R.layout.list_transfer, parent, false)) : createDefaultViews(parent, viewType,
                false);

        if (!holder.isRepresentative()) {
            getFragment().registerLayoutViewClicks(holder);
            holder.itemView.findViewById(R.id.layout_image)
                    .setOnClickListener(v -> getFragment().setItemSelected(holder, true));
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final GroupEditableListAdapter.GroupViewHolder holder, int position)
    {
        try {
            final GenericItem object = getItem(position);

            if (!holder.tryBinding(object)) {
                final View parentView = holder.itemView;

                @ColorInt
                int appliedColor;
                int percentage = (int) (object.getPercentage(this) * 100);
                ProgressBar progressBar = parentView.findViewById(R.id.progressBar);
                ImageView thumbnail = parentView.findViewById(R.id.thumbnail);
                ImageView image = parentView.findViewById(R.id.image);
                ImageView indicator = parentView.findViewById(R.id.indicator);
                ImageView sIcon = parentView.findViewById(R.id.statusIcon);
                TextView titleText = parentView.findViewById(R.id.text);
                TextView firstText = parentView.findViewById(R.id.text2);
                TextView secondText = parentView.findViewById(R.id.text3);
                TextView thirdText = parentView.findViewById(R.id.text4);

                parentView.setSelected(object.isSelectableSelected());

                if (object.hasIssues(this))
                    appliedColor = mColorError;
                else if (object.isComplete(this))
                    appliedColor = mColorDone;
                else
                    appliedColor = mColorPending;

                titleText.setText(object.name);
                firstText.setText(object.getFirstText(this));
                secondText.setText(object.getSecondText(this));
                thirdText.setText(object.getThirdText(this));

                object.handleStatusIcon(sIcon, mGroup);
                object.handleStatusIndicator(indicator);
                ImageViewCompat.setImageTintList(sIcon, ColorStateList.valueOf(appliedColor));
                progressBar.setMax(100);

                if (Build.VERSION.SDK_INT >= 24)
                    progressBar.setProgress(percentage <= 0 ? 1 : percentage, true);
                else
                    progressBar.setProgress(percentage <= 0 ? 1 : percentage);

                thirdText.setTextColor(appliedColor);
                ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(appliedColor));

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    Drawable wrapDrawable = DrawableCompat.wrap(progressBar.getProgressDrawable());

                    DrawableCompat.setTint(wrapDrawable, appliedColor);
                    progressBar.setProgressDrawable(DrawableCompat.unwrap(wrapDrawable));
                } else
                    progressBar.setProgressTintList(ColorStateList.valueOf(appliedColor));

                boolean supportThumbnail = object.loadThumbnail(thumbnail);

                progressBar.setVisibility(!supportThumbnail || !object.isComplete(this) ? View.VISIBLE
                        : View.GONE);

                if (supportThumbnail)
                    image.setImageDrawable(null);
                else {
                    image.setImageResource(object.getIconRes());
                    thumbnail.setImageDrawable(null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface PathChangedListener
    {
        void onPathChange(String path);
    }

    interface StatusItem
    {

    }

    public static abstract class GenericItem extends TransferObject implements GroupEditableListAdapter.GroupEditable
    {
        public int viewType;
        public String representativeText;

        public GenericItem()
        {
        }

        public GenericItem(String representativeText)
        {
            this.viewType = VIEW_TYPE_REPRESENTATIVE;
            setRepresentativeText(representativeText);
        }

        @Override
        public boolean applyFilter(String[] filteringKeywords)
        {
            for (String keyword : filteringKeywords)
                if (name != null && name.toLowerCase().contains(keyword.toLowerCase()))
                    return true;

            return false;
        }

        @DrawableRes
        abstract public int getIconRes();

        abstract public boolean loadThumbnail(ImageView imageView);

        abstract public double getPercentage(TransferListAdapter adapter);

        abstract public boolean hasIssues(TransferListAdapter adapter);

        abstract public boolean isComplete(TransferListAdapter adapter);

        abstract public boolean isOngoing(TransferListAdapter adapter);

        abstract public void handleStatusIcon(ImageView imageView, TransferGroup group);

        abstract public void handleStatusIndicator(ImageView imageView);

        abstract public String getFirstText(TransferListAdapter adapter);

        abstract public String getSecondText(TransferListAdapter adapter);

        abstract public String getThirdText(TransferListAdapter adapter);

        @Override
        public int getRequestCode()
        {
            return 0;
        }

        @Override
        public int getViewType()
        {
            return this.viewType;
        }

        @Override
        public String getRepresentativeText()
        {
            return this.representativeText;
        }

        @Override
        public void setRepresentativeText(CharSequence text)
        {
            this.representativeText = String.valueOf(text);
        }

        @Override
        public boolean isGroupRepresentative()
        {
            return this.viewType == VIEW_TYPE_REPRESENTATIVE;
        }

        @Override
        public void setDate(long date)
        {
            // stamp
        }

        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            return !isGroupRepresentative() && super.setSelectableSelected(selected);
        }

        @Override
        public void setSize(long size)
        {
            this.size = size;
        }
    }

    public static class GenericTransferItem extends GenericItem
    {
        @Nullable
        DocumentFile documentFile;
        ShowingAssignee[] assignees;
        boolean supportThumbnail;

        public GenericTransferItem()
        {
        }

        GenericTransferItem(String representativeText)
        {
            super(representativeText);
        }

        @Override
        public boolean applyFilter(String[] filteringKeywords)
        {
            if (super.applyFilter(filteringKeywords))
                return true;

            for (String keyword : filteringKeywords)
                if (mimeType.toLowerCase().contains(keyword.toLowerCase()))
                    return true;

            return false;
        }

        @Override
        public int getIconRes()
        {
            return MimeIconUtils.loadMimeIcon(mimeType);
        }

        @Override
        public void handleStatusIcon(ImageView imageView, TransferGroup group)
        {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(Type.INCOMING.equals(type) ? R.drawable.ic_arrow_down_white_24dp
                    : R.drawable.ic_arrow_up_white_24dp);
        }

        @Override
        public void handleStatusIndicator(ImageView imageView)
        {
            imageView.setVisibility(View.GONE);
        }

        @Override
        public String getFirstText(TransferListAdapter adapter)
        {
            return FileUtils.sizeExpression(size, false);
        }

        @Override
        public String getSecondText(TransferListAdapter adapter)
        {
            if (adapter.getAssignee() != null)
                return adapter.getAssignee().device.nickname;

            int totalDevices = 1;

            if (Type.OUTGOING.equals(type))
                synchronized (mSenderFlagList) {
                    totalDevices = getSenderFlagList().size();
                }

            return adapter.getContext().getResources().getQuantityString(R.plurals.text_devices,
                    totalDevices, totalDevices);
        }

        @Override
        public String getThirdText(TransferListAdapter adapter)
        {
            return TextUtils.getTransactionFlagString(adapter.getContext(), this,
                    adapter.getPercentFormat(), adapter.getDeviceId());
        }

        @Override
        public boolean loadThumbnail(ImageView imageView)
        {
            if (documentFile != null && supportThumbnail && documentFile.exists()) {
                GlideApp.with(imageView.getContext())
                        .load(documentFile.getUri())
                        .error(getIconRes())
                        .override(160)
                        .centerCrop()
                        .into(imageView);

                return true;
            }

            return false;
        }

        @Override
        public double getPercentage(TransferListAdapter adapter)
        {
            return getPercentage(assignees, adapter.getDeviceId());
        }

        @Override
        public boolean hasIssues(TransferListAdapter adapter)
        {
            if (assignees.length == 0)
                return false;

            if (Type.INCOMING.equals(type))
                return TransferUtils.isError(getFlag());
            else if (adapter.getDeviceId() != null) {
                return TransferUtils.isError(getFlag(adapter.getDeviceId()));
            } else
                synchronized (mSenderFlagList) {
                    for (ShowingAssignee assignee : assignees)
                        if (TransferUtils.isError(getFlag(assignee.deviceId)))
                            return true;
                }

            return false;
        }

        @Override
        public boolean isComplete(TransferListAdapter adapter)
        {
            if (assignees.length == 0)
                return false;

            if (Type.INCOMING.equals(type))
                return Flag.DONE.equals(getFlag());
            else if (adapter.getDeviceId() != null) {
                return Flag.DONE.equals(getFlag(adapter.getDeviceId()));
            } else
                synchronized (mSenderFlagList) {
                    for (ShowingAssignee assignee : assignees)
                        if (!Flag.DONE.equals(getFlag(assignee.deviceId)))
                            return false;
                }

            return true;
        }

        @Override
        public boolean isOngoing(TransferListAdapter adapter)
        {
            if (assignees.length == 0)
                return false;

            if (Type.INCOMING.equals(type))
                return Flag.IN_PROGRESS.equals(getFlag());
            else if (adapter.getDeviceId() != null) {
                return Flag.IN_PROGRESS.equals(getFlag(adapter.getDeviceId()));
            } else
                synchronized (mSenderFlagList) {
                    for (ShowingAssignee assignee : assignees)
                        if (Flag.IN_PROGRESS.equals(getFlag(assignee.deviceId)))
                            return true;
                }

            return false;
        }
    }

    public static class TransferFolder extends GenericItem
    {
        boolean hasIssues;
        boolean hasOngoing;
        int numberOfTotal = 0;
        int numberOfCompleted = 0;
        long bytesTotal = 0;
        long bytesCompleted = 0;
        long bytesReceived = 0;

        TransferFolder(long groupId, String friendlyName, String directory)
        {
            this.groupId = groupId;
            this.name = friendlyName;
            this.directory = directory;
        }

        @Override
        public int getIconRes()
        {
            return R.drawable.ic_folder_white_24dp;
        }

        @Override
        public void handleStatusIcon(ImageView imageView, TransferGroup group)
        {
            imageView.setVisibility(View.GONE);
        }

        @Override
        public void handleStatusIndicator(ImageView imageView)
        {
            imageView.setVisibility(View.GONE);
        }

        @Override
        public boolean hasIssues(TransferListAdapter adapter)
        {
            return hasIssues;
        }

        @Override
        public String getFirstText(TransferListAdapter adapter)
        {
            return FileUtils.sizeExpression(bytesTotal, false);
        }

        @Override
        public String getSecondText(TransferListAdapter adapter)
        {
            return adapter.getContext()
                    .getString(R.string.text_transferStatusFiles, numberOfCompleted, numberOfTotal);
        }

        @Override
        public String getThirdText(TransferListAdapter adapter)
        {
            return adapter.getPercentFormat().format(getPercentage(adapter));
        }

        @Override
        public SQLQuery.Select getWhere()
        {
            return new SQLQuery.Select(Kuick.TABLE_TRANSFER)
                    .setWhere(Kuick.FIELD_TRANSFER_GROUPID + "=? AND ("
                                    + Kuick.FIELD_TRANSFER_DIRECTORY + " LIKE ? OR "
                                    + Kuick.FIELD_TRANSFER_DIRECTORY + " = ?)",
                            String.valueOf(this.groupId), this.directory + File.separator + "%", this.directory);
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof TransferFolder && directory != null && directory.equals(
                    ((TransferFolder) obj).directory);
        }

        @Override
        public long getId()
        {
            return directory.hashCode();
        }

        @Override
        public void setId(long id)
        {
            super.setId(id);
            Log.d(TransferListAdapter.class.getSimpleName(), "setId(): This method should not be invoked");
        }

        @Override
        public boolean loadThumbnail(ImageView imageView)
        {
            return false;
        }

        @Override
        public double getPercentage(TransferListAdapter adapter)
        {
            return bytesTotal == 0 || bytesCompleted == 0 ? 0 : (float) bytesCompleted / bytesTotal;
        }

        @Override
        public boolean isComplete(TransferListAdapter adapter)
        {
            return numberOfTotal == numberOfCompleted && numberOfTotal != 0;
        }

        @Override
        public boolean isOngoing(TransferListAdapter adapter)
        {
            return hasOngoing;
        }
    }

    public static class DetailsTransferFolder extends TransferFolder implements StatusItem
    {
        DetailsTransferFolder(long groupId, String friendlyName, String directory)
        {
            super(groupId, friendlyName, directory);
        }

        @Override
        public void handleStatusIcon(ImageView imageView, TransferGroup group)
        {
            if (group.isServedOnWeb) {
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageResource(R.drawable.ic_web_white_24dp);
            } else
                super.handleStatusIcon(imageView, group);
        }

        @Override
        public void handleStatusIndicator(ImageView imageView)
        {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(R.drawable.ic_arrow_right_white_24dp);
        }

        @Override
        public int getIconRes()
        {
            return R.drawable.ic_device_hub_white_24dp;
        }

        @Override
        public long getId()
        {
            return (directory != null ? directory : name).hashCode();
        }

        @Override
        public boolean isSelectableSelected()
        {
            return false;
        }

        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            return false;
        }
    }

    public static class StorageStatusItem extends GenericItem implements StatusItem
    {
        long bytesTotal = 0;
        long bytesFree = 0;
        long bytesRequired = 0;

        @Override
        public boolean hasIssues(TransferListAdapter adapter)
        {
            return bytesFree < bytesRequired && bytesFree != -1;
        }

        @Override
        public boolean isComplete(TransferListAdapter adapter)
        {
            return bytesFree == -1 || !hasIssues(adapter);
        }

        @Override
        public boolean isOngoing(TransferListAdapter adapter)
        {
            return false;
        }

        @Override
        public boolean isSelectableSelected()
        {
            return false;
        }

        @Override
        public int getIconRes()
        {
            return R.drawable.ic_save_white_24dp;
        }

        @Override
        public long getId()
        {
            return (directory != null ? directory : name).hashCode();
        }

        @Override
        public double getPercentage(TransferListAdapter adapter)
        {
            return bytesTotal <= 0 || bytesFree <= 0 ? 0 : Long.valueOf(bytesTotal - bytesFree)
                    .doubleValue() / Long.valueOf(bytesTotal).doubleValue();
        }

        @Override
        public void handleStatusIcon(ImageView imageView, TransferGroup group)
        {
            imageView.setVisibility(View.GONE);
        }

        @Override
        public void handleStatusIndicator(ImageView imageView)
        {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(R.drawable.ic_arrow_right_white_24dp);
        }

        @Override
        public String getFirstText(TransferListAdapter adapter)
        {
            return bytesFree == -1 ? adapter.getContext().getString(R.string.text_unknown)
                    : FileUtils.sizeExpression(bytesFree, false);
        }

        @Override
        public String getSecondText(TransferListAdapter adapter)
        {
            return adapter.getContext().getString(R.string.text_savePath);
        }

        @Override
        public String getThirdText(TransferListAdapter adapter)
        {
            return adapter.getPercentFormat().format(getPercentage(adapter));
        }

        @Override
        public boolean loadThumbnail(ImageView imageView)
        {
            return false;
        }

        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            return false;
        }
    }

    public static class GroupEditableTransferObjectMerger extends ComparableMerger<GenericItem>
    {
        private Type mType;

        GroupEditableTransferObjectMerger(GenericItem holder, TransferListAdapter adapter)
        {
            if (holder instanceof StatusItem)
                mType = Type.STATUS;
            else if (holder instanceof TransferFolder)
                //mType = holder.hasOngoing(adapter.getDeviceId()) ? Type.FOLDER_ONGOING : Type.FOLDER;
                mType = Type.FOLDER;
            else {
                if (holder.hasIssues(adapter))
                    mType = Type.FILE_ERROR;
                else if (holder.isOngoing(adapter))
                    mType = Type.FILE_ONGOING;
                else
                    mType = Type.FILE;
            }
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof GroupEditableTransferObjectMerger
                    && ((GroupEditableTransferObjectMerger) obj).getType().equals(getType());
        }

        public Type getType()
        {
            return mType;
        }

        @Override
        public int compareTo(@NonNull ComparableMerger<GenericItem> o)
        {
            if (o instanceof GroupEditableTransferObjectMerger)
                return MathUtils.compare(((GroupEditableTransferObjectMerger) o).getType().ordinal(), getType().ordinal());

            return 1;
        }

        public enum Type
        {
            STATUS,
            FOLDER_ONGOING,
            FOLDER,
            FILE_ONGOING,
            FILE_ERROR,
            FILE,
        }
    }
}
