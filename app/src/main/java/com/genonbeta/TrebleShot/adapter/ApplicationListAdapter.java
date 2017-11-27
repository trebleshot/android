package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.util.Shareable;
import com.genonbeta.TrebleShot.util.SweetImageLoader;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ApplicationListAdapter
		extends ShareableListAdapter<ApplicationListAdapter.PackageHolder>
		implements SweetImageLoader.Handler<ApplicationListAdapter.PackageHolder, Drawable>
{
	private boolean mShowSysApps = false;
	private PackageManager mManager;
	private ArrayList<PackageHolder> mList = new ArrayList<>();
	private Comparator<PackageHolder> mComparator = new Comparator<PackageHolder>()
	{
		@Override
		public int compare(PackageHolder compareFrom, PackageHolder compareTo)
		{
			return compareFrom.friendlyName.toLowerCase().compareTo(compareTo.friendlyName.toLowerCase());
		}
	};

	public ApplicationListAdapter(Context context, boolean showSystemApps)
	{
		super(context);
		mShowSysApps = showSystemApps;
		mManager = mContext.getPackageManager();
	}

	@Override
	public Drawable onLoadBitmap(PackageHolder object)
	{
		return object.appInfo.loadIcon(mManager);
	}

	@Override
	public ArrayList<PackageHolder> onLoad()
	{
		ArrayList<PackageHolder> list = new ArrayList<>();

		for (PackageInfo packageInfo : mContext.getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA)) {
			ApplicationInfo appInfo = packageInfo.applicationInfo;

			if (((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1) || mShowSysApps)
					list.add(new PackageHolder(String.valueOf(appInfo.loadLabel(mManager)), appInfo, packageInfo.versionName, appInfo.sourceDir, packageInfo.packageName));
		}

		Collections.sort(list, mComparator);

		return list;
	}

	@Override
	public void onUpdate(ArrayList<PackageHolder> passedItem)
	{
		mList.clear();
		mList.addAll(passedItem);
	}

	@Override
	public int getCount()
	{
		return mList.size();
	}

	@Override
	public Object getItem(int position)
	{
		return mList.get(position);
	}

	@Override
	public long getItemId(int p1)
	{
		return 0;
	}

	public ArrayList<PackageHolder> getList()
	{
		return mList;
	}

	@Override
	public View getView(int position, View view, ViewGroup viewGroup)
	{
		if (view == null)
			view = getInflater().inflate(R.layout.list_application, viewGroup, false);

		PackageHolder info = (PackageHolder) getItem(position);
		TextView text1 = view.findViewById(R.id.text);
		TextView text2 = view.findViewById(R.id.text2);
		ImageView image = view.findViewById(R.id.image);

		text1.setText(info.friendlyName);
		text2.setText(info.version);

		SweetImageLoader.load(this, getContext(), image, info);

		return view;
	}

	public void showSystemApps(boolean show)
	{
		mShowSysApps = show;
	}

	public static class PackageHolder extends Shareable
	{
		public ApplicationInfo appInfo;
		public String version;
		public String packageName;

		public PackageHolder(String friendlyName, ApplicationInfo appInfo, String version, String codePath, String packageName)
		{
			super(friendlyName, friendlyName + "_" + version + ".apk", Uri.fromFile(new File(codePath)));

			this.appInfo = appInfo;
			this.version = version;
			this.packageName = packageName;
		}
	}
}
