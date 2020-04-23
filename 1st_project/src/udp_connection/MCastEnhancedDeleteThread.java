package udp_connection;

import data.PeerDataBase;

import java.util.Random;

public class MCastEnhancedDeleteThread extends Thread {

    private PeerDataBase database;
    private final static int INTERVAL_BETWEEN_DELETES = 500; /*in ms*/

    public MCastEnhancedDeleteThread(PeerDataBase database){
        this.database = database;
    }

    public void run(){

        try {
            Random gerador = new Random();
            Thread.sleep(gerador.nextInt(401));

            for(String fileID : database.get_delete_enhancement()){
                for(int i = 0; i < 3; i++) {
                    UDPMessageSender.sendDELETE(fileID);
                    Thread.sleep(INTERVAL_BETWEEN_DELETES);
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
