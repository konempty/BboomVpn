package com.example.a1117p.bboomvpn.socket;

import java.net.DatagramSocket;
import java.net.Socket;

public interface IProtectSocket {
	void protectSocket(Socket socket);
	void protectSocket(int socket);
	void protectSocket(DatagramSocket socket);
}
