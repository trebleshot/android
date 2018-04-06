package com.genonbeta.TrebleShot.util;

import android.support.v4.app.FragmentActivity;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.ConnectionChooserDialog;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;

import java.util.ArrayList;

/**
 * created by: veli
 * date: 06.04.2018 17:01
 */
public class TransferUtils
{
	public static void changeConnection(FragmentActivity activity, final AccessDatabase database, final TransferGroup group, final NetworkDevice device, final ConnectionUpdatedListener listener)
	{
		new ConnectionChooserDialog(activity, database, device, new ConnectionChooserDialog.OnDeviceSelectedListener()
		{
			@Override
			public void onDeviceSelected(NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> connectionList)
			{
				TransferGroup.Assignee assignee = new TransferGroup.Assignee(group, device, connection);

				database.publish(assignee);

				if (listener != null)
					listener.onConnectionUpdated(connection, assignee);
			}
		}, false).show();
	}

	public interface ConnectionUpdatedListener
	{
		void onConnectionUpdated(NetworkDevice.Connection connection, TransferGroup.Assignee assignee);
	}
}
