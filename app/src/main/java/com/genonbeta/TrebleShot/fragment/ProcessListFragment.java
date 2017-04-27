package com.genonbeta.TrebleShot.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.view.ActionMode;

import com.genonbeta.TrebleShot.adapter.AbstractEditableListAdapter;
import com.genonbeta.TrebleShot.adapter.ProcessListAdapter;

/**
 * Created by: veli
 * Date: 4/15/17 12:28 PM
 */

public class ProcessListFragment extends AbstractEditableListFragment
{
	@Override
	protected AbstractEditableListAdapter onAdapter()
	{
		return new ProcessListAdapter(getActivity().getApplicationContext());
	}

	@Override
	protected ActionModeListener onActionModeListener()
	{
		return new ActionListener();
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		setListAdapter(new ProcessListAdapter(getActivity().getApplicationContext()));
	}

	private class ActionListener extends ActionModeListener
	{
		@Override
		public Uri onItemChecked(ActionMode mode, int position, long id, boolean isChecked)
		{
			return null;
		}
	}
}
