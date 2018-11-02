package com.genonbeta.TrebleShot.object;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.exception.AssigneeNotFoundException;
import com.genonbeta.TrebleShot.exception.ConnectionNotFoundException;
import com.genonbeta.TrebleShot.exception.DeviceNotFoundException;
import com.genonbeta.TrebleShot.exception.TransferGroupNotFoundException;

/**
 * created by: Veli
 * date: 9.01.2018 18:40
 */

public class TransferInstance
{
    private NetworkDevice mDevice;
    private TransferGroup mGroup;
    private NetworkDevice.Connection mConnection;
    private TransferGroup.Assignee mAssignee;

    // false means "to find connection first"
    public TransferInstance(AccessDatabase database, long groupId, String using, boolean findDevice) throws TransferGroupNotFoundException, DeviceNotFoundException, ConnectionNotFoundException, AssigneeNotFoundException
    {
        mGroup = buildGroup(database, groupId);

        if (findDevice) {
            mDevice = buildDevice(database, using);
            mAssignee = buildAssignee(database, mGroup, mDevice);
            mConnection = buildConnection(database, mAssignee);
        } else {
            mConnection = new NetworkDevice.Connection(using);

            try {
                database.reconstruct(mConnection);
            } catch (Exception e) {
                throw new ConnectionNotFoundException();
            }

            mDevice = buildDevice(database, mConnection.deviceId);
            mAssignee = buildAssignee(database, mGroup, mDevice);
        }
    }

    protected TransferGroup.Assignee buildAssignee(AccessDatabase database, TransferGroup group, NetworkDevice device) throws AssigneeNotFoundException
    {
        try {
            TransferGroup.Assignee assignee = new TransferGroup.Assignee(group, device);

            database.reconstruct(assignee);

            return assignee;
        } catch (Exception e) {
            throw new AssigneeNotFoundException();
        }
    }

    protected NetworkDevice.Connection buildConnection(AccessDatabase database, TransferGroup.Assignee assignee) throws ConnectionNotFoundException
    {
        try {
            NetworkDevice.Connection connection = new NetworkDevice.Connection(assignee);

            database.reconstruct(connection);

            return connection;
        } catch (Exception e) {
            throw new ConnectionNotFoundException();
        }
    }

    protected NetworkDevice buildDevice(AccessDatabase database, String deviceId) throws DeviceNotFoundException
    {
        try {
            NetworkDevice device = new NetworkDevice(deviceId);

            database.reconstruct(device);

            return device;
        } catch (Exception e) {
            throw new DeviceNotFoundException();
        }
    }

    protected TransferGroup buildGroup(AccessDatabase database, long groupId) throws TransferGroupNotFoundException
    {
        try {
            TransferGroup group = new TransferGroup(groupId);

            database.reconstruct(group);

            return group;
        } catch (Exception e) {
            throw new TransferGroupNotFoundException();
        }
    }


    public TransferGroup.Assignee getAssignee()
    {
        return mAssignee;
    }

    public NetworkDevice.Connection getConnection()
    {
        return mConnection;
    }

    public NetworkDevice getDevice()
    {
        return mDevice;
    }

    public TransferGroup getGroup()
    {
        return mGroup;
    }
}
