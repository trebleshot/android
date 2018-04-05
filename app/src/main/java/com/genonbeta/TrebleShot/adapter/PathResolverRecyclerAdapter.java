package com.genonbeta.TrebleShot.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/29/17 4:29 PM
 */

abstract public class PathResolverRecyclerAdapter<T> extends RecyclerView.Adapter<PathResolverRecyclerAdapter.Holder>
{
	private ArrayList<Holder.Index<T>> mList = new ArrayList<>();
	private OnClickListener<T> mClickListener;

	@Override
	public Holder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_pathresolver, null));
	}

	@Override
	public void onBindViewHolder(final Holder holder, int position)
	{
		holder.index = mList.get(position);
		holder.text.setText(holder.index.title);
		holder.image.setImageResource(holder.index.imgRes);

		holder.useLeftSpace(position == 0);
		holder.useRightSpace(position == getItemCount() - 1);

		if (mClickListener != null)
			holder.container.setOnClickListener(new View.OnClickListener()
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

	public ArrayList<Holder.Index<T>> getList()
	{
		return mList;
	}

	public void setOnClickListener(OnClickListener<T> clickListener)
	{
		mClickListener = clickListener;
	}

	public static class Holder<E> extends RecyclerView.ViewHolder
	{
		public View container;
		public View leftSpace;
		public View rightSpace;
		public ImageView image;
		public TextView text;
		public Index<E> index;

		private Holder(View view)
		{
			super(view);
			this.container = view;
			this.leftSpace = view.findViewById(R.id.list_pathresolver_left_space);
			this.rightSpace = view.findViewById(R.id.list_pathresolver_right_space);
			this.image = view.findViewById(R.id.list_pathresolver_image);
			this.text = view.findViewById(R.id.list_pathresolver_text);
		}

		public void useLeftSpace(boolean use)
		{
			leftSpace.setVisibility(use ? View.VISIBLE : View.GONE);
		}

		public void useRightSpace(boolean use)
		{
			rightSpace.setVisibility(use ? View.VISIBLE : View.GONE);
		}

		public static class Index<D>
		{
			public String title;
			public int imgRes;
			public D object;

			public Index(String title, int imgRes, D object)
			{
				this.title = title;
				this.imgRes = imgRes;
				this.object = object;
			}

			public Index(String title, D object)
			{
				this(title, R.drawable.ic_keyboard_arrow_right_black_24dp, object);
			}
		}
	}

	public interface OnClickListener<E>
	{
		void onClick(Holder<E> holder);
	}
}
