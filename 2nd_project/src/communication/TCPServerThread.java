package communication;

import chord.Chord;
import data.PeerDataBase;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCPServerThread implements Runnable {
    private ServerSocket serverSocket;
    private AtomicBoolean running;
    private PeerDataBase database;
    private Chord chord;

    public TCPServerThread(int port, PeerDataBase database, Chord chord) throws IOException {
        serverSocket = new ServerSocket(port);
        running = new AtomicBoolean();
        this.database = database;
        this.chord = chord;
    }

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        running.set(false);
    }

    @Override
    public void run() {
        running.set(true);

        while(running.get()){
            try {
                TcpInterface tcpInterface = new TCPSocketThread(serverSocket.accept());
                new TCPConnectionServer(tcpInterface, database, chord).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
