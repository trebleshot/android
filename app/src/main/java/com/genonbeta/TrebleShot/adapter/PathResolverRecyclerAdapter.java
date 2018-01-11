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
	private ArrayList<Holder.Index> mList = new ArrayList<>();
	private OnClickListener mClickListener;

	@Override
	public Holder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_pathresolver, null));
	}

	@Override
	public void onBindViewHolder(final Holder holder, int position)
	{
		holder.index = mList.get(position);

		if (position == 0 && ".".equals(holder.index.name))
			holder.text.setText(R.string.text_fileRoot);
		else
			holder.text.setText(holder.index.name);

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

	public void goTo(String[] paths)
	{
		mList.clear();

		StringBuilder mergedPath = new StringBuilder();

		if (paths != null)
			for (String path : paths) {
				if (path.length() == 0)
					continue;

				if (mergedPath.length() > 0)
					mergedPath.append(File.separator);

				mergedPath.append(path);

				mList.add(new Holder.Index(path, mergedPath.toString()));
			}
	}

	public void setOnClickListener(OnClickListener clickListener)
	{
		mClickListener = clickListener;
	}

	public static class Holder extends RecyclerView.ViewHolder
	{
		public TextView text;
		public Index index;

		private Holder(View view)
		{
			super(view);
			this.text = view.findViewById(R.id.list_pathresolver_text);
		}

		public static class Index
		{
			public String name;
			public String path;

			public Index(String name, String path)
			{
				this.name = name;
				this.path = path;
			}
		}
	}

	public interface OnClickListener
	{
		void onClick(Holder holder);
	}
}
