package communication;

import chord.*;
import data.PeerDataBase;

import java.util.concurrent.Semaphore;
import java.math.BigInteger;
import java.io.IOException;


public class TCPDeleteSenderThread extends Thread {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";

    private final static int MAX_NO_OF_THREADS = 5;
    private final static Semaphore delete_semaphore = new Semaphore(MAX_NO_OF_THREADS);

    private BigInteger idToSearch;
    private Chord chord;
    private String fileID;
    private PeerDataBase database;
   

    public TCPDeleteSenderThread(BigInteger idToSearch, Chord chord, String fileID, PeerDataBase database){
        this.idToSearch = idToSearch;
        this.chord = chord;
        this.fileID = fileID;
        this.database = database;
    }

    public void run(){

        try {
            delete_semaphore.acquire();

            NodeInfo nodeToContact = chord.find_successor(this.idToSearch);
            String ip = nodeToContact.getIp();
            int port = nodeToContact.getPort();

            try {
                TCPSocketThread deleteConnection = new TCPSocketThread(ip, port);
                deleteConnection.start();

                byte[] message = MessageBuilder.buildDELETE(this.fileID);
                deleteConnection.send(message);
            }
            catch(IOException e){
                e.printStackTrace();
            }
            
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            delete_semaphore.release();
        }


    }
}
