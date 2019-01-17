package com.genonbeta.TrebleShot.object;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.exception.AssigneeNotFoundException;
import com.genonbeta.TrebleShot.exception.ConnectionNotFoundException;
import com.genonbeta.TrebleShot.exception.DeviceNotFoundException;
import com.genonbeta.TrebleShot.exception.TransferGroupNotFoundException;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;

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
        buildAll(database, groupId, using, findDevice);
    }

    private TransferInstance()
    {

    }

    protected void buildAll(AccessDatabase database, long groupId, String using, boolean findDevice) throws TransferGroupNotFoundException, DeviceNotFoundException, ConnectionNotFoundException, AssigneeNotFoundException
    {
        buildGroup(database, groupId);

        if (findDevice) {
            buildDevice(database, using);
            buildAssignee(database, mGroup, mDevice);
            buildConnection(database, mAssignee);
        } else {
            buildConnection(database, using);
            buildDevice(database, mConnection.deviceId);
            buildAssignee(database, mGroup, mDevice);
        }

        NetworkDeviceLoader.processConnection(database, getDevice(), getConnection());

        if (!getAssignee().connectionAdapter.equals(getConnection().adapterName)) {
            getAssignee().connectionAdapter = getConnection().adapterName;
            database.publish(getAssignee());
        }
    }

    protected void buildAssignee(AccessDatabase database, TransferGroup group, NetworkDevice device) throws AssigneeNotFoundException
    {
        if (mAssignee != null)
            return;

        try {
            TransferGroup.Assignee assignee = new TransferGroup.Assignee(group, device);

            database.reconstruct(assignee);

            mAssignee = assignee;
        } catch (Exception e) {
            throw new AssigneeNotFoundException();
        }
    }

    protected void buildConnection(AccessDatabase database, String connectionAddress) throws ConnectionNotFoundException
    {
        if (mConnection != null)
            return;

        try {
            NetworkDevice.Connection connection = new NetworkDevice.Connection(connectionAddress);

            database.reconstruct(connection);

            mConnection = connection;
        } catch (Exception e) {
            throw new ConnectionNotFoundException();
        }
    }

    protected void buildConnection(AccessDatabase database, TransferGroup.Assignee assignee) throws ConnectionNotFoundException
    {
        if (mConnection != null)
            return;

        try {
            NetworkDevice.Connection connection = new NetworkDevice.Connection(assignee);

            database.reconstruct(connection);

            mConnection = connection;
        } catch (Exception e) {
            throw new ConnectionNotFoundException();
        }
    }

    protected void buildDevice(AccessDatabase database, String deviceId) throws DeviceNotFoundException
    {
        if (mDevice != null)
            return;

        try {
            NetworkDevice device = new NetworkDevice(deviceId);

            database.reconstruct(device);

            mDevice = device;
        } catch (Exception e) {
            throw new DeviceNotFoundException();
        }
    }

    protected void buildGroup(AccessDatabase database, long groupId) throws TransferGroupNotFoundException
    {
        if (mGroup != null)
            return;

        try {
            TransferGroup group = new TransferGroup(groupId);

            database.reconstruct(group);

            mGroup = group;
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

    public static class Builder
    {
        private TransferInstance mTransferInstance = new TransferInstance();

        public TransferInstance build(AccessDatabase database, long groupId, String using, boolean findDevice) throws AssigneeNotFoundException, DeviceNotFoundException, TransferGroupNotFoundException, ConnectionNotFoundException
        {
            mTransferInstance.buildAll(database, groupId, using, findDevice);
            return mTransferInstance;
        }

        public Builder supply(TransferGroup group)
        {
            mTransferInstance.mGroup = group;
            return this;
        }

        public Builder supply(NetworkDevice device)
        {
            mTransferInstance.mDevice = device;
            return this;
        }

        public Builder supply(NetworkDevice.Connection connection)
        {
            mTransferInstance.mConnection = connection;
            return this;
        }

        public Builder supply(TransferGroup.Assignee assignee)
        {
            mTransferInstance.mAssignee = assignee;
            return this;
        }
    }
}
