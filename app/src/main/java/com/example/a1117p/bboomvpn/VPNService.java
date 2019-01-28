/*
 *  Copyright 2016 Lipi C.H. Lee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.a1117p.bboomvpn;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import com.example.a1117p.bboomvpn.socket.IProtectSocket;
import com.example.a1117p.bboomvpn.socket.IReceivePacket;
import com.example.a1117p.bboomvpn.socket.SocketDataPublisher;
import com.example.a1117p.bboomvpn.socket.SocketNIODataService;
import com.example.a1117p.bboomvpn.socket.SocketProtector;
import com.example.a1117p.bboomvpn.transport.tcp.PacketHeaderException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class VPNService extends VpnService implements Handler.Callback,
        Runnable, IProtectSocket, IReceivePacket {
    private static final String TAG = "ToySharkVPNService";
    private static final int MAX_PACKET_LEN = 1500;
    public static boolean isRunning = false;
    private Handler mHandler;
    private Thread mThread;
    private ParcelFileDescriptor mInterface;
    private boolean serviceValid;
    private SocketNIODataService dataService;
    private Thread dataServiceThread;
    private SocketDataPublisher packetbgWriter;
    private Thread packetQueueThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Log.d(TAG, "onStartCommand");
        if (intent == null) {
            return START_STICKY;
        }
        if (!intent.getBooleanExtra("start", true)) {
            stopVPN();
            return START_STICKY;
        }
        isRunning = true;

        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }

        // Stop the previous session by interrupting the thread.
        if (mThread != null) {
            mThread.interrupt();
            int reps = 0;
            while (mThread.isAlive()) {
                // Log.i(TAG, "Waiting to exit " + ++reps);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // Start a new session by creating a new thread.
        mThread = new Thread(this, "VPNThread");
        mThread.start();
        return START_STICKY;
    }


    private void unregisterAnalyzerCloseCmdReceiver() {
        // Log.d(TAG, "inside unregisterAnalyzerCloseCmdReceiver()");
        try {
            if (serviceCloseCmdReceiver != null) {
                unregisterReceiver(serviceCloseCmdReceiver);
                serviceCloseCmdReceiver = null;
                // Log.d(TAG, "successfully unregistered serviceCloseCmdReceiver");
            }
        } catch (Exception e) {
            // Log.d(TAG, "Ignoring exception in serviceCloseCmdReceiver", e);
        }
    }

    @Override
    public void onRevoke() {
        stopVPN();
        // Log.i(TAG, "revoked!, user has turned off VPN");
        super.onRevoke();
    }

    void stopVPN() {

        isRunning = false;
        serviceValid = false;

        unregisterAnalyzerCloseCmdReceiver();

        if (dataService != null) {
            dataService.setShutdown(true);
            dataService = null;
        }
        if (packetbgWriter != null) {
            packetbgWriter.setShuttingDown(true);
            packetbgWriter = null;
        }
        //	closeTraceFiles();

        if (dataServiceThread != null) {
            dataServiceThread.interrupt();
            dataService = null;
        }
        if (packetQueueThread != null) {
            packetQueueThread.interrupt();
            packetQueueThread = null;
        }

        try {
            if (mInterface != null) {
                // Log.i(TAG, "mInterface.close()");
                mInterface.close();
                mInterface = null;
            }
        } catch (IOException e) {
            // Log.d(TAG, "mInterface.close():" + e.getMessage());
            e.printStackTrace();
        }

        // Stop the previous session by interrupting the thread.
        if (mThread != null) {
            mThread.interrupt();
            int reps = 0;
            while (mThread.isAlive()) {
                // Log.i(TAG, "Waiting to exit " + ++reps);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (reps > 5) {
                    break;
                }
            }
            mThread = null;
        }
        System.gc();
    }

    /**
     * receive message to trigger termination of collection
     */
    private BroadcastReceiver serviceCloseCmdReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            // Log.d(TAG, "received service close cmd intent at " + System.currentTimeMillis());
            unregisterAnalyzerCloseCmdReceiver();
            serviceValid = false;
            stopSelf();
        }
    };

    @Override
    public ComponentName startService(Intent service) {
        // Log.i(TAG, "startService(...)");
        return super.startService(service);
    }


    @Override
    public boolean stopService(Intent name) {
        // Log.i(TAG, "stopService(...)");

        serviceValid = false;
        //	closeTraceFiles();
        return super.stopService(name);
    }

    @Override
    public void protectSocket(Socket socket) {
        this.protect(socket);
    }

    @Override
    public void protectSocket(int socket) {
        this.protect(socket);
    }

    /**
     * called back from background thread when new packet arrived
     */

    /**
     * Close the packet trace file
     */


    /**
     * onDestroy is invoked when user disconnects the VPN
     */
    @Override
    public void onDestroy() {
        // Log.i(TAG, "onDestroy()");
        stopVPN();

    }

    @Override
    public void run() {
        // Log.i(TAG, "running vpnService");
        SocketProtector protector = SocketProtector.getInstance();
        protector.setProtector(this);

        try {
            if (startVpnService()) {
                starVpn();
            } else {
                // Log.e(TAG, "Failed to start VPN Service!");
            }
        } catch (IOException e) {
            // Log.e(TAG, e.getMessage());
        }
    }

    // Trace files


    /**
     * update and close the time file
     */

    /**
     * setup VPN interface.
     *
     * @return boolean
     * @throws IOException
     */
    boolean startVpnService() {
        // If the old interface has exactly the same parameters, use it!
        if (mInterface != null) {
            // Log.i(TAG, "Using the previous interface");
            return false;
        }

        // Log.i(TAG, "startVpnService => create builder");
        // Configure a builder while parsing the parameters.
        Builder builder = new Builder()
                .addAddress("10.120.0.1", 32)
                .addRoute("0.0.0.0", 0)
                .setSession("ToyShark");
        mInterface = builder.establish();

        if (mInterface != null) {
         //   // Log.i(TAG, "VPN Established:interface = " + mInterface.getFileDescriptor().toString());
            return true;
        } else {
            // Log.d(TAG, "mInterface is null");
            return false;
        }
    }

    /**
     * start background thread to handle client's socket, handle incoming and outgoing packet from VPN interface
     *
     * @throws IOException
     */
    void starVpn() throws IOException {


        // Packets to be sent are queued in this input stream.
        FileInputStream clientReader = new FileInputStream(mInterface.getFileDescriptor());

        // Packets received need to be written to this output stream.
        FileOutputStream clientWriter = new FileOutputStream(mInterface.getFileDescriptor());

        // Allocate the buffer for a single packet.
        ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET_LEN);
        IClientPacketWriter clientPacketWriter = new ClientPacketWriterImpl(clientWriter);

        SessionHandler handler = SessionHandler.getInstance();
        handler.setWriter(clientPacketWriter);

        //background task for non-blocking socket
        dataService = new SocketNIODataService(clientPacketWriter);
        dataServiceThread = new Thread(dataService);
        dataServiceThread.start();

        //background task for writing packet data to pcap file
        packetbgWriter = new SocketDataPublisher();
        packetbgWriter.subscribe(this);
        packetQueueThread = new Thread(packetbgWriter);
        packetQueueThread.start();

        byte[] data;
        int length;
        serviceValid = true;
        while (serviceValid) {
            //read packet from vpn client
            data = packet.array();
            length = clientReader.read(data);
            if (length > 0) {
                //// Log.d(TAG, "received packet from vpn client: "+length);
                try {
                    packet.limit(length);

                    handler.handlePacket(packet);
                } catch (PacketHeaderException e) {
                    // Log.e(TAG, e.getMessage());
                }

                packet.clear();
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Log.d(TAG, "Failed to sleep: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            // Log.d(TAG, "handleMessage:" + getString(message.what));
            Toast.makeText(this.getApplicationContext(), message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public void protectSocket(DatagramSocket socket) {
        this.protect(socket);
    }

    @Override
    public void receive(byte[] packet) {

    }
}
