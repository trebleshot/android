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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.ShowingAssignee;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.MimeIconUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.exception.ReconstructionFailedException;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.io.LocalDocumentFile;
import com.genonbeta.android.framework.util.MathUtils;
import com.genonbeta.android.framework.util.listing.ComparableMerger;
import com.genonbeta.android.framework.util.listing.Merger;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.ImageViewCompat;

/**
 * Created by: veli
 * Date: 4/15/17 12:29 PM
 */

public class TransferListAdapter
		extends GroupEditableListAdapter<TransferListAdapter.AbstractGenericItem, GroupEditableListAdapter.GroupViewHolder>
		implements GroupEditableListAdapter.GroupLister.CustomGroupLister<TransferListAdapter.AbstractGenericItem>
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

	public TransferListAdapter(Context context)
	{
		super(context, MODE_GROUP_BY_DEFAULT);

		mColorPending = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorControlNormal));
		mColorDone = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorAccent));
		mColorError = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorError));

		setSelect(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER));
	}

	@Override
	protected void onLoad(GroupLister<AbstractGenericItem> lister)
	{
		final boolean loadThumbnails = AppUtils.getDefaultPreferences(getContext())
				.getBoolean("load_thumbnails", true);

		try {
			AppUtils.getDatabase(getContext()).reconstruct(mGroup);
		} catch (ReconstructionFailedException e) {
			e.printStackTrace();
			return;
		}

		ShowingAssignee assignee = getAssignee();
		boolean hasIncoming = false;
		int senderMultiplierBase = 0;
		Map<String, TransferFolder> folders = new ArrayMap<>();
		List<GenericTransferItem> files = new ArrayList<>();
		String currentPath = getPath();
		currentPath = currentPath == null || currentPath.length() == 0 ? null : currentPath;

		List<ShowingAssignee> assignees = TransferUtils.loadAssigneeList(getContext(),
				getGroupId(), null);

		for (ShowingAssignee showingAssignee : assignees) {
			if (TransferObject.Type.OUTGOING.equals(showingAssignee.type))
				senderMultiplierBase++;
		}

		SQLQuery.Select transferSelect = new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER);
		StringBuilder transferWhere = new StringBuilder(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?");
		List<String> transferArgs = new ArrayList<>();
		transferArgs.add(String.valueOf(mGroup.id));

		if (currentPath != null) {
			transferWhere.append(" AND (" + AccessDatabase.FIELD_TRANSFER_DIRECTORY + "=? OR "
					+ AccessDatabase.FIELD_TRANSFER_DIRECTORY + " LIKE ?)");

			transferArgs.add(currentPath);
			transferArgs.add(currentPath + File.separator + "%");
		}

		if (assignee != null) {
			transferWhere.append(" AND " + AccessDatabase.FIELD_TRANSFER_TYPE + "=?");
			transferArgs.add(assignee.type.toString());
		}

		transferSelect.where = transferWhere.toString();
		transferSelect.whereArgs = new String[transferArgs.size()];
		transferArgs.toArray(transferSelect.whereArgs);

		List<GenericTransferItem> derivedList = AppUtils.getDatabase(getContext()).castQuery(
				transferSelect, GenericTransferItem.class);

		// we first get the default files
		for (GenericTransferItem object : derivedList) {
			object.directory = object.directory == null || object.directory.length() == 0 ? null
					: object.directory;

			if (currentPath != null && object.directory == null)
				continue;

			if ((currentPath == null && object.directory == null)
					|| object.directory.equals(currentPath)) {
				try {
					if (!loadThumbnails)
						object.setSupportThumbnail(false);
					else {
						String[] format = object.mimeType.split(File.separator);

						if (format.length > 0 && ("image".equals(format[0]) || "video".equals(format[0]))) {
							DocumentFile documentFile = null;

							if (TransferObject.Type.OUTGOING.equals(object.type))
								documentFile = FileUtils.fromUri(getContext(), Uri.parse(object.file));
							else if (TransferObject.Flag.DONE.equals(object.getFlag()))
								documentFile = FileUtils.getIncomingPseudoFile(getContext(), object, mGroup, false);

							if (documentFile != null && documentFile.exists()) {
								object.setFile(documentFile);
								object.setSupportThumbnail(true);
							}
						} else
							object.setSupportThumbnail(false);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				files.add(object);
			} else if (currentPath == null || (object.directory.startsWith(currentPath))) {
				int pathToErase = currentPath == null ? 0 : currentPath.length() + File.separator.length();
				String cleanedPath = object.directory.substring(pathToErase);
				int slashPos = cleanedPath.indexOf(File.separator);

				if (slashPos != -1)
					cleanedPath = cleanedPath.substring(0, slashPos);

				TransferFolder transferFolder = folders.get(cleanedPath);

				if (transferFolder == null) {
					transferFolder = new TransferFolder(mGroup.id, cleanedPath, currentPath != null
							? currentPath + File.separator + cleanedPath : cleanedPath);

					folders.put(cleanedPath, transferFolder);
				}

				if (object.hasComplete(getDeviceId()))
					transferFolder.numberOfCompleted++;

				if (!transferFolder.mHasIssues && object.hasIssues(getDeviceId()))
					transferFolder.setHasIssues(true);

				if (!transferFolder.mHasOngoing && object.hasOngoing(getDeviceId()))
					transferFolder.setHasOngoing(true);

				if (TransferObject.Type.INCOMING.equals(object.type)) {
					if (TransferObject.Flag.IN_PROGRESS.equals(object.getFlag()))
						transferFolder.bytesReceived += object.getFlag().getBytesValue();
				} else {
					for (ShowingAssignee showingAssignee : assignees) {
						if (!TransferObject.Type.OUTGOING.equals(showingAssignee.type))
							continue;


					}
				}

				transferFolder.numberOfTotal++;
				transferFolder.bytesTotal += object.size;
			}

			if (!hasIncoming && TransferObject.Type.INCOMING.equals(object.type))
				hasIncoming = true;
		}

		StorageStatusItem storageItem = null;

		if (currentPath == null)
			if (hasIncoming) {
				try {
					TransferGroup group = new TransferGroup(mGroup.id);
					AppUtils.getDatabase(getContext()).reconstruct(group);
					DocumentFile savePath = FileUtils.getSavePath(getContext(), group);

					storageItem = new StorageStatusItem();
					storageItem.directory = savePath.getUri().toString();
					storageItem.name = savePath.getName();

					if (savePath instanceof LocalDocumentFile) {
						File saveFile = ((LocalDocumentFile) savePath).getFile();
						storageItem.bytesTotal = saveFile.getTotalSpace();
						storageItem.bytesFree = saveFile.getFreeSpace(); // return used space
					} else {
						storageItem.bytesTotal = -1;
						storageItem.bytesFree = -1;
					}

					lister.offerObliged(this, storageItem);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		DetailsTransferFolder statusItem = new DetailsTransferFolder(mGroup.id, currentPath == null
				? (assignee == null ? getContext().getString(R.string.text_home) : assignee.device.nickname) : currentPath.contains(
				File.separator) ? currentPath.substring(currentPath.lastIndexOf(File.separator) + 1)
				: currentPath, currentPath);

		lister.offerObliged(this, statusItem);

		for (TransferFolder folder : folders.values()) {
			statusItem.numberOfTotal += folder.numberOfTotal;
			statusItem.numberOfCompleted += folder.numberOfCompleted;
			statusItem.bytesTotal += folder.bytesTotal;
			statusItem.bytesReceived += folder.bytesReceived;

			if (folder.hasIssues(getDeviceId()))
				statusItem.setHasIssues(true);

			if (folder.hasOngoing(getDeviceId()))
				statusItem.setHasIssues(true);

			lister.offerObliged(this, folder);
		}

		for (GenericTransferItem file : files) {
			if (file.hasComplete(getDeviceId())) {
				statusItem.numberOfCompleted++;
				statusItem.percentage += file.getPercentage(getDeviceId());
			} else if (file.hasIssues(getDeviceId())) {
				statusItem.setHasIssues(true);
			}

			statusItem.numberOfTotal++;
			statusItem.bytesTotal += file.size;

			lister.offerObliged(this, file);
		}

		if (storageItem != null)
			storageItem.bytesRequired = statusItem.bytesTotal - statusItem.bytesReceived;
	}

	@Override
	protected GenericTransferItem onGenerateRepresentative(String representativeText)
	{
		return new GenericTransferItem(representativeText);
	}

	@Override
	public boolean onCustomGroupListing(GroupLister<AbstractGenericItem> lister, int mode, AbstractGenericItem object)
	{
		if (mode == MODE_GROUP_BY_DEFAULT)
			lister.offer(object, new GroupEditableTransferObjectMerger(object, this));
		else
			return false;

		return true;
	}

	@Override
	public int compareItems(int sortingCriteria, int sortingOrder, AbstractGenericItem objectOne, AbstractGenericItem objectTwo)
	{
		//if (sortingCriteria == MODE_SORT_BY_DEFAULT)
		//    return MathUtils.compare(objectTwo.requestId, objectOne.requestId);

		return 1;
	}

	@Override
	public GroupLister<AbstractGenericItem> createLister(List<AbstractGenericItem> loadedList, int groupBy)
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

	public boolean setAssignee(ShowingAssignee assignee)
	{
		if (assignee == null) {
			mAssignee = null;
			return true;
		}

		try {
			AppUtils.getDatabase(getContext()).reconstruct(assignee);
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
	public String getRepresentativeText(Merger merger)
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
		if (viewType == VIEW_TYPE_REPRESENTATIVE)
			return new GroupViewHolder(getInflater().inflate(R.layout.layout_list_title, parent, false), R.id.layout_list_title_text);

		return new GroupEditableListAdapter.GroupViewHolder(getInflater().inflate(R.layout.list_transfer, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull final GroupEditableListAdapter.GroupViewHolder holder, int position)
	{
		try {
			final AbstractGenericItem object = getItem(position);

			if (!holder.tryBinding(object)) {
				final View parentView = holder.getView();

				@ColorInt
				int appliedColor;
				int percentage = (int) (object.getPercentage(getDeviceId()) * 100);
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

				if (object.hasComplete(getDeviceId()))
					appliedColor = mColorDone;
				else if (object.hasIssues(getDeviceId()))
					appliedColor = mColorError;
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

				progressBar.setVisibility(!supportThumbnail || !object.hasComplete(getDeviceId())
						? View.VISIBLE
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

	abstract public static class AbstractGenericItem extends TransferObject
			implements GroupEditableListAdapter.GroupEditable
	{
		public int viewType;
		public boolean completed = false;
		public boolean hasIssues = true;
		public double percentage;
		public String representativeText;

		public AbstractGenericItem()
		{
		}

		public AbstractGenericItem(String representativeText)
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
		public void setRepresentativeText(CharSequence representativeText)
		{
			this.representativeText = String.valueOf(representativeText);
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

	public static class GenericTransferItem extends AbstractGenericItem
	{
		private DocumentFile mFile;
		private boolean mSupportThumbnail;

		public GenericTransferItem()
		{
		}

		GenericTransferItem(String representativeText)
		{
			this.viewType = VIEW_TYPE_REPRESENTATIVE;
			setRepresentativeText(representativeText);
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
			imageView.setImageResource(Type.INCOMING.equals(type)
					? R.drawable.ic_arrow_down_white_24dp
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
			if (mFile != null && mSupportThumbnail && mFile.exists()) {
				GlideApp.with(imageView.getContext())
						.load(mFile.getUri())
						.error(getIconRes())
						.override(160)
						.centerCrop()
						.into(imageView);

				return true;
			}

			return false;
		}

		public void setFile(DocumentFile file)
		{
			mFile = file;
		}

		void setSupportThumbnail(boolean support)
		{
			mSupportThumbnail = support;
		}
	}

	public static class TransferFolder extends AbstractGenericItem
	{
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
		public boolean hasIssues(String deviceId)
		{
			return mHasIssues;
		}

		@Override
		public boolean hasOngoing(@Nullable String deviceId)
		{
			return mHasOngoing;
		}

		@Override
		public boolean hasComplete(String deviceId)
		{
			return numberOfTotal == numberOfCompleted && numberOfTotal != 0;
		}

		@Override
		public int getIconRes()
		{
			return R.drawable.ic_folder_white_24dp;
		}

		@Override
		public double getPercentage(String deviceId)
		{
			return bytesTotal == 0 || bytesCompleted == 0 ? 0 : (float) bytesCompleted / bytesTotal;
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
			return adapter.getPercentFormat().format(getPercentage(adapter.getDeviceId()));
		}

		@Override
		public SQLQuery.Select getWhere()
		{
			return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
					.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND ("
									+ AccessDatabase.FIELD_TRANSFER_DIRECTORY + " LIKE ? OR "
									+ AccessDatabase.FIELD_TRANSFER_DIRECTORY + " = ?)",
							String.valueOf(this.groupId),
							this.directory + File.separator + "%",
							this.directory);
		}

		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof TransferFolder
					&& directory != null
					&& directory.equals(((TransferFolder) obj).directory);
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

		void setHasIssues(boolean hasIssues)
		{
			mHasIssues = hasIssues;
		}

		public void setHasOngoing(boolean hasOngoing)
		{
			mHasOngoing = hasOngoing;
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

	public static class StorageStatusItem extends AbstractGenericItem implements StatusItem
	{
		long bytesTotal = 0;
		long bytesFree = 0;
		long bytesRequired = 0;

		@Override
		public boolean hasIssues(String deviceId)
		{
			return bytesFree < bytesRequired && bytesFree != -1;
		}

		@Override
		public boolean hasComplete(String deviceId)
		{
			return bytesFree == -1 || !hasIssues(deviceId);
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
		public double getPercentage(String deviceId)
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
			return adapter.getPercentFormat().format(getPercentage(adapter.getDeviceId()));
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

	public static class GroupEditableTransferObjectMerger extends ComparableMerger<AbstractGenericItem>
	{
		private Type mType;

		GroupEditableTransferObjectMerger(AbstractGenericItem holder, TransferListAdapter adapter)
		{
			if (holder instanceof StatusItem)
				mType = Type.STATUS;
			else if (holder instanceof TransferFolder)
				//mType = holder.hasOngoing(adapter.getDeviceId()) ? Type.FOLDER_ONGOING : Type.FOLDER;
				mType = Type.FOLDER_ONGOING;
			else {
				if (holder.hasIssues(adapter.getDeviceId()))
					mType = Type.FILE_ERROR;
				else if (holder.hasOngoing(adapter.getDeviceId()))
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
		public int compareTo(@NonNull ComparableMerger<AbstractGenericItem> o)
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
