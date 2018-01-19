package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;

/**
 * created by: Veli
 * date: 9.11.2017 23:39
 */

public class TransactionGroupListAdapter extends EditableListAdapter<TransactionGroupListAdapter.PreloadedGroup>
{
	public static final String FIELD_TRANSACTIONCOUNT = "transactionCount";
	public static final String FIELD_DEVICENAME = "deviceName";

	private AccessDatabase mDatabase;
	private ArrayList<TransactionGroupListAdapter.PreloadedGroup> mList = new ArrayList<>();
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
		ArrayList<PreloadedGroup> list = mDatabase.castQuery(getSelect()
				.setLoadListener(new SQLQuery.Select.LoadListener()
				{
					@Override
					public void onOpen(SQLiteDatabase db, Cursor cursor)
					{

					}

					@Override
					public void onLoad(SQLiteDatabase db, Cursor cursor, CursorItem item)
					{
						int groupId = item.getInt(AccessDatabase.FIELD_TRANSFERGROUP_ID);
						String deviceId = item.getString(AccessDatabase.FIELD_TRANSFERGROUP_DEVICEID);

						NetworkDevice device = new NetworkDevice(deviceId);

						try {
							mDatabase.reconstruct(device);
							item.put(FIELD_DEVICENAME, device.nickname);
						} catch (Exception e) {
							item.put(FIELD_DEVICENAME, "-");
						}

						item.put(FIELD_TRANSACTIONCOUNT, db.getTable(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER, AccessDatabase.FIELD_TRANSFER_TYPE)
								.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(groupId))).size());
					}
				}), PreloadedGroup.class);

		Collections.sort(list, getDefaultComparator());

		return list;
	}

	@Override
	public void onUpdate(ArrayList<PreloadedGroup> passedItem)
	{
		mList.clear();
		mList.addAll(passedItem);
	}

	@Override
	public int getCount()
	{
		return mList.size();
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

	public ArrayList<PreloadedGroup> getList()
	{
		return mList;
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

		String firstLetters = TextUtils.getFirstLetters(group.deviceName, 1);
		TextDrawable drawable = TextDrawable.builder().buildRoundRect(firstLetters.length() > 0 ? firstLetters : "?", ContextCompat.getColor(mContext, R.color.networkDeviceRipple), 100);

		if (getSelectionConnection() != null) {
			selector.setSelected(getSelectionConnection().isSelected(group));

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

		text1.setText(group.deviceName);
		text2.setText(getContext().getResources().getQuantityString(R.plurals.text_files, group.transactionCount, group.transactionCount));
		text3.setText(TimeUtils.getTimeAgo(getContext(), group.dateCreated));
		image.setImageDrawable(drawable);

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
		public int transactionCount;
		public String deviceName;

		@Override
		public void reconstruct(CursorItem item)
		{
			super.reconstruct(item);

			transactionCount = item.getInt(FIELD_TRANSACTIONCOUNT);
			deviceName = item.getString(FIELD_DEVICENAME);
		}

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
			return transactionCount;
		}
	}
}
