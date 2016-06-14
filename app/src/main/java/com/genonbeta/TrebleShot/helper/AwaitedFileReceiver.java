package com.genonbeta.TrebleShot.helper;

public class AwaitedFileReceiver
{
    public String ip;
    public String fileName;
    public String fileMimeType;
    public int requestId;
    public int acceptId;
    public long fileSize;
    public boolean processCancelled = false;

    public AwaitedFileReceiver(String ip, int requestId, int acceptId, String fileName, long fileSize, String fileMime)
    {
        this.ip = ip;
        this.requestId = requestId;
        this.acceptId = acceptId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileMimeType = fileMime;
    }

    public AwaitedFileReceiver()
    {
    }
}
