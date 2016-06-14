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

public class ApplicationListAdapter extends AbstractFlexibleAdapter
{
    private boolean mShowSysApps = false;
    private Context mContext;
    private String mSearchWord;
    private PackageManager mManager;
    private ArrayList<AppInfo> mList = new ArrayList<AppInfo>();
    private ArrayList<AppInfo> mPendingList = new ArrayList<AppInfo>();
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
        this.mShowSysApps = showSystemApps;
        this.mContext = context;
        this.mManager = this.mContext.getPackageManager();
    }

    @Override
    protected void onUpdate()
    {
        this.mPendingList.clear();

        for (PackageInfo packageInfo : this.mContext.getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA))
        {
            ApplicationInfo appInfo = packageInfo.applicationInfo;

            if (((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1) || this.mShowSysApps)
            {
                String label = (String) appInfo.loadLabel(this.mManager);

                if (this.mSearchWord == null || (this.mSearchWord != null && ApplicationHelper.searchWord(label, this.mSearchWord)))
                    this.mPendingList.add(new AppInfo(appInfo.loadLogo(this.mManager), label, packageInfo.versionName, appInfo.sourceDir));
            }
        }

        Collections.sort(this.mPendingList, this.mComparator);
    }

    @Override
    protected void onSearch(String word)
    {
        this.mSearchWord = word;
    }

    @Override
    public int getCount()
    {
        return this.mList.size();
    }

    @Override
    public Object getItem(int position)
    {
        return this.mList.get(position);
    }

    @Override
    public long getItemId(int p1)
    {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup)
    {
        return this.getViewAt(LayoutInflater.from(mContext).inflate(R.layout.list_application, viewGroup, false), position);
    }

    public View getViewAt(View view, int position)
    {
        AppInfo info = (AppInfo) this.getItem(position);
        TextView text = (TextView) view.findViewById(R.id.text);
        TextView text2 = (TextView) view.findViewById(R.id.text2);

        text.setText(info.label);
        text2.setText(info.version);

        return view;
    }

    @Override
    public void notifyDataSetChanged()
    {
        if (mPendingList.size() > 0)
        {
            this.mList.clear();
            this.mList.addAll(this.mPendingList);

            this.mPendingList.clear();
        }

        super.notifyDataSetChanged();
    }

    public void showSystemApps(boolean show)
    {
        this.mShowSysApps = show;
    }

    public static class AppInfo
    {
        public Drawable icon;
        public String label;
        public String version;
        public String codePath;

        public AppInfo(Drawable icon, String label, String version, String codePath)
        {
            this.icon = icon;
            this.label = label;
            this.version = version;
            this.codePath = codePath;
        }
    }
}
