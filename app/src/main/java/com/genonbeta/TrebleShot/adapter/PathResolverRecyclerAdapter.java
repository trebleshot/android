package com.genonbeta.TrebleShot.adapter;

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

public class PathResolverRecyclerAdapter extends RecyclerView.Adapter<PathResolverRecyclerAdapter.Holder>
{
	private ArrayList<File> mList = new ArrayList<>();
	private OnClickListener mClickListener;

	@Override
	public Holder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_pathresolver, null));
	}

	@Override
	public void onBindViewHolder(final Holder holder, int position)
	{
		holder.text.setText(mList.get(position).getName());
		holder.file = mList.get(position);

		if (mClickListener != null)
			holder.text.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					mClickListener.onClick(holder);
				}
			});
	}

	@Override
	public int getItemCount()
	{
		return mList.size();
	}

	public void goTo(File file)
	{
		ArrayList<File> list = new ArrayList<>();

		if (file != null)
			do {
				list.add(file);
				file = file.getParentFile();
			}
			while (file != null && file.canWrite());

		mList.clear();

		if (list.size() > 0)
			for (int i = list.size() - 1; i >= 0; i--)
				mList.add(list.get(i));
	}

	public void setOnClickListener(OnClickListener clickListener)
	{
		mClickListener = clickListener;
	}

	public static class Holder extends RecyclerView.ViewHolder
	{
		public TextView text;
		public File file;

		private Holder(View view)
		{
			super(view);
			this.text = (TextView) view.findViewById(R.id.list_pathresolver_text);
		}
	}

	public interface OnClickListener
	{
		void onClick(Holder holder);
	}
}
