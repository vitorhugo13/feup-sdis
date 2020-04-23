package data;


import data.Chunk;
import data.DataFile;


import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.io.*;
import java.util.*;

/*class where peer stores it's information regarding files/chunks and whatever they need*/

@SuppressWarnings("serial") 
public class PeerDataBase implements Serializable {

    private long maximum_space; /*maximum store a peer has to store files and chunks*/
    private long occupied_space; /*space that is being used at the moment*/

    private ArrayList<DataFile> files; /*arraylist to store the files of which the peer is the home*/
    private ArrayList<Chunk> other_chunks; /*arraylist to store chunks from other peers */

    private ConcurrentHashMap<String, Integer> chunk_rep_deg; /*hashmap to keep information of my chunks actual replication degree */
    private ConcurrentHashMap<String, Integer> other_rep_deg; /*hashmap to keep information of others chunks actual replication degree */
    private ConcurrentHashMap<String, Boolean> restore_chunks; /*hashmap to keep information of chunks asked to be stored */
    private ConcurrentHashMap<String, Boolean> reclaim_chunks; /*hashmap to keep information of reclaim chunks to backup again */

    private ArrayList<Chunk> received_restore; /*array to store received chunk during restore subprotocol*/
    private ArrayList<String> delete_enhancement;  /*array to store info during delete enhancement*/

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_YELLOW = "\u001B[33m";




    private int peer_ID;

    public PeerDataBase(long maximum_space, int peer_ID){

        this.peer_ID = peer_ID;
        this.maximum_space = maximum_space; 
        this.occupied_space = 0;

        this.files = new ArrayList<>();
        this.other_chunks = new ArrayList<>();

        this.chunk_rep_deg = new ConcurrentHashMap<>();
        this.other_rep_deg = new ConcurrentHashMap<>();
        this.restore_chunks = new ConcurrentHashMap<>();
        this.reclaim_chunks = new ConcurrentHashMap<>();
        this.received_restore = new ArrayList<>();
        this.delete_enhancement = new ArrayList<>();

    }

    public int get_peer_id(){

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


    public ConcurrentHashMap<String, Integer> get_other_rep_deg(){

        return this.other_rep_deg;

    }

    public void clean_temp(){

        if(!this.received_restore.isEmpty())
            this.received_restore.clear();

    }

    public ArrayList<Chunk> get_temp(){

        return this.received_restore;

    }

    /* delete enhancement */

    public ArrayList<String> get_delete_enhancement(){

        return this.delete_enhancement;

    }

    public void add_to_delete(String fileID){

        this.delete_enhancement.add(fileID);

    }

    public void remove_from_delete(String fileID){

        for(int i = 0; i < this.delete_enhancement.size(); ){

            if(this.delete_enhancement.get(i).equals(fileID)){
                this.delete_enhancement.remove(i);
            }
            else{
                i++;
            }

        }

    }

    /* ****/

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
    public Boolean add_chunk_from_other(Chunk chunk){

        long chunk_length = 0;

        if(chunk.get_body() != null){

            chunk_length = chunk.get_body().length;

        }

        long space = this.maximum_space - this.occupied_space;

        //can not store my own chunks
        if(!is_chunk_mine(chunk) && space >= chunk_length ){

            if(!existing_piece(chunk)){

                this.other_chunks.add(chunk);
                add_to_other_rep_deg(chunk);

                if(chunk.get_body() != null){

                    this.occupied_space = this.occupied_space + chunk.get_body().length;
        
                }

                return true;

            }
            else{

                System.out.println("> Chunk already stored.");
                return false;

            }

        }
        else{

            System.out.println("> ERROR STORING CHUNK! (not enough space or this chunk is yours)");
            return false;

        }
        
    }

    
    public void add_chunk_rep_deg(ArrayList<String> chunks){

        /* key = fileID:chunkID */
        for(int i = 0; i < chunks.size(); i++){
            this.chunk_rep_deg.put(chunks.get(i), 0);
        }

    }

    public void add_to_other_rep_deg(Chunk chunk){

        String key = chunk.get_fileID() + ':' + chunk.get_chunkID();
        this.other_rep_deg.put(key, 1);

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


    public void increase_other_rep_deg(String fileID, Integer chunkID){

        String key = fileID+ ':' + chunkID;
        
        if(this.other_rep_deg.containsKey(key)){

            Integer new_value = other_rep_deg.get(key) + 1;
            this.other_rep_deg.put(key, new_value);

        }
        

    }

    public void decrease_other_rep_deg(String fileID, Integer chunkID){

        String key = fileID+ ':' + chunkID;
        
        if(this.other_rep_deg.containsKey(key)){

            Integer new_value = other_rep_deg.get(key) - 1;
            this.other_rep_deg.put(key, new_value);

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

                String key = fileID + ':' + chunkID;

                this.other_chunks.remove(i);
                this.other_rep_deg.remove(key);

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
                
                String key = fileID + ':' + this.other_chunks.get(i).get_chunkID();

                this.other_chunks.remove(i);
                this.other_rep_deg.remove(key);
                
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



    /*verify if a chunk from other peer is stored with desired replication degree */
    public Boolean desired_other_rep_deg(String fileID, int chunkID){

       String key = fileID + ':' + chunkID;
       int actual = this.other_rep_deg.get(key);
       int rd = obtain_other_chunk_rd(fileID, chunkID);

       if(actual >= rd){

           return true;

       }

       return false;

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


    /*creates hashmap <chunk, int> com a diferença que cada chunk tem entre actual e o desired replication degree */
    public ConcurrentHashMap<String, Integer> dif_actual_desired(){

        ConcurrentHashMap<String, Integer> dif = new ConcurrentHashMap<>();

        for(int i = 0; i < this.other_chunks.size(); i++){

            int desired = this.other_chunks.get(i).get_replication_degree();
            String key = this.other_chunks.get(i).get_fileID() + ':' + this.other_chunks.get(i).get_chunkID();
            int actual = this.other_rep_deg.get(key);
            int difference = actual - desired;

            dif.put(key, difference);

        }

        return dif;

    }

    public String chunk_to_remove(ConcurrentHashMap<String, Integer> difference){

        int max = -10;
        String ck = " ";
        int ck_length = -1;

        for (Map.Entry<String, Integer> entry : difference.entrySet()) {

            String key = entry.getKey();
            Integer value = entry.getValue();

            java.util.List<String> args_list = Arrays.asList(key.split(":"));
            String fileID = args_list.get(0);
            int chunkID = Integer.parseInt(args_list.get(1));


            if(value > max){

                ck = key;
                max = value;
                ck_length = length_chunk(fileID, chunkID);

            }
            else if(value == max){


                if(length_chunk(fileID, chunkID) > ck_length){

                    ck = key;
                    max = value;
                    ck_length = length_chunk(fileID, chunkID);

                }

            }
            
        }

        return ck;

    }

    public int length_chunk(String fileID, Integer chunkID){

        for(int i = 0; i < this.other_chunks.size(); i++){

            if(this.other_chunks.get(i).get_fileID().equals(fileID) && this.other_chunks.get(i).get_chunkID() == chunkID){

                return this.other_chunks.get(i).get_body().length;

            }

        }

        return 0;
    }

    /*RESTORE FUNCTIONS  */
    public void init_restore(String fileID, int chunkID){

        String key = fileID + ':' + chunkID;
        this.restore_chunks.put(key, false);

    }

    public void update_restore(String fileID, int chunkID){


        String id = fileID + ':' + chunkID;
        if(this.restore_chunks.containsKey(id)){

            this.restore_chunks.computeIfPresent(id, (key, value) -> true);

        }
    }

    /*RECLAIM FUNCTIONS  */
    public void init_reclaim_backup(String fileID, int chunkID){

        String key = fileID + ':' + chunkID;
        this.reclaim_chunks.put(key, false);

    }

    public void update_reclaim_backup(String fileID, int chunkID){


        String id = fileID + ':' + chunkID;
        if(this.reclaim_chunks.containsKey(id)){

            this.reclaim_chunks.computeIfPresent(id, (key, value) -> true);

        }
    }

    public Boolean already_sent_restore(String fileID, int chunkID){

        String id = fileID + ':' + chunkID;
        if(this.restore_chunks.containsKey(id)){

            return this.restore_chunks.get(id);

        }

        return false;

    }

    public Boolean already_sent_reclaim(String fileID, int chunkID){

        String id = fileID + ':' + chunkID;
        if(this.reclaim_chunks.containsKey(id)){

            return this.reclaim_chunks.get(id);

        }

        return false;

    }

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

        state = state + "\n" + ANSI_CYAN + "---- Stored Chunks from other files ("+ this.other_chunks.size()+") ---- " + ANSI_RESET + "\n ";

        if(this.other_chunks.size()== 0){
            state = state + "\n" +" You haven't stored any chunk of other file! \n \n" ;
        }

        for(int i = 0; i < this.other_chunks.size(); i++){

            state = state + "ID: " + this.other_chunks.get(i).get_chunkID() + "\n";
            state = state + "Size: ";

            if(this.other_chunks.get(i).get_body() != null){
                state = state + ( (double) this.other_chunks.get(i).get_body().length / 1000) +" KBytes \n";
            }
            else{
                state = state + "0 Kbytes \n";
            }

            String key = this.other_chunks.get(i).get_fileID() + ':' + this.other_chunks.get(i).get_chunkID();
            state = state + "Perceived replication degree: " + this.other_rep_deg.get(key) + "\n \n";
        }

        return state;
    }

  
}