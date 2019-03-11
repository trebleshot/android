package com.genonbeta.TrebleShot.adapter;

import android.content.Context;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.NetworkDevice;

import java.io.File;

/**
 * created by: veli
 * date: 3/11/19 7:39 PM
 */
public class TransferPathResolverRecyclerAdapter extends PathResolverRecyclerAdapter<String>
{
    private NetworkDevice mDevice;
    private String mHomeName;

    public TransferPathResolverRecyclerAdapter(Context context)
    {
        super(context);
        mHomeName = context.getString(R.string.text_home);
    }

    @Override
    public Holder.Index<String> onFirstItem()
    {
        if (mDevice != null)
            return new Holder.Index<>(mDevice.nickname, R.drawable.ic_device_hub_white_24dp,
                    null);

        return new Holder.Index<>(mHomeName, R.drawable.ic_home_white_24dp, null);
    }

    public void goTo(NetworkDevice device, String[] paths)
    {
        mDevice = device;

        StringBuilder mergedPath = new StringBuilder();
        initAdapter();

        synchronized (getList()) {
            if (paths != null)
                for (String path : paths) {
                    if (path.length() == 0)
                        continue;

                    if (mergedPath.length() > 0)
                        mergedPath.append(File.separator);

                    mergedPath.append(path);

                    getList().add(new Holder.Index<>(path, mergedPath.toString()));
                }
        }
    }
}
