import data.Chunk;
import data.PeerDataBase;
import udp_connection.UDPMessageSender;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class RestoreMainThread extends Thread {

    private String path_of_file;
    private PeerDataBase database;
    private int peerID;
    private final static Semaphore restore_semaphore = new Semaphore(1);

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";


    public RestoreMainThread(String path_of_file, PeerDataBase database, int peerID){
        this.path_of_file = path_of_file;
        this.database = database;
        this.peerID = peerID;
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
                    String name_file = database.get_files().get(i).get_file().getName();

                    ArrayList<String> chunks = database.get_chunks_of_file(database.get_files().get(i).get_fileID());


                    for(int j = 0; j < chunks.size(); j++){

                        String ck = chunks.get(j);
                        java.util.List<String> args_list = Arrays.asList(ck.split(":"));
                        UDPMessageSender.sendGETCHUNK(args_list.get(0), Integer.parseInt(args_list.get(1)));

                        try{
                            Thread.sleep(1100);
                        }catch(Exception e){
                            e.printStackTrace();
                        }

                    }

                    ArrayList<Chunk> temp = database.get_temp();


                    if(chunks.size() != temp.size()){
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

                System.out.println(ANSI_RED + "> PEER" + peerID + " does not started a backup to " + path_of_file + ANSI_RESET);

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
