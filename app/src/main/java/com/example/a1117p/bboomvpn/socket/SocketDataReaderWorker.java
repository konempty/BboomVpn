package com.example.a1117p.bboomvpn.socket;

import android.support.annotation.NonNull;

import com.example.a1117p.bboomvpn.HttpResponse;
import com.example.a1117p.bboomvpn.IClientPacketWriter;
import com.example.a1117p.bboomvpn.MySharedPreferences;
import com.example.a1117p.bboomvpn.NotEndException;
import com.example.a1117p.bboomvpn.Session;
import com.example.a1117p.bboomvpn.SessionManager;
import com.example.a1117p.bboomvpn.network.ip.IPPacketFactory;
import com.example.a1117p.bboomvpn.network.ip.IPv4Header;
import com.example.a1117p.bboomvpn.transport.tcp.PacketHeaderException;
import com.example.a1117p.bboomvpn.transport.tcp.TCPHeader;
import com.example.a1117p.bboomvpn.transport.tcp.TCPPacketFactory;
import com.example.a1117p.bboomvpn.transport.udp.UDPHeader;
import com.example.a1117p.bboomvpn.transport.udp.UDPPacketFactory;
import com.example.a1117p.bboomvpn.util.PacketUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Date;
import java.util.HashMap;

/**
 * background task for reading data from remote server and write data to vpn client
 *
 * @author Borey Sao
 * Date: July 30, 2014
 */
class SocketDataReaderWorker implements Runnable {
    private static final String TAG = "SocketDataReaderWorker";
    private IClientPacketWriter writer;
    private String sessionKey;
    private SocketData pData;
    private final static int MAX_PACKET_SIZE = 65356;

    SocketDataReaderWorker(IClientPacketWriter writer, String sessionKey) {
        pData = SocketData.getInstance();
        this.writer = writer;
        this.sessionKey = sessionKey;
    }

    @Override
    public void run() {
        Session session = SessionManager.INSTANCE.getSessionByKey(sessionKey);
        if (session == null) {
            // Log.e(TAG, "Session NOT FOUND");
            return;
        }

        AbstractSelectableChannel channel = session.getChannel();

        if (channel instanceof SocketChannel) {
            readTCP(session);
        } else if (channel instanceof DatagramChannel) {
            readUDP(session);
        } else {
            return;
        }

        if (session.isAbortingConnection()) {
            //	// Log.d(TAG,"removing aborted connection -> "+ sessionKey);
            session.getSelectionKey().cancel();
            if (channel instanceof SocketChannel) {
                try {
                    SocketChannel socketChannel = (SocketChannel) channel;
                    if (socketChannel.isConnected()) {
                        socketChannel.close();
                    }
                } catch (IOException e) {
                    // Log.e(TAG, e.toString());
                }
            } else {
                try {
                    DatagramChannel datagramChannel = (DatagramChannel) channel;
                    if (datagramChannel.isConnected()) {
                        datagramChannel.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            SessionManager.INSTANCE.closeSession(session);
        } else {
            session.setBusyread(false);
        }
    }

    private void readTCP(@NonNull Session session) {
        if (session.isAbortingConnection()) {
            return;
        }

        SocketChannel channel = (SocketChannel) session.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(DataConst.MAX_RECEIVE_BUFFER_SIZE);
        int len;

        try {
            do {
                if (!session.isClientWindowFull()) {
                    len = channel.read(buffer);
                    if (len > 0) { //-1 mean it reach the end of stream
                        //// Log.d(TAG,"SocketDataService received "+len+" from remote server: "+name);
                        sendToRequester(buffer, len, session);
                        buffer.clear();
                    } else if (len == -1) {
                        // Log.d(TAG,"End of data from remote server, will send FIN to client");
                        //		// Log.d(TAG,"send FIN to: " + sessionKey);
                        sendFin(session);
                        session.setAbortingConnection(true);
                    }
                } else {
                    //	// Log.e(TAG,"*** client window is full, now pause for " + sessionKey);
                    break;
                }
            } while (len > 0);
        } catch (NotYetConnectedException e) {
            // Log.e(TAG,"socket not connected");
        } catch (ClosedByInterruptException e) {
            // Log.e(TAG,"ClosedByInterruptException reading SocketChannel: "+ e.getMessage());
            //session.setAbortingConnection(true);
        } catch (ClosedChannelException e) {
            // Log.e(TAG,"ClosedChannelException reading SocketChannel: "+ e.getMessage());
            //session.setAbortingConnection(true);
        } catch (IOException e) {
            // Log.e(TAG,"Error reading data from SocketChannel: "+ e.getMessage());
            session.setAbortingConnection(true);
        }
    }

    private void sendToRequester(ByteBuffer buffer, int dataSize, @NonNull Session session) {
        //last piece of data is usually smaller than MAX_RECEIVE_BUFFER_SIZE
        if (dataSize < DataConst.MAX_RECEIVE_BUFFER_SIZE)
            session.setHasReceivedLastSegment(true);
        else
            session.setHasReceivedLastSegment(false);

        buffer.limit(dataSize);
        buffer.flip();
        // TODO should allocate new byte array?
        byte[] data = new byte[dataSize];
        System.arraycopy(buffer.array(), 0, data, 0, dataSize);
        session.addReceivedData(data);
        //// Log.d(TAG,"DataService added "+data.length+" to session. session.getReceivedDataSize(): "+session.getReceivedDataSize());
        //pushing all data to vpn client
        while (session.hasReceivedData()) {
            pushDataToClient(session);
        }
    }

    /**
     * create packet data and send it to VPN client
     *
     * @param session Session
     */
    boolean isNotEnded = false;
    byte[] before;

    private void pushDataToClient(@NonNull final Session session) {
        if (!session.hasReceivedData()) {
            //no data to send
            //	// Log.d(TAG,"no data for vpn client");
        }

        IPv4Header ipHeader = session.getLastIpHeader();
        int ip = ipHeader.getDestinationIP();
        TCPHeader tcpheader = session.getLastTcpHeader();
        // TODO What does 60 mean?
        int max = session.getMaxSegmentSize() - 60;

        if (max < 1) {
            max = 1024;
        } else if (max > MAX_PACKET_SIZE - 60) {
            max = MAX_PACKET_SIZE - 60;
        }
        byte[] packetBody = session.getReceivedData(max);
        if (packetBody != null && packetBody.length > 0) {
            long unAck = session.getSendNext();
            boolean isNormalPacket=true;

            //we need this data later on for retransmission
            //session.setUnackData(packetBody);
            String string = new String(packetBody);
            if (string.contains("gzip")) {
                if (isNotEnded) {
                    byte[] tmp = new byte[packetBody.length + before.length];
                    System.arraycopy(before, 0, tmp, 0, before.length);
                    System.arraycopy(packetBody, 0, tmp, before.length, packetBody.length);
                    packetBody = tmp;
                }

                isNormalPacket = isNotEnded = false;

                try {
                    packetBody = Post(packetBody);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NumberFormatException | NullPointerException e) {
                    e.printStackTrace();
                } catch (NotEndException e) {
                    //   isNotEnded = true;
                    before = packetBody;
                }
            } else if (string.contains("data-comment-no")&&MySharedPreferences.getCommentFilter()) {
                if (isNotEnded) {
                    byte[] tmp = new byte[packetBody.length + before.length];
                    System.arraycopy(before, 0, tmp, 0, before.length);
                    System.arraycopy(packetBody, 0, tmp, before.length, packetBody.length);
                    packetBody = tmp;
                }

                isNormalPacket = isNotEnded = false;

                try {
                    packetBody = reply(packetBody);
                } catch (JSONException | IOException | NumberFormatException e) {
                    e.printStackTrace();
                } catch (NotEndException e) {
                    isNotEnded = true;
                    before = packetBody;
                }
            }
            if (isNormalPacket||!isNotEnded) {
                long nextUnAck = session.getSendNext() + packetBody.length;
                //// Log.d(TAG,"sending vpn client body len: "+packetBody.length+", current seq: "+unAck+", next seq: "+nextUnAck);
                session.setSendNext(nextUnAck);

                session.setResendPacketCounter(0);
                byte[] data = TCPPacketFactory.createResponsePacketData(ipHeader,
                        tcpheader, packetBody, session.hasReceivedLastSegment(),
                        session.getRecSequence(), unAck,
                        session.getTimestampSender(), session.getTimestampReplyto());
                try {
                    writer.write(data);
                    pData.addData(data);
                } catch (IOException e) {
                    // Log.e(TAG,"Failed to send ACK + Data packet: " + e.getMessage());
                }
            }
        }


    }

    private byte[] reply(byte[] packetBody) throws IOException, NotEndException, JSONException, NumberFormatException {
     //   String pa = new String(packetBody);
        HttpResponse httpResponse = HttpResponse.parse(packetBody);
        String str = httpResponse.getResponseBodyAsString();
        JSONObject jsonObject = new JSONObject(str);
        int code = Integer.parseInt(jsonObject.getString("code"));
        int count = Integer.parseInt(jsonObject.getString("message"));
        Document document = Jsoup.parse(jsonObject.getString("result"));
        Elements elements = document.select("li");
        HashMap<Integer, String> hashMap = MySharedPreferences.getHashmap();
        for (Element element : elements) {
            str = element.select(".pf_img").attr("href");
            int Userno = Integer.parseInt(str.replace("/profile/home.nhn?userNo=", ""));

            if (hashMap.containsKey(Userno)) {
                element.select("a").next().next().remove();
                element.select("[class^=bb_up]").remove();
                element.select(".sc_thumb").remove();
            }
        }
        JSONObject jsonEdited = new JSONObject();
        str = document.body().html().replace("\n", "");
        jsonEdited.put("code", code).put("message", String.valueOf(count)).put("result", str);
        packetBody = HttpResponse.reverse(packetBody, jsonEdited.toString().getBytes(), false);
       // pa = new String(packetBody);
        return packetBody;
    }

    byte[] Post(byte[] packetBody) throws IOException, NotEndException, NumberFormatException {
        boolean mov_block = MySharedPreferences.getBlock_Mov();
        HttpResponse httpResponse = HttpResponse.parse(packetBody);
        String str = httpResponse.getResponseBodyAsString();
        Document document = Jsoup.parse(str);
        if (str.contains("뿜업하기")) {
            Elements elements = document.select("li");
            HashMap<Integer, String> hashMap = MySharedPreferences.getHashmap();
            for (Element element : elements) {
                Elements elements1 = element.select(".sc_usr a");
                String[] strs = elements1.first().attr("href").split("userNo=");
                int userno = Integer.valueOf(strs[1]);
                if (hashMap.containsKey(userno) || (mov_block && element.select(".play_mov").size() != 0))
                    element.remove();
            }
            str = document.body().html().replace("\n", "");
            packetBody = HttpResponse.reverse(packetBody, str.getBytes(), true);

        }
        return packetBody;
    }

    private void sendFin(Session session) {
        final IPv4Header ipHeader = session.getLastIpHeader();
        final TCPHeader tcpheader = session.getLastTcpHeader();
        final byte[] data = TCPPacketFactory.createFinData(ipHeader, tcpheader,
                session.getSendNext(), session.getRecSequence(),
                session.getTimestampSender(), session.getTimestampReplyto());
        try {
            writer.write(data);
            pData.addData(data);
        } catch (IOException e) {
            // Log.e(TAG,"Failed to send FIN packet: " + e.getMessage());
        }
    }

    private void readUDP(Session session) {
        DatagramChannel channel = (DatagramChannel) session.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(DataConst.MAX_RECEIVE_BUFFER_SIZE);
        int len;

        try {
            do {
                if (session.isAbortingConnection()) {
                    break;
                }
                len = channel.read(buffer);
                if (len > 0) {
                    Date date = new Date();
                    long responseTime = date.getTime() - session.connectionStartTime;

                    buffer.limit(len);
                    buffer.flip();
                    //create UDP packet
                    byte[] data = new byte[len];
                    System.arraycopy(buffer.array(), 0, data, 0, len);
                    byte[] packetData = UDPPacketFactory.createResponsePacket(
                            session.getLastIpHeader(), session.getLastUdpHeader(), data);
                    //write to client
                    writer.write(packetData);
                    //publish to packet subscriber
                    pData.addData(packetData);
                    // Log.d(TAG,"SDR: sent " + len + " bytes to UDP client, packetData.length: "
                    //	+ packetData.length);
                    buffer.clear();

                    try {
                        final ByteBuffer stream = ByteBuffer.wrap(packetData);
                        IPv4Header ip = IPPacketFactory.createIPv4Header(stream);
                        UDPHeader udp = UDPPacketFactory.createUDPHeader(stream);
                        String str = PacketUtil.getUDPoutput(ip, udp);
                        // Log.d(TAG,"++++++ SD: packet sending to client ++++++++");
                        // Log.i(TAG,"got response time: " + responseTime);
                        // Log.d(TAG,str);
                        // Log.d(TAG,"++++++ SD: end sending packet to client ++++");
                    } catch (PacketHeaderException e) {
                        e.printStackTrace();
                    }
                }
            } while (len > 0);
        } catch (NotYetConnectedException ex) {
            // Log.e(TAG,"failed to read from unconnected UDP socket");
        } catch (IOException e) {
            e.printStackTrace();
            // Log.e(TAG,"Failed to read from UDP socket, aborting connection");
            session.setAbortingConnection(true);
        }
    }
}
