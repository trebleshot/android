package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.RecyclerViewFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;
import java.util.Collections;

/**
 * created by: Veli
 * date: 9.11.2017 23:39
 */

public class TransactionGroupListAdapter
		extends EditableListAdapter<TransactionGroupListAdapter.PreloadedGroup, RecyclerViewAdapter.ViewHolder>
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

	public SQLQuery.Select getSelect()
	{
		return mSelect;
	}

	public TransactionGroupListAdapter setSelect(SQLQuery.Select select)
	{
		if (select != null)
			mSelect = select;

		return this;
	}

	@NonNull
	@Override
	public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		return new RecyclerViewAdapter.ViewHolder(getInflater().inflate(R.layout.list_transaction_group, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position)
	{
		final PreloadedGroup object = getItem(position);
		final View parentView = holder.getView();

		final View selector = parentView.findViewById(R.id.selector);
		final View layoutImage = parentView.findViewById(R.id.layout_image);
		ImageView image = parentView.findViewById(R.id.image);
		TextView text1 = parentView.findViewById(R.id.text);
		TextView text2 = parentView.findViewById(R.id.text2);
		TextView text3 = parentView.findViewById(R.id.text3);

		if (getSelectionConnection() != null) {
			selector.setSelected(object.isSelectableSelected());

			layoutImage.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					getSelectionConnection().setSelected(object);
					selector.setSelected(object.isSelectableSelected());
				}
			});
		}

		if ((object.index.outgoingCount == 0 && object.index.incomingCount == 0)
				|| (object.index.outgoingCount > 0 && object.index.incomingCount > 0))
			image.setImageResource(object.index.outgoingCount > 0
					? R.drawable.ic_compare_arrows_white_24dp
					: R.drawable.ic_error_white_24dp);
		else
			image.setImageResource(object.index.outgoingCount > 0
					? R.drawable.ic_file_upload_black_24dp
					: R.drawable.ic_file_download_black_24dp);

		text1.setText(object.deviceName);
		text2.setText(object.totalFiles);
		text3.setText(object.totalSize);
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
