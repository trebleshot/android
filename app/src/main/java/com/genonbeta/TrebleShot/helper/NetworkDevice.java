package com.genonbeta.TrebleShot.helper;

public class NetworkDevice {
    public String ip;
    public String brand;
    public String model;
    public String user;
    public boolean isRestricted = false;
    public boolean isLocalAddress = false;

    public NetworkDevice(String ip, String brand, String model, String user) {
        this.ip = ip;
        this.brand = brand;
        this.model = model;
        this.user = user;
    }

    public NetworkDevice() {
    }

    public String toString() {
        return (this.model != null) ? this.model + " - " + this.ip : this.ip;
    }
}
