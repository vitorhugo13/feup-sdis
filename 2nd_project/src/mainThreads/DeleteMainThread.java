package mainThreads;

import data.PeerDataBase;
import chord.*;
import communication.*;
import utility.Utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

public class DeleteMainThread extends Thread {

    private String path_of_file;
    private PeerDataBase database;
    private int peerID;
    private Chord chord;
    private String protocol_version;


    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";

    public DeleteMainThread(String path_of_file, PeerDataBase database, int peerID, Chord chord, String protocol_version){
        this.path_of_file = path_of_file;
        this.database = database;
        this.peerID = peerID;
        this.chord = chord;
        this.protocol_version = protocol_version;
    }

    public void run(){
        Boolean found = false;

        for(int i = 0; i < database.get_files().size(); i++){

            if(database.get_files().get(i).get_file().getPath().equals(path_of_file)){
                found = true;
                String fileID = database.get_files().get(i).get_fileID();
                int nmr_chunks = database.get_files().get(i).get_chunks().size();

                BigInteger idToSearch;
                byte[] hashedID;
                
                for(int ck = 0; ck < nmr_chunks; ck++){

                    for(int degree = 0; degree < database.get_chunk_rep_deg().get(fileID + ':' + ck); degree++){

                        hashedID = Utility.hash_Key(fileID, ck, degree);
                        idToSearch = new BigInteger(1, hashedID);

                        if(idToSearch.compareTo(this.chord.getNode().getKey()) == 0){

                            if(database.delete_chunk_of_other(fileID)){

                                String file_path = "./peer_disk/peer" + chord.getPeerID() + "/backup/" + fileID;
                                File file = new File(file_path);

                                if(file.exists()){

                                    String[]entries = file.list();
                                    for(String s: entries){
                                        File currentFile = new File(file.getPath(), s);
                                        currentFile.delete();
                                    }

                                    file.delete();
                                }

                                System.out.println(ANSI_GREEN + "DELETED CHUNKS OF FILE WITH ID: " + ANSI_RESET + fileID );
                            }

                        }
                        else{

                            TCPDeleteSenderThread chunkSender = new TCPDeleteSenderThread(idToSearch, this.chord, fileID, database);
                            chunkSender.start();

                        }
                    }

                }
                   
                if(database.delete(fileID)){
                    System.out.println(ANSI_GREEN + "DELETED " + ANSI_RESET + path_of_file );
                }
            }
        }

        if(!found){
            System.out.println(ANSI_RED + "> PEER " + peerID + " does not started a backup to " + path_of_file + ANSI_RESET);
        }
    }
}
