package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
import com.genonbeta.TrebleShot.helper.AwaitedFileSender;

import java.util.ArrayList;
import java.util.HashSet;

public class PendingProcessListAdapter extends BaseAdapter
{
	private Context mContext;
	private String mIp;
	private ArrayList<ItemHolder> mList = new ArrayList<ItemHolder>();

	public PendingProcessListAdapter(Context context, String forIp)
	{
		this.mContext = context;
		this.mIp = forIp;

		loadList();
	}

	public void loadList()
	{
		mList.clear();

		for (AwaitedFileSender sender : ApplicationHelper.getSenders().values())
		{
			if (this.mIp != null && !sender.ip.equals(this.mIp))
				continue;	
				
			addToList(sender);
		}

		for (AwaitedFileReceiver receiver : ApplicationHelper.getPendingReceivers())
		{
			if (this.mIp != null && !receiver.ip.equals(this.mIp))
				continue;
			
			addToList(receiver);
		}
		
		for (AwaitedFileReceiver receiver : ApplicationHelper.getReceivers())
		{
			if (this.mIp != null && !receiver.ip.equals(this.mIp))
				continue;

			addToList(receiver);
		}
	}
	
	private void addToList(AwaitedFileReceiver r)
	{
		ItemHolder holder = new ItemHolder();
		
		holder.file = r.fileName;
			
		holder.type = true;
		holder.deviceIp = r.ip;
		
		mList.add(holder);
	}
	
	private void addToList(AwaitedFileSender s)
	{
		ItemHolder holder = new ItemHolder();

		holder.file = s.file.getName();

		holder.type = false;
		holder.deviceIp = s.ip;

		mList.add(holder);
	}

	public void clearQueue()
	{
		HashSet<Integer> keyList = new HashSet<Integer>();

		for (int key : ApplicationHelper.getSenders().keySet())
		{
			AwaitedFileSender sender = ApplicationHelper.getSenders().get(key);

			if (sender.ip.equals(this.mIp))
				keyList.add(key);
		}

		for (int currentNumber : keyList)
			ApplicationHelper.getSenders().remove(currentNumber);
			
		for (AwaitedFileReceiver receiver : ApplicationHelper.getReceivers())
		{
			if (receiver.ip.equals(this.mIp))
				ApplicationHelper.getReceivers().remove(receiver);
		}
		
		for (AwaitedFileReceiver receiver : ApplicationHelper.getPendingReceivers())
		{
			if (receiver.ip.equals(this.mIp))
				ApplicationHelper.getPendingReceivers().remove(receiver);
		}
	}

	@Override
	public void notifyDataSetChanged()
	{
		loadList();
		super.notifyDataSetChanged();
	}

	@Override
	public int getCount()
	{
		return mList.size();
	}

	@Override
	public Object getItem(int itemId)
	{
		return mList.get(itemId);
	}

	@Override
	public long getItemId(int p1)
	{
		return 0;
	}

	@Override
	public View getView(int position, View view, ViewGroup viewGroup)
	{
		return getViewAt(LayoutInflater.from(mContext).inflate(R.layout.list_pending_queue, viewGroup, false), position);
	}

	public View getViewAt(View view, int position)
	{
		TextView filenameText = (TextView) view.findViewById(R.id.pending_queue_list_filename);
		TextView processTypeText = (TextView) view.findViewById(R.id.pending_queue_list_process_type_text);

		ItemHolder item = (ItemHolder) getItem(position);
		
		filenameText.setText(item.file);
		processTypeText.setText((!item.type) ? R.string.send : R.string.receive);

		return view;
	}
	
	private class ItemHolder 
	{
		public String file;
		public String deviceIp;
		public boolean type = false; // send, receive
	}
}
