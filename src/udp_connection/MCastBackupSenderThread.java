package udp_connection;

import data.Chunk;
import data.PeerDataBase;

import java.util.concurrent.Semaphore;

public class MCastBackupSenderThread extends Thread {

    private final static int MAX_NO_OF_THREADS = 5;
    private final static Semaphore backup_semaphore = new Semaphore(MAX_NO_OF_THREADS);
    private Chunk chunk;
    private PeerDataBase database;

    public MCastBackupSenderThread(Chunk chunk, PeerDataBase database){

        this.chunk = chunk;
        this.database = database;
    }

    public void run(){

        try {
            backup_semaphore.acquire();
            long time = 1000;

            for (int retries = 5; retries > 0; retries--) {

                UDPMessageSender.sendPUTCHUNK(chunk.get_fileID(), chunk.get_chunkID(), chunk.get_replication_degree(), chunk.get_body());
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (database.desired_rep_degree(chunk.get_fileID(), chunk.get_chunkID())) {
                    break;
                }

                time = time * 2;

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            backup_semaphore.release();

        }


    }
}
