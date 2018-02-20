package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;
import java.util.Collections;

/**
 * created by: Veli
 * date: 9.11.2017 23:39
 */

public class TransactionGroupListAdapter extends EditableListAdapter<TransactionGroupListAdapter.PreloadedGroup>
{
	private AccessDatabase mDatabase;
	private SQLQuery.Select mSelect;

	public TransactionGroupListAdapter(Context context)
	{
		super(context);

		mDatabase = new AccessDatabase(context);

		setSelect(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP));
	}

	@Override
	public ArrayList<PreloadedGroup> onLoad()
	{
		ArrayList<PreloadedGroup> list = mDatabase.castQuery(getSelect(), PreloadedGroup.class);

		Collections.sort(list, getDefaultComparator());

		for (PreloadedGroup group : list) {
			try {
				NetworkDevice device = new NetworkDevice(group.deviceId);

				mDatabase.reconstruct(device);

				group.deviceName = device.nickname;
			} catch (Exception e) {
				group.deviceName = "-";
			}

			mDatabase.calculateTransactionSize(group.groupId, group.index);

			group.totalCount = group.index.incomingCount + group.index.outgoingCount;
			group.totalBytes = group.index.incoming + group.index.outgoing;

			group.totalFiles = getContext().getResources().getQuantityString(R.plurals.text_files, group.totalCount, group.totalCount);
			group.totalSize = FileUtils.sizeExpression(group.totalBytes, false);
		}

		return list;
	}

	@Override
	public int getCount()
	{
		return getItemList().size();
	}

	@Override
	public Object getItem(int i)
	{
		return getItemList().get(i);
	}

	@Override
	public long getItemId(int i)
	{
		return 0;
	}

	public ArrayList<PreloadedGroup> getList()
	{
		return getItemList();
	}

	public SQLQuery.Select getSelect()
	{
		return mSelect;
	}

	@Override
	public View getView(int i, View convertView, ViewGroup viewGroup)
	{
		if (convertView == null)
			convertView = getInflater().inflate(R.layout.list_transaction_group, viewGroup, false);

		final PreloadedGroup group = (PreloadedGroup) getItem(i);

		final View selector = convertView.findViewById(R.id.selector);
		final View layoutImage = convertView.findViewById(R.id.layout_image);
		ImageView image = convertView.findViewById(R.id.image);
		TextView text1 = convertView.findViewById(R.id.text);
		TextView text2 = convertView.findViewById(R.id.text2);
		TextView text3 = convertView.findViewById(R.id.text3);

		if (getSelectionConnection() != null) {
			selector.setSelected(group.isSelectableSelected());

			layoutImage.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					getSelectionConnection().setSelected(group);
					selector.setSelected(group.isSelectableSelected());
				}
			});
		}

		if ((group.index.outgoingCount == 0 && group.index.incomingCount == 0)
				|| (group.index.outgoingCount > 0 && group.index.incomingCount > 0))
			image.setImageResource(group.index.outgoingCount > 0
					? R.drawable.ic_compare_arrows_white_24dp
					: R.drawable.ic_error_white_24dp);
		else
			image.setImageResource(group.index.outgoingCount > 0
					? R.drawable.ic_file_upload_black_24dp
					: R.drawable.ic_file_download_black_24dp);

		text1.setText(group.deviceName);
		text2.setText(group.totalFiles);
		text3.setText(group.totalSize);

		return convertView;
	}

	public TransactionGroupListAdapter setSelect(SQLQuery.Select select)
	{
		if (select != null)
			mSelect = select;

		return this;
	}

	public static class PreloadedGroup
			extends TransactionObject.Group
			implements Editable
	{
		public Index index = new Index();
		public String deviceName;
		public String totalFiles;
		public String totalSize;

		public int totalCount;
		public long totalBytes;

		@Override
		public String getComparableName()
		{
			return getSelectableFriendlyName();
		}

		@Override
		public long getComparableDate()
		{
			return dateCreated;
		}

		@Override
		public long getComparableSize()
		{
			return totalCount;
		}

		@Override
		public String getSelectableFriendlyName()
		{
			return deviceName + " (" + totalSize + ")";
		}
	}
}
