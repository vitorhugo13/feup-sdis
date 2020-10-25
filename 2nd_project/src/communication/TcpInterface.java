package communication;

import chord.NodeInfo;

public interface TcpInterface extends Runnable{
    byte[] receive(int timeout);
    void send(byte[] message);
    void start();
    void stop();
    NodeInfo getConnectionInfo();
}
