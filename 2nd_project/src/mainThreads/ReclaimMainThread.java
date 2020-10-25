package mainThreads;

import data.Chunk;
import data.PeerDataBase;
import chord.*;
import communication.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;



public class ReclaimMainThread extends Thread {

    private long max_disk_space;
    private PeerDataBase database;
    private Chord chord;
    private final static Semaphore reclaim_semaphore = new Semaphore(1);

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_GREEN = "\u001B[32m";

    public ReclaimMainThread(long max_disk_space, PeerDataBase database, Chord chord){
        this.max_disk_space = max_disk_space;
        this.database = database;
        this.chord = chord;
    }

    public void run(){

        try{
            reclaim_semaphore.acquire();

            max_disk_space = max_disk_space * 1000;


            System.out.println(ANSI_GREEN + "> ANALYZING DISK" + ANSI_RESET);

            if(database.get_occupied_space() > max_disk_space){

                System.out.println("> REMOVING CHUNKS TO FREE UP SPACE");
                long space_to_free = database.get_occupied_space() - max_disk_space;

                ArrayList<Chunk> biggerRD;
                while(space_to_free > 0){

                    biggerRD = database.reclaimChunks();
                    String ckToRemove = database.chunk_to_remove(biggerRD, space_to_free);
                    java.util.List<String> args_list = Arrays.asList(ckToRemove.split(":"));
                    String fileID = args_list.get(0);
                    int chunkID = Integer.parseInt(args_list.get(1));

                    String contact = database.getContact(fileID, chunkID);
                    java.util.List<String> contact_list = Arrays.asList(contact.split(":"));
                    String contactIP = contact_list.get(0);
                    int contactPORT = Integer.parseInt(contact_list.get(1));

                    int size = database.length_chunk(fileID, chunkID);
                    byte[] removed_chunk_body = database.get_other_chunk_body(fileID, chunkID);

                    if(database.remove_other_chunk(fileID, chunkID)){

                        String chunk_path = "./peer_disk/peer" + chord.getPeerID() + "/backup/" + fileID +  "/" + chunkID;
                        File file = new File(chunk_path);
            
                        if (file.exists()) {
                            file.delete();
                        }
                        

                        System.out.println("REMOVED CHUNK (fileID, chunkID): " + fileID + ", " + chunkID);
                        space_to_free = space_to_free - size;
                        
                        if(contactIP.equals(this.chord.getNode().getIp()) && contactPORT == this.chord.getNode().getPort()){

                            if(database.my_chunk(fileID, chunkID)){
                                database.decrease_rep_deg(fileID, chunkID);
                    
                                if(!database.desired_rep_degree(fileID, chunkID)){
                                    int rd = database.obtain_my_chunk_rd(fileID, chunkID);
                                    Chunk chunk = new Chunk(chunkID, fileID, removed_chunk_body, rd);
                     
                                    TCPBackupSenderThread chunkSender = new TCPBackupSenderThread(chunk, database, chord);
                                    chunkSender.start();
                                }
                            }
                        }
                        else{
                            try {
                                TCPSocketThread removeConnection = new TCPSocketThread(contactIP, contactPORT);
                                removeConnection.start();
                            
                                byte[] message = MessageBuilder.buildREMOVED(fileID, chunkID, removed_chunk_body);
                                removeConnection.send(message);
                            }
                            catch(IOException e){
                                e.printStackTrace();
                            }
                        }
                    }

                }

            }

            //we still need to eliminate chunks with a size of 0 bytes
            if(max_disk_space == 0){

                ArrayList<Chunk> cks = database.get_other_chunks();


                if(cks.size() != 0){

                    for(int i = 0; i < cks.size(); i++){

                        String fileID = cks.get(i).get_fileID();
                        int nmr = cks.get(i).get_chunkID();

                    
                        String contactInfo = database.getContact(fileID, nmr);
                        java.util.List<String> contactList = Arrays.asList(contactInfo.split(":"));
                        String contact_ip = contactList.get(0);
                        int contact_port = Integer.parseInt(contactList.get(1));
                        byte[] removed_chunk_body2 = database.get_other_chunk_body(fileID, nmr);
                       

                        if(database.remove_other_chunk(fileID, nmr)){

                            String chunk_path = "./peer_disk/peer" + chord.getPeerID() + "/backup/" + fileID +  "/" + nmr;
                            File file = new File(chunk_path);
            
                            if (file.exists()) {
                                file.delete();
                            }

                            System.out.println("REMOVED CHUNK (fileID, chunkID): " + fileID + ", " + nmr);

                            if(contact_ip.equals(this.chord.getNode().getIp()) && contact_port == this.chord.getNode().getPort()){
                                if(database.my_chunk(fileID, nmr)){
                                    database.decrease_rep_deg(fileID, nmr);
                        
                                    if(!database.desired_rep_degree(fileID, nmr)){
                                        int rd = database.obtain_my_chunk_rd(fileID, nmr);
                                        Chunk chunk = new Chunk(nmr, fileID, removed_chunk_body2, rd);
                         
                                        TCPBackupSenderThread chunkSender = new TCPBackupSenderThread(chunk, database, chord);
                                        chunkSender.start();
                                    }
                                }
                            }
                            else{
                                try {
                                    TCPSocketThread removeConnection = new TCPSocketThread(contact_ip, contact_port);
                                    removeConnection.start();
                                
                                    byte[] message = MessageBuilder.buildREMOVED(fileID, nmr, removed_chunk_body2);
                                    removeConnection.send(message);
                                }
                                catch(IOException e){
                                    e.printStackTrace();
                                }
                            }
                        }

                    }

                }

            }

            database.set_maximum_space(max_disk_space);
            System.out.println(ANSI_GREEN + "> SPACE RECLAIMED SUCCESSFULLY" + ANSI_RESET);
            System.out.println(ANSI_CYAN + "> Maximum space: " + ANSI_RESET + database.get_maximum_space() / 1000 + " KBytes");
            System.out.println(ANSI_CYAN + "> Occupied space: " + ANSI_RESET + database.get_occupied_space() / 1000 + " KBytes");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            reclaim_semaphore.release();
        }
    }
}
