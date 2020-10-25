package mainThreads;

import chord.*;
import utility.Utility;

import data.Chunk;
import data.PeerDataBase;
import communication.*;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.io.IOException;


public class RestoreMainThread extends Thread {

    private String path_of_file;
    private PeerDataBase database;
    private int peerID;
    private Chord chord;
    private final static Semaphore restore_semaphore = new Semaphore(1);

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";


    public RestoreMainThread(String path_of_file, PeerDataBase database, int peerID, Chord chord){
        this.path_of_file = path_of_file;
        this.database = database;
        this.peerID = peerID;
        this.chord = chord;
    }

    public void run(){

        try {

            restore_semaphore.acquire();

            database.clean_temp();


            Boolean found = false;
            Boolean done = true;

            for(int i = 0; i < database.get_files().size(); i++){

                if(database.get_files().get(i).get_file().getPath().equals(path_of_file)){

                    found = true;
                    Boolean chunk_error = false;
                    int chunkID_failed = -1;
                    String name_file = database.get_files().get(i).get_file().getName();
                    ArrayList<String> chunks = database.get_chunks_of_file(database.get_files().get(i).get_fileID());
                    //int chunkRD = database.get_files().get(i).get_replication_degree();

                    for(int j = 0; j < chunks.size(); j++){

                        String ck = chunks.get(j);
                        java.util.List<String> args_list = Arrays.asList(ck.split(":"));
                        String id_file = args_list.get(0);
                        int id_chunk = Integer.parseInt(args_list.get(1));
                        int chunkRD = database.get_chunk_rep_deg().get(id_file + ':' + id_chunk);

                        byte[] hash;
                        BigInteger toSearch;
                        Boolean chunk_found = false;

                        for(int k = 1; k <= chunkRD; k++){
                            hash = Utility.hash_Key(id_file, id_chunk, k);
                            toSearch =  new BigInteger(1, hash);
                            NodeInfo succ = chord.find_successor(toSearch);
                            
                            //If the successor does not respond, send it to the next k (let the cycle go) if any succ responds, 
                            //we abort the cycle and move on to the next chunk. if no succ responds we abort the restore

                            if(succ.getKey() == this.chord.getNode().getKey()){
                                if(database.have_other_chunk(id_file, id_chunk)){
                                    Chunk chunkRestored = new Chunk(id_chunk, id_file, database.get_other_chunk_body(id_file, id_chunk),1);
                                    database.add_received_ck(chunkRestored);
                                    chunk_found = true;
                                    break;
                                }
                            }
                            else{
                                try {
                                    TCPSocketThread restoreConnection = new TCPSocketThread(succ.getIp(), succ.getPort());
                                    restoreConnection.start();
                                
                                    byte[] message = MessageBuilder.buildGETCHUNK(id_file, id_chunk);
                                    restoreConnection.send(message);

                                    byte[] response = restoreConnection.receive(1000);

                                    if(response != null){
                                        MessageData responseData = new MessageData(response, response.length);

                                        Chunk chunk_restored = new Chunk(responseData.getChunkID(), responseData.getFileID(), responseData.getChunk(), 1);
                                        database.add_received_ck(chunk_restored);
                                        chunk_found = true;
                                        break;
                                    }

                                }
                                catch(IOException e){
                                    e.printStackTrace();
                                }
                            }
                        }

                        if(!chunk_found){
                            chunkID_failed = j;
                            chunk_error = true;
                            break;  
                        }
                    }

                    ArrayList<Chunk> temp = database.get_temp();

                    if(chunk_error){
                        System.out.println(ANSI_RED + "CHUNK RECOVERY ERROR: Not possible to restore chunk " + chunkID_failed + ANSI_RESET );
                        done = false;
                    }
                    else if(chunks.size() != temp.size()){
                        System.out.println(ANSI_RED + "CHUNK RECOVERY ERROR: Not possible to finish the restore process for " + ANSI_RESET +path_of_file );
                        done = false;
                    }
                    else{

                        String path = "./peer_disk/peer" + peerID + "/restore/" + name_file;
                        File new_file = new File(path);

                        try{

                            if(!new_file.exists()){

                                new_file.getParentFile().mkdirs();
                                new_file.createNewFile();

                            }


                            for(int k = 0; k < temp.size(); k++){

                                Chunk chunk = database.chunk_with_id(k);

                                if(chunk != null){

                                    byte[] body = chunk.get_body();

                                    if(body != null){
                                        FileOutputStream os = new FileOutputStream(path, true);
                                        os.write(body);
                                        os.close();
                                    }

                                }
                                else{
                                    System.out.println(ANSI_RED + "ERROR: Not possible to finish the restore process for "+ ANSI_RESET + path_of_file);
                                    done = false;
                                    new_file.delete();
                                    break;
                                }

                            }


                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }

                    database.clean_temp();
                }

            }

            if(!found){
                System.out.println(ANSI_RED + "> PEER " + peerID + " does not started a backup to " + path_of_file + ANSI_RESET);
            }

            if(found && done){
                System.out.println(ANSI_GREEN + "> File successfully restored "+ ANSI_RESET);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            restore_semaphore.release();
        }
    }
}
