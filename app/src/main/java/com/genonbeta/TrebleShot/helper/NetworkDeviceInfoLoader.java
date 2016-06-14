package com.genonbeta.TrebleShot.helper;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.genonbeta.CoolSocket.CoolCommunication;
import com.genonbeta.TrebleShot.config.AppConfig;

import org.json.JSONObject;

import java.net.Socket;

public class NetworkDeviceInfoLoader
{
    private OnInfoAvaiableListener mOnInfoAvaiableListener;

    public NetworkDeviceInfoLoader(OnInfoAvaiableListener listener)
    {
        setOnInfoAvaiableListener(listener);
    }

    public boolean startLoading(final Context context, final String deviceIp, final boolean dontDeleteSelfIps)
    {
        CoolCommunication.Messenger.send(deviceIp, AppConfig.COMMUNATION_SERVER_PORT, null,
                new JsonResponseHandler()
                {
                    @Override
                    public void onConfigure(CoolCommunication.Messenger.Process process)
                    {
                        super.onConfigure(process);

                        try
                        {
                            Thread.sleep(1500);
                        } catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onJsonMessage(Socket socket, CoolCommunication.Messenger.Process process, JSONObject json)
                    {
                    }

                    @Override
                    public void onResponseAvaiable(String response)
                    {
                        try
                        {
                            Log.d("DeviceInfo", deviceIp + ": " + response);

                            JSONObject json = new JSONObject(response).getJSONObject("deviceInfo");

                            NetworkDevice device = new NetworkDevice(deviceIp, null, null, null);

                            device.brand = json.getString("brand");
                            device.model = json.getString("model");
                            device.user = json.getString("deviceName");

                            if (device.user == null || device.model == null || device.brand == null)
                                return;

                            if (dontDeleteSelfIps == false)
                                if (Build.DISPLAY.equals(json.getString("display")))
                                    return;

                            if (mOnInfoAvaiableListener != null)
                                mOnInfoAvaiableListener.onInfoAvaiable(device);
                        } catch (Exception e)
                        {
                            this.onError(e);
                        }
                    }
                }
        );

        return true;
    }

    public void setOnInfoAvaiableListener(OnInfoAvaiableListener listener)
    {
        mOnInfoAvaiableListener = listener;
    }

    public static interface OnInfoAvaiableListener
    {
        public void onInfoAvaiable(NetworkDevice device);
    }
}
