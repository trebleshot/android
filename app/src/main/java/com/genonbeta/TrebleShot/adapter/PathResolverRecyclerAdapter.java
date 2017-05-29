package com.genonbeta.TrebleShot.adapter;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/29/17 4:29 PM
 */

public class PathResolverRecyclerAdapter extends RecyclerView.Adapter<PathResolverRecyclerAdapter.MyHolder>
{
	public ArrayList<File> mList = new ArrayList<>();

	@Override
	public MyHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		return new MyHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_pathresolver, null));
	}

	@Override
	public void onBindViewHolder(MyHolder holder, int position)
	{
		holder.text.setText(mList.get(position).getName());
	}

	@Override
	public int getItemCount()
	{
		return mList.size();
	}

	public void goTo(File file)
	{
		do
		{
			mList.add(file);
			file = file.getParentFile();
		}
		while(file != null && file.canRead());
	}

	static class MyHolder extends RecyclerView.ViewHolder
	{
		public TextView text;

		private MyHolder(View view)
		{
			super(view);
			this.text = (TextView) view.findViewById(R.id.list_pathresolver_text);
		}
	}
}
