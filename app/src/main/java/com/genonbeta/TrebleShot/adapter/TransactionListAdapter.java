package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.util.MathUtils;
import com.genonbeta.android.framework.util.listing.ComparableMerger;
import com.genonbeta.android.framework.util.listing.Merger;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 4/15/17 12:29 PM
 */

public class TransactionListAdapter
		extends GroupEditableListAdapter<TransactionListAdapter.GroupEditableTransferObject, GroupEditableListAdapter.GroupViewHolder>
		implements GroupEditableListAdapter.GroupLister.CustomGroupListener<TransactionListAdapter.GroupEditableTransferObject>
{
	public static final int MODE_SORT_BY_DEFAULT = MODE_SORT_BY_NAME - 1;
	public static final int MODE_GROUP_BY_DEFAULT = MODE_GROUP_BY_NOTHING + 1;

	private AccessDatabase mDatabase;
	private SQLQuery.Select mSelect;
	private String mPath;
	private long mGroupId;
	private PathChangedListener mListener;
	private NumberFormat mPercentFormat;
	private long totalByte;

	public TransactionListAdapter(Context context, AccessDatabase database)
	{
		super(context, MODE_GROUP_BY_DEFAULT);

		mDatabase = database;
		mPercentFormat = NumberFormat.getPercentInstance();

		setSelect(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER));
	}

	@Override
	protected void onLoad(GroupLister<GroupEditableTransferObject> lister)
	{
		ArrayMap<String, TransferFolder> folders = new ArrayMap<>();
		ArrayList<GroupEditableTransferObject> files = new ArrayList<>();
		String currentPath = getPath();

		ArrayList<GroupEditableTransferObject> derivedList = mDatabase.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
				.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(mGroupId)), GroupEditableTransferObject.class);

		// in an order to keep the database sequences at the least level possible
		// we should gather all the available data and crunch it as needed

		currentPath = currentPath == null || currentPath.length() == 0 ? null : currentPath;

		// we first get the default files
		for (GroupEditableTransferObject object : derivedList) {
			object.directory = object.directory == null || object.directory.length() == 0 ? null : object.directory;

			if (currentPath != null && object.directory == null)
				continue;

			if ((currentPath == null && object.directory == null)
					|| object.directory.equals(currentPath)) {
				files.add(object);
			} else if (currentPath == null
					|| (object.directory != null && object.directory.startsWith(currentPath))) {
				int pathToErase = currentPath == null ? 0 : currentPath.length() + File.separator.length();
				String cleanedPath = object.directory.substring(pathToErase);
				int slashPos = cleanedPath.indexOf(File.separator);

				if (slashPos != -1)
					cleanedPath = cleanedPath.substring(0, slashPos);

				TransferFolder transferFolder = folders.get(cleanedPath);

				if (transferFolder == null) {
					transferFolder = new TransferFolder();

					transferFolder.friendlyName = cleanedPath;
					transferFolder.directory = currentPath != null
							? currentPath + File.separator + cleanedPath
							: cleanedPath;

					folders.put(cleanedPath, transferFolder);
				}

				if (TransferObject.Flag.DONE.equals(object.flag))
					transferFolder.filesReceived++;

				transferFolder.filesTotal++;
			}
		}

		StatusItem statusItem = new StatusItem();

		statusItem.directory = currentPath;
		statusItem.friendlyName = currentPath == null
				? getContext().getString(R.string.text_home)
				: (currentPath.contains(File.separator) ? currentPath.substring(currentPath.lastIndexOf(File.separator) + 1) : currentPath);

		lister.offer(statusItem);

		for (TransferFolder folder : folders.values()) {
			statusItem.filesTotal += folder.filesTotal;
			statusItem.filesReceived += folder.filesReceived;
			lister.offer(folder);
		}

		for (GroupEditableTransferObject file : files) {
			if (TransferObject.Flag.DONE.equals(file.flag))
				statusItem.filesReceived++;

			statusItem.filesTotal++;

			lister.offer(file);
		}
	}

	@Override
	protected GroupEditableTransferObject onGenerateRepresentative(String representativeText)
	{
		return new GroupEditableTransferObject(representativeText);
	}

	@Override
	public boolean onCustomGroupListing(GroupLister<GroupEditableTransferObject> lister, int mode, GroupEditableTransferObject object)
	{
		if (mode == MODE_GROUP_BY_DEFAULT)
			lister.offer(object, new GroupEditableTransferObjectMerger(object));
		else
			return false;

		return true;
	}

	@Override
	public int compareItems(int sortingCriteria, int sortingOrder, GroupEditableTransferObject objectOne, GroupEditableTransferObject objectTwo)
	{
		if (sortingCriteria == MODE_SORT_BY_DEFAULT)
			return MathUtils.compare(objectTwo.requestId, objectOne.requestId);

		return 1;
	}

	public GroupLister<GroupEditableTransferObject> createLister(ArrayList<GroupEditableTransferObject> loadedList, int groupBy)
	{
		return super.createLister(loadedList, groupBy)
				.setCustomLister(this);
	}

	public long getGroupId()
	{
		return mGroupId;
	}

	public String getPath()
	{
		return mPath;
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

	public TransactionListAdapter setSelect(SQLQuery.Select select)
	{
		if (select != null)
			mSelect = select;

		return this;
	}

	public void setGroupId(long groupId)
	{
		mGroupId = groupId;
	}

	public void setPath(String path)
	{
		mPath = path;

		if (mListener != null)
			mListener.onPathChange(path);
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

		return new GroupEditableListAdapter.GroupViewHolder(getInflater().inflate(R.layout.list_transaction, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull final GroupEditableListAdapter.GroupViewHolder holder, int position)
	{
		final GroupEditableTransferObject object = getItem(position);

		if (!holder.tryBinding(object)) {
			final View parentView = holder.getView();

			int appliedColor = R.color.colorAccent;
			ImageView image = parentView.findViewById(R.id.image);
			TextView mainText = parentView.findViewById(R.id.text);
			TextView statusText = parentView.findViewById(R.id.text2);
			TextView sizeText = parentView.findViewById(R.id.text3);

			parentView.setSelected(object.isSelectableSelected());

			if (object instanceof TransferFolder) {
				TransferFolder transferFolder = (TransferFolder) object;

				image.setImageResource(object instanceof StatusItem
						? R.drawable.ic_info_white_24dp
						: R.drawable.ic_folder_black_24dp);
				mainText.setText(object.friendlyName);

				statusText.setText(mPercentFormat.format(transferFolder.getPercent()));
				sizeText.setText(getContext().getString(R.string.text_transferStatusFiles, transferFolder.filesReceived, transferFolder.filesTotal));

				appliedColor = transferFolder.filesReceived == transferFolder.filesTotal
						? R.color.colorAccent : R.color.layoutTintLightColor;
			} else {
				switch (object.flag) {
					case DONE:
						appliedColor = R.color.colorAccent;
						break;
					case REMOVED:
					case INTERRUPTED:
						appliedColor = R.color.errorTintColor;
						break;
					default:
						appliedColor = R.color.layoutTintLightColor;
				}

				boolean isIncoming = object.type.equals(TransferObject.Type.INCOMING);

				image.setImageResource(isIncoming ? R.drawable.ic_file_download_black_24dp : R.drawable.ic_file_upload_black_24dp);
				mainText.setText(object.friendlyName);
				statusText.setText(getContext().getString(TextUtils.getTransactionFlagString(object.flag)).toLowerCase());
				sizeText.setText(FileUtils.sizeExpression(object.fileSize, false));
			}

			mainText.setTextColor(ContextCompat.getColor(getContext(), appliedColor));
		}
	}

	public interface PathChangedListener
	{
		void onPathChange(String path);
	}

	public static class GroupEditableTransferObject
			extends TransferObject
			implements GroupEditableListAdapter.GroupEditable
	{
		public int viewType;
		public String representativeText;

		public GroupEditableTransferObject()
		{

		}

		public GroupEditableTransferObject(String representativeText)
		{
			this.viewType = VIEW_TYPE_REPRESENTATIVE;
			setRepresentativeText(representativeText);
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
			this.fileSize = size;
		}

		@Override
		public void setRepresentativeText(CharSequence representativeText)
		{
			this.representativeText = String.valueOf(representativeText);
		}
	}

	public static class TransferFolder extends GroupEditableTransferObject
	{
		public int filesTotal = 0;
		public int filesReceived = 0;

		public TransferFolder()
		{
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

		public double getPercent()
		{
			return filesReceived == 0 || filesTotal == 0
					? 0 : Integer.valueOf(filesReceived).doubleValue() / Integer.valueOf(filesTotal).doubleValue();
		}

		@Override
		public void setId(long id)
		{
			super.setId(id);
			Log.d(TransactionListAdapter.class.getSimpleName(), "setId(): This method should not be invoked");
		}
	}

	public static class StatusItem extends TransferFolder
	{
		@Override
		public boolean isSelectableSelected()
		{
			return false;
		}

		@Override
		public long getId()
		{
			return (directory != null ? directory : friendlyName).hashCode();
		}

		@Override
		public boolean setSelectableSelected(boolean selected)
		{
			return false;
		}
	}

	public static class GroupEditableTransferObjectMerger extends ComparableMerger<GroupEditableTransferObject>
	{
		private Type mType;

		public GroupEditableTransferObjectMerger(GroupEditableTransferObject holder)
		{
			if (holder instanceof StatusItem)
				mType = Type.STATUS;
			else if (holder instanceof TransferFolder)
				mType = Type.FOLDER;
			else
				mType = Type.FILE;
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
		public int compareTo(@NonNull ComparableMerger<GroupEditableTransferObject> o)
		{
			if (o instanceof GroupEditableTransferObjectMerger)
				return MathUtils.compare(((GroupEditableTransferObjectMerger) o).getType().ordinal(), getType().ordinal());

			return 1;
		}

		public enum Type
		{
			STATUS,
			FOLDER,
			FILE
		}
	}
}
