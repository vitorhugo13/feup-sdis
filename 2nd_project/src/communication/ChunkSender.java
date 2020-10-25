package communication;

import chord.*;
import data.*;

import java.util.concurrent.Semaphore;
import java.io.IOException;

public class ChunkSender extends Thread{
    private final static int MAX_NO_OF_THREADS = 2;
    private final static Semaphore backup_semaphore = new Semaphore(MAX_NO_OF_THREADS);

    private PeerDataBase database;
    private Chunk chunk;
    private NodeInfo toSend;

    public ChunkSender(PeerDataBase database, Chunk chunk, NodeInfo toSend){
        this.database = database;
        this.chunk = chunk;
        this.toSend = toSend;
    }

    @Override
    public void run(){
        try {
            backup_semaphore.acquire();

            try {
                TCPSocketThread backupConnection = new TCPSocketThread(this.toSend.getIp(), this.toSend.getPort());
                backupConnection.start();

                byte[] message = MessageBuilder.buildPUTCHUNK(chunk.get_fileID(), chunk.get_chunkID(), chunk.get_replication_degree(), chunk.get_body());
                backupConnection.send(message);
                byte[] response = backupConnection.receive(3000);

                if(response != null){
                    MessageData data = new MessageData(response, response.length);

                    String fileID = data.getFileID();
                    int chunkID = data.getChunkID();

                    if(database.file_exists(fileID)){
                        database.increase_rep_deg(fileID, chunkID);
                    }
                }
            }
            catch(IOException e){
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            backup_semaphore.release();
        }
    }
}