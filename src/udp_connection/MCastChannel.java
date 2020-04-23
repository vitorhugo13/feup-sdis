package udp_connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class MCastChannel {

    private InetAddress address;
    private int port;
    private MulticastSocket socket;

    public MCastChannel(String channel_ip, String channel_port){

        try {
            address = InetAddress.getByName(channel_ip);
            port = Integer.parseInt(channel_port);
            socket = new MulticastSocket(port);
            socket.joinGroup(address);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public void sendPacket(DatagramPacket packet) {
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DatagramPacket receivePacket() {
        byte[] buffer = new byte[64500];
        DatagramPacket received = new DatagramPacket(buffer, buffer.length);
        try {
            socket.receive(received);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return received;
    }

    public void closeSocket() {
        try {
            socket.leaveGroup(address);
        } catch (IOException e) {
            e.printStackTrace();
        }

        socket.close();
    }
}
