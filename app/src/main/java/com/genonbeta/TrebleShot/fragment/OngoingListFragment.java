package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.net.Uri;
import android.view.ActionMode;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.OngoingListAdapter;
import com.genonbeta.TrebleShot.support.FragmentTitle;

public class OngoingListFragment extends AbstractEditableListFragment<OngoingListAdapter> implements FragmentTitle
{
	@Override
	protected OngoingListAdapter onAdapter()
	{
		return new OngoingListAdapter(getActivity());
	}

	@Override
	protected ActionModeListener onActionModeListener()
	{
		return new ChoiceListener();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
	}

	@Override
	public CharSequence getFragmentTitle(Context context)
	{
		return context.getString(R.string.ongoing_process);
	}

	private class ChoiceListener extends ActionModeListener
	{
		public Uri onItemChecked(ActionMode mode, int pos, long id, boolean isChecked)
		{
			getAdapter().getItem(pos);

			return null;
		}
	}
}
