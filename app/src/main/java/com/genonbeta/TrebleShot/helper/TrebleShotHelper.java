package com.genonbeta.TrebleShot.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class TrebleShotHelper extends SQLiteOpenHelper
{
    public final static String DATABASE_NAME = "OptimizedData";

    public final static String TABLE_DEVICE = "device";
    public final static String TABLE_SENDER = "sender";
    public final static String TABLE_MULTIPLESENDER = "multipleSender";
    public final static String TABLE_RECEIVER = "receiver";

    public final static String DEVICES_IP = "ip";
    public final static String DEVICES_BRAND = "brand";
    public final static String DEVICES_MODEL = "model";
    public final static String DEVICES_USER = "user";
    public final static String DEVICES_ISRESTRICTED = "isRestricted";
    public final static String DEVICES_ISLOCAL = "isLocalAddress";

    public final static String SENDER_IP = "ip";
    public final static String SENDER_PORT = "port";
    public final static String SENDER_REQUESTID = "requestId";
    public final static String SENDER_FILE = "file";
    public final static String SENDER_ISCANCELLED = "isCancelled";

    private Context mContext;

    public TrebleShotHelper(Context context)
    {
        super(context, TrebleShotHelper.DATABASE_NAME, null, 1);

        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase p1)
    {
    }

    @Override
    public void onUpgrade(SQLiteDatabase p1, int p2, int p3)
    {

    }

    public boolean isDeviceExist(String ip)
    {
        return false;
    }

    public boolean putDevice(NetworkDevice device)
    {
        return false;
    }

    public boolean removeDevice(String ip)
    {
        return false;
    }

    public NetworkDevice getDevice(String ip)
    {
        return null;
    }

    public ArrayList<NetworkDevice> getDeviceList()
    {
        return null;
    }

    public boolean removeAllDevices()
    {
        return false;
    }

    public boolean putSender()
    {
        return false;
    }

    public boolean putReceiver()
    {
        return false;
    }

    public boolean removeSender()
    {
        return false;
    }

    public boolean removeReceiver()
    {
        return false;
    }

    public boolean isSenderExist()
    {
        return false;
    }

    public boolean isReceiverExist()
    {
        return false;
    }
}
