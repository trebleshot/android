package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ApplicationListAdapter extends AbstractEditableListAdapter<ApplicationListAdapter.AppInfo>
{
	private boolean mShowSysApps = false;
	private PackageManager mManager;
	private ArrayList<AppInfo> mList = new ArrayList<>();
	private Comparator<AppInfo> mComparator = new Comparator<AppInfo>()
	{
		@Override
		public int compare(ApplicationListAdapter.AppInfo compareFrom, ApplicationListAdapter.AppInfo compareTo)
		{
			return compareFrom.label.toLowerCase().compareTo(compareTo.label.toLowerCase());
		}
	};

	public ApplicationListAdapter(Context context, boolean showSystemApps)
	{
		super(context);
		mShowSysApps = showSystemApps;
		mManager = mContext.getPackageManager();
	}

	@Override
	public ArrayList<AppInfo> onLoad()
	{
		ArrayList<AppInfo> list = new ArrayList<>();

		for (PackageInfo packageInfo : mContext.getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA))
		{
			ApplicationInfo appInfo = packageInfo.applicationInfo;

			if (((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1) || mShowSysApps)
			{
				String label = (String) appInfo.loadLabel(mManager);

				if (getSearchWord() == null || (getSearchWord() != null && ApplicationHelper.searchWord(label, getSearchWord())))
					list.add(new AppInfo(appInfo.loadLogo(mManager), label, packageInfo.versionName, appInfo.sourceDir, packageInfo.packageName));
			}
		}

		Collections.sort(list, mComparator);

		return list;
	}

	@Override
	public void onUpdate(ArrayList<AppInfo> passedItem)
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

	@Override
	public View getView(int position, View view, ViewGroup viewGroup)
	{
		if (view == null)
			view = LayoutInflater.from(getContext()).inflate(R.layout.list_application, viewGroup, false);

		AppInfo info = (AppInfo) getItem(position);
		TextView text = (TextView) view.findViewById(R.id.text);
		TextView text2 = (TextView) view.findViewById(R.id.text2);

		text.setText(info.label);
		text2.setText(info.version);

		return view;
	}

	public void showSystemApps(boolean show)
	{
		mShowSysApps = show;
	}

	public static class AppInfo
	{
		public Drawable icon;
		public String label;
		public String version;
		public String codePath;
		public String packageName;

		public AppInfo(Drawable icon, String label, String version, String codePath, String packageName)
		{
			this.icon = icon;
			this.label = label;
			this.version = version;
			this.codePath = codePath;
			this.packageName = packageName;
		}
	}
}
