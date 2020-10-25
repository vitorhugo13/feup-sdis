package communication;

import data.Chunk;
import data.DataFile;
import data.PeerDataBase;

import chord.*;
import utility.Utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.Semaphore;
import java.math.BigInteger;

public class TCPBackupSenderThread extends Thread {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_YELLOW = "\u001B[33m";


    private final static int MAX_NO_OF_THREADS = 5;
    private final static Semaphore backup_semaphore = new Semaphore(MAX_NO_OF_THREADS);
    private Chunk chunk;
    private DataFile datafile;
    private PeerDataBase database;
    private Chord chord;

    public TCPBackupSenderThread(Chunk chunk, PeerDataBase database, Chord chord){

        this.chunk = chunk;
        this.database = database;
        this.chord = chord;
    }

    public void save_my_chunk(Chunk chunk) {

        if(database.add_chunk_from_other(chunk, this.chord.getNode().getIp(), this.chord.getNode().getPort())){

            String chunk_path = "./peer_disk/peer" + chord.getPeerID() + "/backup/" + chunk.get_fileID() +  "/" + chunk.get_chunkID();
            File file = new File(chunk_path);

            try {

                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }

                FileOutputStream fileOutput = new FileOutputStream(file);
                ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput);

                objectOutput.writeObject(chunk.get_body());
                objectOutput.close();
                fileOutput.close();

            }
            catch(IOException exception){
              exception.printStackTrace();
            }


            if(database.file_exists(chunk.get_fileID())){
                database.increase_rep_deg(chunk.get_fileID(), chunk.get_chunkID());
            }
        }   

    }


    public void run(){

        try {
            backup_semaphore.acquire();
            long time = 1000;

            for (int retries = 5; retries > 0; retries--) {

                for(int rd = 1; rd <= chunk.get_replication_degree(); rd++){
                
                    byte[] key = Utility.hash_Key(chunk.get_fileID(), chunk.get_chunkID(), rd);
                    BigInteger keyToSearch = new BigInteger(1, key);
                    NodeInfo node_to_send = chord.find_successor(keyToSearch);

                
                    if(node_to_send.getKey().compareTo(chord.getNode().getKey()) == 0){
                        save_my_chunk(chunk);
                    }
                    else{
                        ChunkSender sender = new ChunkSender(database, chunk, node_to_send);
                        sender.start();
                    }
                }
                
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
