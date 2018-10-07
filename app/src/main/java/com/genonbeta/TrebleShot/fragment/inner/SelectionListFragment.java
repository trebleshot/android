package com.genonbeta.TrebleShot.fragment.inner;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.SmartFragmentPagerAdapter;
import com.genonbeta.TrebleShot.ui.callback.IconSupport;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment;
import com.genonbeta.android.framework.object.Selectable;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * created by: veli
 * date: 9/3/18 10:17 PM
 */
public class SelectionListFragment
		extends DynamicRecyclerViewFragment<Selectable, RecyclerViewAdapter.ViewHolder, SelectionListFragment.MyAdapter>
		implements IconSupport, TitleSupport, SmartFragmentPagerAdapter.ShowingChangeListener
{
	private WeakReference<ReadyLoadListener> mListener;
	private SmartFragmentPagerAdapter.TabSelectionOracle mTabSelectionOracle = new SmartFragmentPagerAdapter.TabSelectionOracle(this);

	@Override
	public MyAdapter onAdapter()
	{
		return new MyAdapter(getContext());
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.actions_selection_list, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.actions_selection_list_check_all)
			updateSelection(true);
		else if (id == R.id.actions_selection_list_undo_all)
			updateSelection(false);
		else
			return super.onOptionsItemSelected(item);

		return true;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		setEmptyImage(R.drawable.ic_insert_drive_file_white_24dp);
		setEmptyText(getString(R.string.text_listEmpty));

		useEmptyActionButton(getString(R.string.butn_refresh), new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				refreshList();
			}
		});
	}

	@Override
	public void onResume()
	{
		super.onResume();
		mTabSelectionOracle.cycle(true);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		mTabSelectionOracle.cycle(false);
	}

	@Override
	public void onNotifyShowingChange()
	{
		if (mTabSelectionOracle.isResuming())
			refreshList();
	}

	@DrawableRes
	@Override
	public int getIconRes()
	{
		return R.drawable.ic_insert_drive_file_white_24dp;
	}

	@Override
	public SmartFragmentPagerAdapter.TabSelectionOracle getTabSelectionOracle()
	{
		return mTabSelectionOracle;
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.butn_previewSelections);
	}

	protected boolean loadIfReady()
	{
		if (getAdapter() != null) {
			ReadyLoadListener listener = mListener != null && mListener.get() != null
					? mListener.get()
					: getActivity() != null && getActivity() instanceof ReadyLoadListener ? (ReadyLoadListener) getActivity() : null;

			if (listener != null) {
				getAdapter().load(listener.onSelectionReadyLoad());
				return true;
			}
		}

		return false;
	}

	@Override
	public void refreshList()
	{
		if (loadIfReady())
			super.refreshList();
	}

	public void setReadyLoadListener(ReadyLoadListener listener)
	{
		mListener = new WeakReference<>(listener);
	}

	public boolean updateSelection(boolean selectAll)
	{
		if (getAdapter() != null) {
			synchronized (getAdapter().getList()) {
				for (Selectable selectable : getAdapter().getList())
					selectable.setSelectableSelected(selectAll);
			}

			getAdapter().notifyDataSetChanged();
			return true;
		}

		return false;
	}

	public static class MyAdapter extends RecyclerViewAdapter<Selectable, RecyclerViewAdapter.ViewHolder>
	{
		final private ArrayList<Selectable> mList = new ArrayList<>();
		final private ArrayList<Selectable> mPendingList = new ArrayList<>();

		public MyAdapter(Context context)
		{
			super(context);
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
		{
			final ViewHolder holder = new ViewHolder(getInflater().inflate(R.layout.list_selection_editor, parent, false));
			final AppCompatCheckBox checkBox = holder.getView().findViewById(R.id.checkbox);

			holder.getView().setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					checkReversed(checkBox, getList().get(holder.getAdapterPosition()));
				}
			});

			checkBox.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					checkReversed(checkBox, getList().get(holder.getAdapterPosition()));
				}
			});

			return holder;
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position)
		{
			final Selectable selectable = getList().get(position);
			final AppCompatCheckBox checkBox = holder.getView().findViewById(R.id.checkbox);
			final TextView text1 = holder.getView().findViewById(R.id.text);

			text1.setText(selectable.getSelectableTitle());
			checkBox.setChecked(selectable.isSelectableSelected());
		}

		@Override
		public int getItemCount()
		{
			return mList.size();
		}

		@Override
		public ArrayList<Selectable> onLoad()
		{
			ArrayList<Selectable> selectableList = new ArrayList<>(mPendingList);
			mPendingList.clear();

			return selectableList;
		}

		@Override
		public void onUpdate(ArrayList<Selectable> passedItem)
		{
			synchronized (getList()) {
				mList.clear();
				mList.addAll(passedItem);
			}
		}

		@Override
		public ArrayList<Selectable> getList()
		{
			return mList;
		}

		public void checkReversed(AppCompatCheckBox checkBox, Selectable selectable)
		{
			if (selectable.setSelectableSelected(!selectable.isSelectableSelected()))
				checkBox.setChecked(selectable.isSelectableSelected());
		}

		protected void load(ArrayList<? extends Selectable> selectableList)
		{
			if (selectableList == null)
				return;

			synchronized (mPendingList) {
				mPendingList.clear();
				mPendingList.addAll(selectableList);
			}
		}
	}

	public interface ReadyLoadListener
	{
		ArrayList<? extends Selectable> onSelectionReadyLoad();
	}
}
