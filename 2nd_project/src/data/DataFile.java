package data;
import chord.*;
import communication.*;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@SuppressWarnings("serial")
public class DataFile implements Serializable{

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";

    public static final int MAX_CHUNK_SIZE = 64000;
   
    private int replication_degree;
    private File file;
    private String fileID; 
    private ArrayList<String> chunks;
    transient private Chord chord;
    transient private PeerDataBase database;

    public DataFile(String path_of_file, int replication_degree, Chord chord, PeerDataBase database) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        
        this.replication_degree = replication_degree;
        this.chunks = new ArrayList<>();
        

        this.file = new File(path_of_file);
        this.chord = chord;
        this.database = database;

        String identifier = create_fileID();
        this.fileID = encrypts_sha_256(identifier);

        file_into_chunks(fileID, replication_degree);
    }

    
    private String create_fileID(){
        String id;

        String name = this.file.getName();
        long last_modified = this.file.lastModified();
    
        id = name + String.valueOf(last_modified);

        return id;
    }

    private String encrypts_sha_256(String identifier) throws NoSuchAlgorithmException{

        MessageDigest md = MessageDigest.getInstance("SHA-256");  
        byte[] hash = md.digest(identifier.getBytes(StandardCharsets.UTF_8));
        
        // Convert byte array into signum representation  
        BigInteger number = new BigInteger(1, hash);  
  
        // Convert message digest into hex value  
        StringBuilder hexadecimal_string = new StringBuilder(number.toString(16));  
  
        // Pad with leading zeros 
        while (hexadecimal_string.length() < 32){ 
            hexadecimal_string.insert(0, '0');  
        }  
  
        return hexadecimal_string.toString();
    }

    public int get_replication_degree(){

        return this.replication_degree;

    }

    public String get_fileID(){
        
        return this.fileID;

    }

    public File get_file(){

        return this.file;

    }

    public ArrayList<String> get_chunks(){

        return this.chunks;

    }

    /*splits file into chunks, adding them to the ArrayList as fileID:chunkID */
    private void file_into_chunks(String fileID, int replication_degree) throws FileNotFoundException, IOException{
        
        int chunk_id;
        int total_chunks;

        total_chunks = ((int)file.length() / MAX_CHUNK_SIZE) + 1;

        for(chunk_id = 0; chunk_id < total_chunks; chunk_id++){
            String key = fileID + ':' + chunk_id;
            this.chunks.add(key);
        }

    }  

    public void send_chunks(PeerDataBase database) throws IOException {
        int chunk_id;
        int total_chunks;
        Boolean last_is_0 = false;
        int file_length = (int) file.length();

        FileInputStream input_stream = new FileInputStream(file);
        total_chunks = (file_length / MAX_CHUNK_SIZE) + 1;

        if((file_length % MAX_CHUNK_SIZE) == 0){

            last_is_0 = true;

        }

        System.out.println(ANSI_GREEN + "Sending chunks\n" + ANSI_RESET);

        for(chunk_id = 0; chunk_id < total_chunks; chunk_id++){

            byte[] chunk_data;
            Chunk chunk;

            if(chunk_id == (total_chunks - 1)){

                if(last_is_0){

                    chunk_data = null;

                }
                else{
                    chunk_data = new byte[ file_length % MAX_CHUNK_SIZE];
                    input_stream.read(chunk_data);

                }
            }
            else{
                chunk_data = new byte[MAX_CHUNK_SIZE];
                input_stream.read(chunk_data);
            }

            chunk = new Chunk(chunk_id, fileID, chunk_data, replication_degree);

            TCPBackupSenderThread chunkSender = new TCPBackupSenderThread(chunk, database, this.chord);
            chunkSender.start();
              
        }

        input_stream.close();

    }
}