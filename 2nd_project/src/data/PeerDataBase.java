package data;

import data.Chunk;
import data.DataFile;

import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.io.*;
import java.util.*;

@SuppressWarnings("serial") 
public class PeerDataBase implements Serializable {

    private static int peer_ID;
    private long maximum_space; /*maximum store a peer has to store files and chunks*/
    private long occupied_space; /*space that is being used at the moment*/

    private ArrayList<DataFile> files; /*arraylist to store the files of which the peer is the home*/
    private ArrayList<Chunk> other_chunks; /*arraylist to store chunks from other peers */
    private ConcurrentHashMap<String, String> chunkContacts; /*hashmap to keep information about ip:port from chunks i store */
    //(key-> fileID:chunkID value-> ip:port)

    private ConcurrentHashMap<String, Integer> chunk_rep_deg; /*hashmap to keep information of my chunks actual replication degree */

    private ArrayList<Chunk> received_restore; /*array to store received chunk during restore subprotocol*/

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RED = "\u001B[31m";

    public PeerDataBase(long maximum_space, int peer_ID){

        this.peer_ID = peer_ID;
        this.maximum_space = maximum_space; 
        this.occupied_space = 0;

        this.files = new ArrayList<>();
        this.other_chunks = new ArrayList<>();
        this.chunkContacts = new ConcurrentHashMap<>();

        this.chunk_rep_deg = new ConcurrentHashMap<>();
        this.received_restore = new ArrayList<>();

    }

    public static int get_peer_id(){
        return peer_ID;
    }

    public void set_maximum_space(long new_max_space){
        this.maximum_space = new_max_space;
    }

    public long get_maximum_space(){
        return this.maximum_space;
    }

    public long get_occupied_space(){
        return this.occupied_space;
    }

    public ArrayList<DataFile> get_files(){
        return this.files;
    }


    public ArrayList<Chunk> get_other_chunks(){
        return this.other_chunks;
    }

    public ConcurrentHashMap<String, Integer> get_chunk_rep_deg(){
        return this.chunk_rep_deg;
    }

    public ConcurrentHashMap<String, String> get_chunks_contacts(){
        return this.chunkContacts;
    }

    public void clean_temp(){
        if(!this.received_restore.isEmpty())
            this.received_restore.clear();
    }

    public ArrayList<Chunk> get_temp(){
        return this.received_restore;
    }

    /*Chunk contacts */
    public void add_contact(String fileID, int chunkID, String ip, int port){
        String key = fileID + ':' + chunkID;
        String value = ip + ':' + port;
        this.chunkContacts.put(key, value);
    }

    public void delete_contact(String fileID, int chunkID){
       String key = fileID + ':' + chunkID;
       if(this.chunkContacts.containsKey(key)){
           this.chunkContacts.remove(key);
       }   
    }

    public String getContact(String fileID, int chunkID){
        String key = fileID + ':' + chunkID;
        if(this.chunkContacts.containsKey(key)){
            return this.chunkContacts.get(key);
        }

        return "";
    }

    /*****/

    public Chunk chunk_with_id(int id){
        Chunk ck = null;
        for(int i = 0; i < this.received_restore.size(); i++){
            if(this.received_restore.get(i).get_chunkID() == id){
                ck = this.received_restore.get(i);
            }
        }
        return ck;
    }

    public void add_received_ck(Chunk ck){
        this.received_restore.add(ck);
    }

    public ArrayList<String> getFileChunks(String fileID){
        ArrayList<String> cks = new ArrayList<>();

        for(int i = 0; i < this.files.size();i++){
            if(this.files.get(i).get_fileID().equals(fileID)){
                cks = this.files.get(i).get_chunks();
            }
        }

        return cks;
    }

    /* add my backup files  */
    public void add_file(DataFile data_file) throws IOException {

        if(!file_already_exists(data_file)){
            this.files.add(data_file);
            ArrayList<String> chunks = data_file.get_chunks();
            add_chunk_rep_deg(chunks);
        }


    }



    /* add other backup chunk's into my storage */
    public Boolean add_chunk_from_other(Chunk chunk, String ip, int port){
        long chunk_length = 0;
        if(chunk.get_body() != null){
            chunk_length = chunk.get_body().length;
        }

        long space = this.maximum_space - this.occupied_space;

        if(space >= chunk_length ){

            if(!existing_piece(chunk)){
                this.other_chunks.add(chunk);
                add_contact(chunk.get_fileID(), chunk.get_chunkID(), ip, port);

                if(chunk.get_body() != null){
                    this.occupied_space = this.occupied_space + chunk.get_body().length;
                }
                return true;
            }
            else{
                System.out.println(ANSI_RED + "> Chunk already stored." + ANSI_RESET);
                return false;
            }
        }
        else{
            System.out.println(ANSI_RED + "> ERROR STORING CHUNK! (not enough space)" + ANSI_RESET);
            return false;
        }
    }

    
    public void add_chunk_rep_deg(ArrayList<String> chunks){
        /* key = fileID:chunkID */
        for(int i = 0; i < chunks.size(); i++){
            this.chunk_rep_deg.put(chunks.get(i), 0);
        }
    }

    public void increase_rep_deg(String fileID, Integer chunkID){
        String key = fileID+ ':' + chunkID;
        if(this.chunk_rep_deg.containsKey(key)){
            Integer new_value = chunk_rep_deg.get(key) + 1;
            this.chunk_rep_deg.put(key, new_value);
        }
    }

    public void decrease_rep_deg(String fileID, Integer chunkID){
        String key = fileID+ ':' + chunkID;
        if(this.chunk_rep_deg.containsKey(key)){
            Integer new_value = chunk_rep_deg.get(key) - 1;
            this.chunk_rep_deg.put(key, new_value);
        }
    }

    public Boolean delete(String fileID){
        if(file_exists(fileID)){
            if(!delete_chunk_rep_deg(fileID))
                return false;

            if(!delete_file(fileID))
                return false;
        
            return true;
        }
        return false;
    }

    public Boolean remove_other_chunk(String fileID, Integer chunkID){

        Boolean removed = false;

        for(int i = 0; i < this.other_chunks.size(); ){
            
            if(this.other_chunks.get(i).get_fileID().equals(fileID) && this.other_chunks.get(i).get_chunkID() == chunkID){
                
                if(this.other_chunks.get(i).get_body() != null){
                    this.occupied_space = this.occupied_space - this.other_chunks.get(i).get_body().length;
                }

                this.other_chunks.remove(i);
                delete_contact(fileID, chunkID);
                removed = true;
            }
            else{
                i++;
            }
        }

        return removed;
    }

    public Boolean delete_chunk_of_other(String fileID){

        Boolean deleted = false;
        
        for(int i = 0; i < this.other_chunks.size(); ){

            if(this.other_chunks.get(i).get_fileID().equals(fileID)){


                if(this.other_chunks.get(i).get_body() != null){

                    this.occupied_space = this.occupied_space - this.other_chunks.get(i).get_body().length;
        
                }
                
                this.other_chunks.remove(i);
                deleted = true;

            }
            else{

                i++;

            }
        }

        return deleted;
    }



    public Boolean delete_file(String fileID){
        ArrayList<DataFile> data_files = get_files();

        for(int i = 0; i < data_files.size(); i++){
            if(data_files.get(i).get_fileID().equals(fileID)){
                this.files.remove(i);
                return true;
            } 
        }

       return false;
    }




    public Boolean delete_chunk_rep_deg(String fileID){
        Boolean deleted = false;
        int n_chunks = get_chunks_of_file(fileID).size();

        for(int i = 0; i < n_chunks; i++){
            String key = fileID + ':' + i;
            if(this.chunk_rep_deg.containsKey(key)){
                this.chunk_rep_deg.remove(key);
                deleted = true;
            }
        }
        return deleted;
    }

    public void update_space(int size){
        this.occupied_space = this.occupied_space + size;
    }

    public ArrayList<String> get_chunks_of_file(String fileID){

        ArrayList<String> chunks = new ArrayList<>();

        for(int i = 0; i < this.files.size();i++){

            if(this.files.get(i).get_fileID().equals(fileID)){

                chunks = this.files.get(i).get_chunks();
                
            }

        }

        return chunks;

    }

    public Boolean is_chunk_mine(Chunk chunk){

        String fileID = chunk.get_fileID();

        for(int i = 0; i < this.files.size(); i++){

            if(this.files.get(i).get_fileID().equals(fileID)){

                return true;

            }

        }

        return false;
    }

    public Boolean my_chunk(String fileID, int chunkID){

        ArrayList<String> my_chunks = getFileChunks(fileID);

        for(int i = 0; i < my_chunks.size(); i++){

            String ck = my_chunks.get(i);
            java.util.List<String> args_list = Arrays.asList(ck.split(":"));

            if(args_list.get(0).equals(fileID) && Integer.parseInt(args_list.get(1)) == chunkID){

               return true;

            }

        }

        return false;
    }

    public Boolean existing_piece(Chunk chunk){

        Boolean exist= false;

        for(int i = 0; i < this.other_chunks.size(); i++){

            if(this.other_chunks.get(i).get_fileID().equals(chunk.get_fileID()) && this.other_chunks.get(i).get_chunkID() == chunk.get_chunkID()){

                exist = true;

            }

        }

        return exist;


    }

    public Boolean have_other_chunk(String fileID, int chunkID){
        
        Boolean exist= false;

        for(int i = 0; i < this.other_chunks.size(); i++){

            if(this.other_chunks.get(i).get_fileID().equals(fileID) && this.other_chunks.get(i).get_chunkID() == chunkID){

                exist = true;

            }

        }

        return exist;

    }

    public Boolean file_already_exists(DataFile file){

        for(int i = 0; i < this.files.size(); i++){

            if(this.files.get(i).get_fileID().equals(file.get_fileID())){

                return true;

            }

        }

        return false;

    }

    public Boolean file_exists(String fileID){

        for(int i = 0; i < this.files.size(); i++){

            if(this.files.get(i).get_fileID().equals(fileID)){

                return true;

            }

        }

        return false;

    }

    /*verify if a chunk is stored with desired replication degree */
    public Boolean desired_rep_deg(Chunk ck){

        String key = ck.get_fileID() + ':' + ck.get_chunkID();
        int actual = this.chunk_rep_deg.get(key);

        if(actual >= ck.get_replication_degree()){

            return true;

        }

        return false;

    }

    
    /*verify if a chunk is stored with desired replication degree */
    public Boolean desired_rep_degree(String fileID, int chunkID){

        String key = fileID + ':' + chunkID;

        if(this.chunk_rep_deg.containsKey(key)){

            int actual = this.chunk_rep_deg.get(key);
            int rd = obtain_my_chunk_rd(fileID, chunkID);

            if(actual >= rd){

                return true;

            }

        }

        return false;

    }

    public int obtain_my_chunk_rd(String fileID, int chunkID){

        ArrayList<String> my_chunks = getFileChunks(fileID);

        int rd = 0;

        for(int i = 0; i < my_chunks.size(); i++){

            String ck = my_chunks.get(i);
            java.util.List<String> args_list = Arrays.asList(ck.split(":"));

            if(args_list.get(0).equals(fileID) && Integer.parseInt(args_list.get(1)) == chunkID){

                DataFile file = get_file(fileID);
                if(file != null){
                    return file.get_replication_degree();
                }

            }

        }

        
        return rd;

    }

    public int obtain_other_chunk_rd(String fileID, int chunkID){

        int rd = 0;

        for(int i = 0; i < this.other_chunks.size(); i++){

            if(this.other_chunks.get(i).get_fileID().equals(fileID) && this.other_chunks.get(i).get_chunkID() == chunkID){

                return this.other_chunks.get(i).get_replication_degree();

            }

        }


        return rd;

    }


    


    /*returns body of other peers chunk */
    public byte[] get_other_chunk_body(String fileID, int chunkID){

        byte[] body = null;

        for(int i = 0; i < this.other_chunks.size(); i++){

            if(this.other_chunks.get(i).get_fileID().equals(fileID) && this.other_chunks.get(i).get_chunkID() == chunkID){

                body= this.other_chunks.get(i).get_body();

            }

        }

        return body;

    }


    public int length_chunk(String fileID, Integer chunkID){

        for(int i = 0; i < this.other_chunks.size(); i++){

            if(this.other_chunks.get(i).get_fileID().equals(fileID) && this.other_chunks.get(i).get_chunkID() == chunkID){

                if(this.other_chunks.get(i).get_body() != null){
                    return this.other_chunks.get(i).get_body().length;
                }
                else{
                    return 0;
                }

            }

        }

        return 0;
    }



    /*RECLAIM FUNCTIONS  */
    public DataFile get_file(String fileID){

        DataFile file = null;

        for(int i = 0; i < this.files.size(); i++){
            if(this.files.get(i).get_fileID().equals(fileID)){
                return this.files.get(i);
            }
        }
        return file;
    }

    /*Funtions used to print Peer's state*/
    public String state(){
        
        String state;
        
        state = ANSI_CYAN + "----------------------------------------" + ANSI_RESET + "\n";
        state = state + ANSI_CYAN +"-------------- Peer State --------------" + ANSI_RESET + "\n";
        state = state + ANSI_CYAN +"---------------------------------------- " + ANSI_RESET + "\n";

        state = state_file(state);
        state = state_chunks(state);

        state = state + "\n" + ANSI_CYAN + "---- Storage capacity ----" +ANSI_RESET+ "\n";
        state = state + ANSI_YELLOW + "Maximum space: " + ANSI_RESET+ this.maximum_space / 1000 + " KBytes \n";
        state = state + ANSI_YELLOW + "Used space: " + ANSI_RESET + (double) this.occupied_space / 1000 + " KBytes \n";


        return state;

    }

    public String state_file(String state){

        state = state + "\n" + ANSI_CYAN +"----- Backup Files (" + this.files.size()+") ---- " +ANSI_RESET +"\n";

        if(this.files.size() == 0){
            state = state + "\n" + " You haven't backed up any files yet! \n \n" ;
        }

        for(int i = 0; i < this.files.size(); i++){

            state = state + "\n" + "- File " + (i + 1) + " - \n";
            state = state + ANSI_YELLOW+"Filepath: "+ANSI_RESET + this.files.get(i).get_file().getPath() + "\n";
            state = state + ANSI_YELLOW+"FileID: " +ANSI_RESET + this.files.get(i).get_fileID() + "\n";
            state = state + ANSI_YELLOW+"Desired replication degree: " +ANSI_RESET + this.files.get(i).get_replication_degree() + "\n";
            state = state + ANSI_YELLOW+"Chunks";


            ArrayList<String> chunks = get_chunks_of_file(this.files.get(i).get_fileID());
                
            state = state + "(" + chunks.size() + ") " + ANSI_RESET + "\n";

            for(int k = 0; k < chunks.size(); k++){

                java.util.List<String> args_list = Arrays.asList(chunks.get(k).split(":"));

                state = state + "\t Chunk " + (k + 1) + "\n";
                state = state + "\t \t  ID: " + Integer.parseInt(args_list.get(1)) + "\n";
                state = state + "\t \t  Perceived replication degree: " + this.chunk_rep_deg.get(chunks.get(k)) + "\n";


            }

            state = state + "\n";

        }

        return state;
    }

    public String state_chunks(String state){

        state = state + "\n" + ANSI_CYAN + "---- Stored Chunks from other files ("+ this.other_chunks.size()+") ---- " + ANSI_RESET + "\n";

        if(this.other_chunks.size()== 0){
            state = state + "\n" +" You haven't stored any chunk of other file!\n\n" ;
        }

        for(int i = 0; i < this.other_chunks.size(); i++){

            state = state + ANSI_YELLOW + "CHUNK FROM: "+ANSI_RESET + this.other_chunks.get(i).get_fileID() + "\n";
            state = state + ANSI_YELLOW + "ID: " + ANSI_RESET + this.other_chunks.get(i).get_chunkID() + "\n";
            state = state + ANSI_YELLOW + "Size: " + ANSI_RESET;

            if(this.other_chunks.get(i).get_body() != null){
                state = state + ( (double) this.other_chunks.get(i).get_body().length / 1000) +" KBytes \n";
            }
            else{
                state = state + "0 Kbytes \n";
            }

            state = state + ANSI_YELLOW + "Contact: " + ANSI_RESET+ this.chunkContacts.get(this.other_chunks.get(i).get_fileID() + ':' + this.other_chunks.get(i).get_chunkID()) + "\n";

            state = state + "\n";
        }

        return state;
    }

    /*SELECT CHUNK - RECLAIM FUNCTIONS*/

    public ArrayList<Chunk> reclaimChunks(){
        ArrayList<Chunk> temp = new ArrayList<>();
        int biggest_rd = 0;

        for(int i = 0; i < this.other_chunks.size(); i++){
            if(this.other_chunks.get(i).get_replication_degree() > biggest_rd)
                biggest_rd = this.other_chunks.get(i).get_replication_degree();
        }

        for(int j = 0; j < this.other_chunks.size(); j++){
            if(this.other_chunks.get(j).get_replication_degree() == biggest_rd)
                temp.add(this.other_chunks.get(j));
        }

        return temp;
    }
    
    public String chunk_to_remove(ArrayList<Chunk> cks, long space){

        ArrayList<Chunk> cks64;

        if(space >= 64000){
            cks64 = getChunks64(cks);
            if(cks64.size() > 0){
                return cks64.get(0).get_fileID() + ':' + cks64.get(0).get_chunkID();
            }
            else{
                if(biggerChunk(cks) == null){
                    return cks.get(0).get_fileID() + ':' + cks.get(0).get_chunkID();
                }
                else{
                    return biggerChunk(cks).get_fileID() + ':' + biggerChunk(cks).get_chunkID();
                }
            }
        }
        else{
            if(chunkBetween(cks, space) == null){
                if(biggerChunk(cks) == null){
                    return cks.get(0).get_fileID() + ':' + cks.get(0).get_chunkID();
                }
                else{
                    return biggerChunk(cks).get_fileID() + ':' + biggerChunk(cks).get_chunkID();
                }
            }
            else{
                return chunkBetween(cks, space).get_fileID() + ':' + chunkBetween(cks, space).get_chunkID();
            }
        }
    }

    public ArrayList<Chunk> getChunks64(ArrayList<Chunk> cks){
        ArrayList<Chunk> bigger64 = new ArrayList<>();

        for(int i = 0; i < cks.size(); i++){

            if(cks.get(i).get_body() != null){
                if(cks.get(i).get_body().length >= 64000){
                    bigger64.add(cks.get(i));
                }
            }
        }
        return bigger64;
    }

    public Chunk biggerChunk(ArrayList<Chunk> cks){
        long bigger = -1;
        Chunk chunk = null;

        for(int i = 0; i < cks.size(); i++){
            if(cks.get(i).get_body() != null){
                if(cks.get(i).get_body().length > bigger){
                    chunk = cks.get(i);
                    bigger = cks.get(i).get_body().length;
                }
            }
        }

        return chunk;
    }

    public Chunk chunkBetween(ArrayList<Chunk> cks, long space){
        long min = 640001;
        Chunk chunk = null;

        for(int i = 0; i < cks.size(); i++){
            if(cks.get(i).get_body() != null){
                if(cks.get(i).get_body().length >= space && cks.get(i).get_body().length < min){
                    chunk = cks.get(i);
                    min = cks.get(i).get_body().length;
                }
            }
        }

        return chunk;
    }
}