package com.genonbeta.TrebleShot.fragment;

import android.content.*;
import android.net.*;
import android.os.*;
import android.support.v4.app.*;
import android.view.*;
import android.widget.*;
import com.genonbeta.TrebleShot.*;
import com.genonbeta.TrebleShot.adapter.*;
import com.genonbeta.TrebleShot.helper.*;
import java.io.*;
import java.util.*;
import android.preference.*;

public class ApplicationListFragment extends AbstractMediaListFragment<ApplicationListAdapter>
{
	@Override
	protected ApplicationListAdapter onAdapter()
	{
		return new ApplicationListAdapter(getActivity(), PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("show_system_apps", false));
	}

	@Override
	protected MediaChoiceListener onChoiceListener()
	{
		return new ChoiceListener();
	}
	
	private class ChoiceListener extends MediaChoiceListener
	{
		@Override
		public void onItemChecked(ActionMode mode, int pos, long id, boolean isChecked)
		{
			ApplicationListAdapter.AppInfo info = (ApplicationListAdapter.AppInfo) getAdapter().getItem(pos);

			if (isChecked)
				mCheckedList.add(Uri.parse("file://" + info.codePath));
			else
				mCheckedList.remove(Uri.parse("file://" + info.codePath));
		}
	}
}
