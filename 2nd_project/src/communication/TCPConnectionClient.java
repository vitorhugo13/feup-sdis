package communication;

public class TCPConnectionClient {
    private TcpInterface tcpInterface;

    public TCPConnectionClient (TcpInterface tcpInterface) {
        this.tcpInterface = tcpInterface;
    }

    //test function TODO delete this
    public void handshake(){
        tcpInterface.start();

        String message = "hello!";
        tcpInterface.send(message.getBytes());
        byte[] response = tcpInterface.receive(3000);
        System.out.println("Client " + new String(response));
    }
}
