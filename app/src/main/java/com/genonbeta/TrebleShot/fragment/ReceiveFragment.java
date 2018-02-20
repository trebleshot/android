package com.genonbeta.TrebleShot.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.util.FABSupport;

/**
 * created by: Veli
 * date: 20.02.2018 14:46
 */

public class ReceiveFragment
		extends Fragment
		implements FABSupport
{
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public boolean onFABRequested(FloatingActionButton floatingActionButton)
	{
		floatingActionButton.setImageResource(R.drawable.ic_arrow_downward_black_24dp);

		floatingActionButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{

			}
		});

		return false;
	}
}
