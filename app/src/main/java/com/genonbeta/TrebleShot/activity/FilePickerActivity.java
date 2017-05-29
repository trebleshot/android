package com.genonbeta.TrebleShot.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;

import java.io.File;

/**
 * Created by: veli
 * Date: 5/29/17 3:18 PM
 */

public class FilePickerActivity extends Activity
{
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_filepicker);

		RecyclerView recyclerView = (RecyclerView) findViewById(R.id.activity_filepicker_fragment_pathresolver);

		// use this setting to improve performance if you know that changes
		// in content do not change the layout size of the RecyclerView
		recyclerView.setHasFixedSize(true);

		// use a linear layout manager
		LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
		recyclerView.setLayoutManager(layoutManager);

		// specify an adapter (see also next example)
		PathResolverRecyclerAdapter adapter = new PathResolverRecyclerAdapter();
		adapter.goTo(ApplicationHelper.getApplicationDirectory(this));
		recyclerView.setAdapter(adapter);
	}
}
