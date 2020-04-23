package udp_connection;

import data.Chunk;
import data.PeerDataBase;
import java.net.DatagramPacket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;


public class MCastListenerThread extends Thread {

    private String channel_type;
    private MCastChannel MC, MDB, MDR;
    private int peerID;
    private String protocol_version;
    private PeerDataBase database;


    private static ConcurrentHashMap<String, Integer> enhanc_actual;


    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    

    public MCastListenerThread(int peerID, String protocol_version, String channel_type, MCastChannel MC, MCastChannel MDB, MCastChannel MDR, PeerDataBase database){
        this.peerID = peerID;
        this.protocol_version = protocol_version;
        this.channel_type = channel_type;
        this.MC = MC;
        this.MDB = MDB;
        this.MDR = MDR;
        this.database = database;

        if(protocol_version.equals("2.0")){

            this.enhanc_actual = new ConcurrentHashMap<>();

        }

    }

    public void run(){

        while(true){
            DatagramPacket received;
            if(channel_type.equals("MDB")) {
                received = MDB.receivePacket();
            } else if(channel_type.equals("MDR")) {
                received = MDR.receivePacket();
            } else {
                received = MC.receivePacket();
            }
            MessageData received_data = new MessageData(received.getData(), received.getLength());
            if(!(received_data.getProtocol_version().equals("1.0") || received_data.getProtocol_version().equals("2.0"))){
                System.out.println(ANSI_RED + "Unknown protocol version, ignoring " + received_data.getType() + " message from peer " + received_data.getPeer() + ANSI_RESET);
                continue;
            }
            if(received_data.getPeer() == peerID){
                continue;
            }
            if(channel_type.equals("MDB")){

                if(!(received_data.getType().equals("PUTCHUNK"))){
                    System.out.println(ANSI_RED + "Invalid message from peer " + received_data.getPeer() + "on MDB channel, ignoring" + ANSI_RESET);
                    continue;
                }
                else{

                    if(protocol_version.equals("2.0")){

                        System.out.println(ANSI_YELLOW + "Received on MDB: " + ANSI_RESET + received_data.getHeader());
                        String key = received_data.getFileID() + ':' + received_data.getChunkID();

                        this.enhanc_actual.put(key, 0);
                        

                        try{

                            Random ger = new Random();
                            Thread.sleep(ger.nextInt(401));

                        }catch(Exception e){
                            e.printStackTrace();
                        }

                        int actual = 0;
                        if(this.enhanc_actual.containsKey(key)){
                            actual = this.enhanc_actual.get(key);
                        }

                        


                        if(actual < received_data.getRep_deg()){

                            Chunk chunk = new Chunk(received_data.getChunkID(), received_data.getFileID(), received_data.getChunk(), received_data.getRep_deg());
                            if(database.add_chunk_from_other(chunk)){
                                MCastResponseSenderThread resp = new MCastResponseSenderThread(received_data, "STORED", database);
                                resp.start();
                            }

                        }
                        

                    }
                    else{

                        System.out.println(ANSI_YELLOW + "Received on MDB: " + ANSI_RESET + received_data.getHeader());
                        
                        Chunk chunk = new Chunk(received_data.getChunkID(), received_data.getFileID(), received_data.getChunk(), received_data.getRep_deg());

                        database.update_reclaim_backup(received_data.getFileID(), received_data.getChunkID());

                        if(database.add_chunk_from_other(chunk)){
                            MCastResponseSenderThread resp = new MCastResponseSenderThread(received_data, "STORED", database);
                            resp.start();
                        }

                    }
                }

            }
            else if(channel_type.equals("MDR")){
                if(!(received_data.getType().equals("CHUNK"))){
                    System.out.println(ANSI_RED + "Invalid message from peer " + received_data.getPeer() + "on MDR channel, ignoring" + ANSI_RESET);
                    continue;
                }
                else{
                    System.out.println(ANSI_YELLOW + "Received on MDR: " + ANSI_RESET+ received_data.getHeader());

                    if(!database.my_chunk(received_data.getFileID(), received_data.getChunkID())){

                        if(database.have_other_chunk(received_data.getFileID(), received_data.getChunkID())){

                            database.update_restore(received_data.getFileID(), received_data.getChunkID());

                        }

                    }
                    else{
              
                       Chunk received_ck = new Chunk(received_data.getChunkID(), received_data.getFileID(), received_data.getChunk(), 1);
                       database.add_received_ck(received_ck);                   
                        
                    }
                }
            }
            else {
                System.out.println(ANSI_YELLOW + "Received on MC: "+ ANSI_RESET + received_data.getHeader());
                switch (received_data.getType()) {
                    case "STORED":
                        
                        if(this.protocol_version.equals("2.0")){
                            String key = received_data.getFileID() + ':' + received_data.getChunkID();

                           

                            if(this.enhanc_actual.containsKey(key)){
                                Integer new_value = this.enhanc_actual.get(key) + 1;
                                this.enhanc_actual.put(key, new_value);
                            }
                        }
                        
                        if(database.file_exists(received_data.getFileID())){
                            database.increase_rep_deg(received_data.getFileID(), received_data.getChunkID());
                        }
                        else if(database.have_other_chunk(received_data.getFileID(), received_data.getChunkID())){
                            database.increase_other_rep_deg(received_data.getFileID(), received_data.getChunkID());
                        }
                        

                        break;
                    case "GETCHUNK":

                        if(!database.my_chunk(received_data.getFileID(), received_data.getChunkID())){

                            if(database.have_other_chunk(received_data.getFileID(), received_data.getChunkID())){

                                database.init_restore(received_data.getFileID(), received_data.getChunkID());

                                MCastResponseSenderThread resp = new MCastResponseSenderThread(received_data, "CHUNK", database);
                                resp.start();

                            }

                        }

                        break;
                    case "DELETE":

                        String fileID = received_data.getFileID();

                        if(database.delete_chunk_of_other(fileID)){

                            System.out.println(ANSI_GREEN + "DELETED CHUNKS OF FILE WITH ID: " + ANSI_RESET + fileID );

                        }


                        break;
                    case "REMOVED":

                        int chunkID = received_data.getChunkID();
                        String file = received_data.getFileID();

                        
                        if(database.my_chunk(file, chunkID)){

                            database.decrease_rep_deg(file, chunkID);
                             
                        }
                        else if(database.have_other_chunk(file, chunkID)){

                            database.decrease_other_rep_deg(file, chunkID);


                            if(!database.desired_other_rep_deg(file, chunkID)){

                                database.init_reclaim_backup(received_data.getFileID(), received_data.getChunkID());

                                MCastResponseSenderThread resp = new MCastResponseSenderThread(received_data, "PUTCHUNK", database);
                                resp.start();

                            }

                        }
                        break;
                    case "CONNECTED":
                        if(protocol_version.equals("2.0")){
                            MCastEnhancedDeleteThread resp = new MCastEnhancedDeleteThread(database);
                            resp.start();
                        }
                        break;
                    default:
                        System.out.println(ANSI_RED + "Invalid message from peer " + received_data.getPeer() + "on MC channel, ignoring" + ANSI_RESET);
                        break;
                }
            }
        }

    }
}