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
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.SweetImageLoader;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class ApplicationListAdapter
		extends ShareableListAdapter<ApplicationListAdapter.PackageHolder>
		implements SweetImageLoader.Handler<ApplicationListAdapter.PackageHolder, Drawable>
{
	private boolean mShowSysApps = false;
	private PackageManager mManager;

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

			if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1 || mShowSysApps)
				list.add(new PackageHolder(String.valueOf(appInfo.loadLabel(mManager)),
						appInfo,
						packageInfo.versionName,
						packageInfo.packageName,
						new File(appInfo.sourceDir)));
		}

		Collections.sort(list, getDefaultComparator());

		return list;
	}

	@Override
	public int getCount()
	{
		return getItemList().size();
	}

	@Override
	public Object getItem(int position)
	{
		return getItemList().get(position);
	}

	@Override
	public long getItemId(int p1)
	{
		return 0;
	}

	public ArrayList<PackageHolder> getList()
	{
		return getItemList();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup viewGroup)
	{
		if (convertView == null)
			convertView = getInflater().inflate(R.layout.list_application, viewGroup, false);

		final PackageHolder holder = (PackageHolder) getItem(position);

		final View selector = convertView.findViewById(R.id.selector);
		final View layoutImage = convertView.findViewById(R.id.layout_image);
		ImageView image = convertView.findViewById(R.id.image);
		TextView text1 = convertView.findViewById(R.id.text);
		TextView text2 = convertView.findViewById(R.id.text2);

		text1.setText(holder.friendlyName);
		text2.setText(holder.version);

		if (getSelectionConnection() != null) {
			selector.setSelected(holder.isSelectableSelected());

			layoutImage.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					getSelectionConnection().setSelected(holder);
					selector.setSelected(holder.isSelectableSelected());
				}
			});
		}

		SweetImageLoader.load(this, getContext(), image, holder);

		return convertView;
	}

	public void showSystemApps(boolean show)
	{
		mShowSysApps = show;
	}

	public static class PackageHolder extends Shareable
	{
		public static final String FORMAT = ".apk";
		public static final String MIME_TYPE = FileUtils.getFileContentType(FORMAT);

		public ApplicationInfo appInfo;
		public String version;
		public String packageName;

		public PackageHolder(String friendlyName, ApplicationInfo appInfo, String version, String packageName, File executableFile)
		{
			super(friendlyName,
					friendlyName + "_" + version + ".apk",
					MIME_TYPE,
					executableFile.lastModified(),
					executableFile.length(),
					Uri.fromFile(executableFile));

			this.appInfo = appInfo;
			this.version = version;
			this.packageName = packageName;
		}
	}
}
