package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.ConnectionChooserDialog;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.service.CommunicationService;

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

	public static void resumeTransfer(Context context, TransferGroup group, TransferGroup.Assignee assignee)
	{
		AppUtils.startForegroundService(context, new Intent(context, CommunicationService.class)
				.setAction(CommunicationService.ACTION_SEAMLESS_RECEIVE)
				.putExtra(CommunicationService.EXTRA_GROUP_ID, group.groupId)
				.putExtra(CommunicationService.EXTRA_DEVICE_ID, assignee.deviceId));
	}

	public interface ConnectionUpdatedListener
	{
		void onConnectionUpdated(NetworkDevice.Connection connection, TransferGroup.Assignee assignee);
	}
}
