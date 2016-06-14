package com.genonbeta.CoolSocket;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public abstract class CoolCommunication extends CoolSocket
{
    public CoolCommunication()
    {
    }

    public CoolCommunication(int port)
    {
        super(port);
    }

    public CoolCommunication(String address, int port)
    {
        super(address, port);
    }

    @Override
    protected void onPacketReceived(Socket socket)
    {
        try
        {
            PrintWriter writer = this.getStreamWriter(socket.getOutputStream());
            String message = this.readStreamMessage(socket.getInputStream());

            this.onMessage(socket, message, writer, socket.getInetAddress().isAnyLocalAddress() ? "127.0.0.1" : socket.getInetAddress().getHostAddress());

            writer.append(CoolSocket.END_SEQUENCE);
            writer.flush();

            socket.close();
        } catch (IOException e)
        {
            this.onError(e);
        }
    }

    abstract protected void onMessage(Socket socket, String message, PrintWriter writer, String clientIp);

    public static class Messenger
    {
        public static final int NO_TIMEOUT = -1;

        public static void send(String socketHost, int socketPort, String message, ResponseHandler handler)
        {
            send(new InetSocketAddress(socketHost, socketPort), message, handler);
        }

        public static void send(InetSocketAddress address, String message, ResponseHandler handler)
        {
            SenderRunnable runnable = new SenderRunnable(address, message, handler);
            new Thread(runnable).start();
        }

        public static boolean sendOnCurrentThread(String socketHost, int socketPort, String message, ResponseHandler handler)
        {
            return sendOnCurrentThread(new InetSocketAddress(socketHost, socketPort), message, handler);
        }

        public static boolean sendOnCurrentThread(InetSocketAddress address, String message, ResponseHandler handler)
        {
            SenderRunnable runnable = new SenderRunnable(address, message, handler);

            return runnable.runProcess();
        }

        private static class SenderRunnable implements Runnable
        {
            private Process mProcess;

            public SenderRunnable(SocketAddress address, String message, ResponseHandler handler)
            {
                this.mProcess = new Process(address, message, handler);
            }

            @Override
            public void run()
            {
                runProcess();
            }

            public boolean runProcess()
            {
                if (this.mProcess.getResponseHandler() != null)
                    this.mProcess.getResponseHandler().onConfigure(this.mProcess);

                Socket socket = new Socket();

                try
                {
                    socket.bind(null);
                    socket.connect(this.mProcess.getSocketAddress());

                    if (this.mProcess.getSocketTimeout() != NO_TIMEOUT)
                        socket.setSoTimeout(this.mProcess.getSocketTimeout());

                    PrintWriter writer = getStreamWriter(socket.getOutputStream());

                    this.mProcess.setSocket(socket);
                    this.mProcess.setWriter(writer);

                    if (this.mProcess.getMessage() != null)
                        writer.append(this.mProcess.getMessage());

                    if (this.mProcess.getResponseHandler() != null)
                        this.mProcess.getResponseHandler().onMessage(socket, mProcess, writer);

                    this.mProcess.waitForResponse();

                    if (this.mProcess.getResponseHandler() != null)
                        this.mProcess.getResponseHandler().onResponseAvaiable(this.mProcess.getResponse());

                    return true;
                } catch (IOException e)
                {
                    if (this.mProcess.getResponseHandler() != null)
                        this.mProcess.getResponseHandler().onError(e);
                } finally
                {
                    if (this.mProcess.getResponseHandler() != null)
                        this.mProcess.getResponseHandler().onFinal();
                }

                return false;
            }
        }

        public static class Process
        {
            private SocketAddress mAddress;
            private ResponseHandler mHandler;
            private String mMessage;
            private String mResponse;
            private PrintWriter mWriter;
            private Socket mSocket;
            private Object mPutLater;

            private boolean mResponseReceived = false;
            private boolean mIsFlushRequested = false;
            private int mSocketTimeout = Messenger.NO_TIMEOUT;

            public Process(SocketAddress address, String message, ResponseHandler handler)
            {
                this.mAddress = address;
                this.mMessage = message;
                this.mHandler = handler;
            }

            public String getMessage()
            {
                return this.mMessage;
            }

            public Object getPutLater()
            {
                return this.mPutLater;
            }

            public PrintWriter getPrintWriter()
            {
                return this.mWriter;
            }

            public String getResponse()
            {
                return this.mResponse;
            }

            public ResponseHandler getResponseHandler()
            {
                return this.mHandler;
            }

            public Socket getSocket()
            {
                return this.mSocket;
            }

            public SocketAddress getSocketAddress()
            {
                return this.mAddress;
            }

            public int getSocketTimeout()
            {
                return this.mSocketTimeout;
            }

            public boolean isResponseReceived()
            {
                return this.mResponseReceived;
            }

            public boolean isFlushRequested()
            {
                return this.mIsFlushRequested;
            }

            public void putLater(Object object)
            {
                this.mPutLater = object;
            }

            public boolean requestFlush()
            {
                if (!this.isFlushRequested() && this.getPrintWriter() != null)
                {
                    this.mIsFlushRequested = true;

                    if (this.getPutLater() != null)
                    {
                        this.getPrintWriter().append(this.getPutLater().toString());
                        this.mPutLater = null;
                    }

                    this.getPrintWriter().append(CoolSocket.END_SEQUENCE);
                    this.getPrintWriter().flush();

                    try
                    {
                        this.setResponseReceived(readStreamMessage(this.getSocket().getInputStream()));
                    } catch (IOException e)
                    {
                        return false;
                    }

                    return true;
                }

                return false;
            }

            public void setResponseReceived(String response)
            {
                this.mResponse = response;
                this.mResponseReceived = true;
            }

            public void setSocket(Socket socket)
            {
                this.mSocket = socket;
            }

            public void setSocketTimeout(int timeout)
            {
                this.mSocketTimeout = timeout;
            }

            public void setWriter(PrintWriter writer)
            {
                this.mWriter = writer;
            }

            public String waitForResponse()
            {
                long timeStart = System.currentTimeMillis();

                if (this.requestFlush())
                {
                    while (!this.isResponseReceived() && (this.getSocketTimeout() == Messenger.NO_TIMEOUT || (System.currentTimeMillis() - timeStart) < this.getSocketTimeout()))
                    {
                    }
                }

                return this.getResponse();
            }
        }

        public static class ResponseHandler
        {
            public void onConfigure(Process process)
            {
            }

            public void onMessage(Socket socket, Process process, PrintWriter response)
            {
            }

            public void onResponseAvaiable(String response)
            {
            }

            public void onError(Exception exception)
            {
            }

            public void onFinal()
            {
            }
        }
    }
}
