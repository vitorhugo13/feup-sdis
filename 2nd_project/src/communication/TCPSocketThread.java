package communication;

import chord.NodeInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCPSocketThread implements TcpInterface {
    private Socket socket;
    private BlockingQueue<byte[]> messageQueue;
    private AtomicBoolean running;

    public TCPSocketThread(Socket socket){
        this.socket = socket;
        messageQueue = new ArrayBlockingQueue<>(10); //arbitrary capacity TODO change this (?)
        running = new AtomicBoolean();
    }

    public TCPSocketThread(String hostname, int port) throws IOException {
        InetAddress hostAddress = InetAddress.getByName(hostname);
        this.socket = new Socket(hostAddress, port);
        messageQueue = new ArrayBlockingQueue<>(10); //arbitrary capacity TODO change this (?)
        running = new AtomicBoolean();
    }

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        running.set(false);
    }

    @Override
    public NodeInfo getConnectionInfo() {
        String ipAddress = ((InetSocketAddress)socket.getRemoteSocketAddress()).getAddress().toString().replace("/","");
        int port = socket.getPort();
        return new NodeInfo(BigInteger.valueOf(-1), ipAddress, port); 
    }

    public byte[] readMessage() {
        try {
            InputStream dataStream = socket.getInputStream();

            /*--------------------------------------------------------
                Length delimit specific
             ---------------------------------------------------------*/
            
            byte[] lengthBytes = new byte[4];
            dataStream.read(lengthBytes, 0, 4);
            int length = ByteBuffer.wrap(lengthBytes).getInt();

         

            byte[] message = new byte[length];
            int readBytes = 0;
            while (readBytes < length) {
                int byteIncrement = dataStream.read(message, readBytes, length - readBytes);
                if (byteIncrement == -1) {
                    //TODO throw exception(?)
                }
                readBytes += byteIncrement;
            }
            
            /*--------------------------------------------------------
                Length delimit specific
             ---------------------------------------------------------*/

            return message;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Returns the head element of the message queue
    public byte[] receive(int timeout){
        try {
            socket.setSoTimeout(timeout);
            byte[] message = readMessage();
            socket.setSoTimeout(0);
            return message;
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void send(byte[] message){
        try {
            OutputStream outputStream = socket.getOutputStream();
            byte[] delimitedMessage = MessageDelimiter.wrapLength(message);
            outputStream.write(delimitedMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

    }
}
