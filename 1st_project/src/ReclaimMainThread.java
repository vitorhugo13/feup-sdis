import data.Chunk;
import data.PeerDataBase;
import udp_connection.UDPMessageSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class ReclaimMainThread extends Thread {

    private long max_disk_space;
    private PeerDataBase database;
    private final static Semaphore reclaim_semaphore = new Semaphore(1);

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_GREEN = "\u001B[32m";

    public ReclaimMainThread(long max_disk_space, PeerDataBase database){
        this.max_disk_space = max_disk_space;
        this.database = database;
    }

    public void run(){
        try{
            reclaim_semaphore.acquire();

            max_disk_space = max_disk_space * 1000;


            System.out.println(ANSI_GREEN + "> ANALYZING DISK" + ANSI_RESET);

            if(database.get_occupied_space() > max_disk_space){

                System.out.println("> REMOVING CHUNKS TO FREE UP SPACE");
                long space_to_free = database.get_occupied_space() - max_disk_space;


                while(space_to_free > 0){

                    ConcurrentHashMap<String, Integer> diff_RedDeg = database.dif_actual_desired();
                    String remove = database.chunk_to_remove(diff_RedDeg);

                    java.util.List<String> args_list = Arrays.asList(remove.split(":"));
                    String fileID = args_list.get(0);
                    int chunkID = Integer.parseInt(args_list.get(1));

                    int size = database.length_chunk(fileID, chunkID);

                    if(database.remove_other_chunk(fileID, chunkID)){

                        System.out.println("REMOVED CHUNK (fileID, chunkID): " + fileID + ", " + chunkID);
                        space_to_free = space_to_free - size;
                        UDPMessageSender.sendREMOVED(fileID, chunkID);

                    }

                }

            }

            //we still need to eliminate chunks with a size of 0 bytes
            if(max_disk_space == 0){

                ArrayList<Chunk> cks = database.get_other_chunks();


                if(cks.size() != 0){

                    for(int i = 0; i < cks.size(); i++){

                        String file = cks.get(i).get_fileID();
                        int nmr = cks.get(i).get_chunkID();

                        if(database.remove_other_chunk(file, nmr)){

                            System.out.println("REMOVED CHUNK (fileID, chunkID): " + file + ", " + nmr);
                            UDPMessageSender.sendREMOVED(file, nmr);

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
