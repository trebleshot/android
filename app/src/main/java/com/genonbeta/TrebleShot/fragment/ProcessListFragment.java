package com.genonbeta.TrebleShot.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;

import com.genonbeta.TrebleShot.adapter.ProcessListAdapter;

/**
 * Created by: veli
 * Date: 4/15/17 12:28 PM
 */

public class ProcessListFragment extends ListFragment
{
	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		setListAdapter(new ProcessListAdapter(getActivity().getApplicationContext()));
	}
}
