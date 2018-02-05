package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.android.database.SQLQuery;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by: veli
 * Date: 4/15/17 12:29 PM
 */

public class TransactionListAdapter extends EditableListAdapter<TransactionObject>
{
	private AccessDatabase mDatabase;
	private ArrayList<TransactionObject> mList = new ArrayList<>();
	private SQLQuery.Select mSelect;

	private String mPath;
	private int mGroupId;
	private PathChangedListener mListener;

	public TransactionListAdapter(Context context)
	{
		super(context);
		initialize(context);
		setSelect(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER));
	}

	@Override
	public ArrayList<TransactionObject> onLoad()
	{
		ArrayList<TransactionObject> mergedList = new ArrayList<>();
		String currentPath = getPath();

		for (TransactionObject transactionObject : mDatabase.castQuery(getSelect()
				.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_DIRECTORY + " LIKE ?",
						String.valueOf(getGroupId()),
						currentPath != null ? currentPath + File.separator + "%" : "%")
				.setGroupBy(AccessDatabase.FIELD_TRANSFER_DIRECTORY), TransactionObject.class)) {
			String cleanedName = currentPath != null ? transactionObject.directory.substring(currentPath.length() + File.separator.length()) : transactionObject.directory;
			int obtainSlash = cleanedName.indexOf(File.separator);

			if (obtainSlash != -1)
				cleanedName = cleanedName.substring(0, obtainSlash);

			TransactionFolder transactionFolder = new TransactionFolder();

			transactionFolder.friendlyName = cleanedName;
			transactionFolder.directory = currentPath != null ? currentPath + File.separator + cleanedName : cleanedName;

			boolean addThis = true;

			for (TransactionObject testObject : mergedList) {
				if (!(testObject instanceof TransactionFolder))
					continue;

				TransactionFolder testFolder = (TransactionFolder) testObject;

				if (testFolder.friendlyName.equals(transactionFolder.friendlyName)) {
					addThis = false;
					break;
				}
			}

			if (addThis)
				mergedList.add(transactionFolder);
		}

		ArrayList<TransactionObject> mainItems = mDatabase.castQuery((currentPath == null
				? getSelect().setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_DIRECTORY + " IS NULL", String.valueOf(getGroupId()))
				: getSelect().setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_DIRECTORY + "=?",
				String.valueOf(getGroupId()), currentPath)
		).setGroupBy(null), TransactionObject.class);

		Collections.sort(mainItems, getDefaultComparator());

		mergedList.addAll(mainItems);

		return mergedList;
	}

	@Override
	public void onUpdate(ArrayList<TransactionObject> passedItem)
	{
		mList.clear();
		mList.addAll(passedItem);
	}

	private void initialize(Context context)
	{
		mDatabase = new AccessDatabase(context);
	}

	@Override
	public int getCount()
	{
		return mList.size();
	}

	public int getGroupId()
	{
		return mGroupId;
	}

	@Override
	public Object getItem(int i)
	{
		return mList.get(i);
	}

	@Override
	public long getItemId(int i)
	{
		return 0;
	}

	public ArrayList<TransactionObject> getList()
	{
		return mList;
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

	public String getPath()
	{
		return mPath;
	}

	@Override
	public View getView(int i, View convertView, ViewGroup viewGroup)
	{
		if (convertView == null)
			convertView = getInflater().inflate(R.layout.list_transaction, viewGroup, false);

		final TransactionObject transactionObject = (TransactionObject) getItem(i);

		final View selector = convertView.findViewById(R.id.selector);
		final View layoutImage = convertView.findViewById(R.id.layout_image);
		ImageView image = convertView.findViewById(R.id.image);
		TextView mainText = convertView.findViewById(R.id.text);
		TextView statusText = convertView.findViewById(R.id.text2);
		TextView sizeText = convertView.findViewById(R.id.text3);

		if (getSelectionConnection() != null) {
			selector.setSelected(getSelectionConnection().isSelected(transactionObject));

			layoutImage.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					getSelectionConnection().setSelected(transactionObject);
					selector.setSelected(transactionObject.isSelectableSelected());
				}
			});
		}

		if (transactionObject instanceof TransactionFolder) {
			image.setImageResource(R.drawable.ic_folder_black_24dp);
			mainText.setText(transactionObject.friendlyName);
			statusText.setText(R.string.text_folder);
			sizeText.setText(null);
		} else {
			boolean isIncoming = transactionObject.type.equals(TransactionObject.Type.INCOMING);

			image.setImageResource(isIncoming ? R.drawable.ic_file_download_black_24dp : R.drawable.ic_file_upload_black_24dp);
			mainText.setText(transactionObject.friendlyName);
			statusText.setText(getContext().getString(TextUtils.getTransactionFlagString(transactionObject.flag)).toLowerCase());
			sizeText.setText(FileUtils.sizeExpression(transactionObject.fileSize, false));
		}

		return convertView;
	}

	public void setGroupId(int groupId)
	{
		this.mGroupId = groupId;
	}

	public void setPath(String path)
	{
		mPath = path;

		if (mListener != null)
			mListener.onPathChange(path);
	}

	public void setPathChangedListener(PathChangedListener listener)
	{
		this.mListener = listener;
	}

	public interface PathChangedListener
	{
		public void onPathChange(String path);
	}

	public static class TransactionFolder extends TransactionObject
	{
		public TransactionFolder()
		{
		}

		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof TransactionFolder
					&& directory != null
					&& directory.equals(((TransactionFolder) obj).directory);
		}
	}
}
