package udp_connection;

import data.PeerDataBase;

import java.util.Random;

public class MCastResponseSenderThread extends Thread{

    private MessageData received_data;
    private String type;
    private PeerDataBase database;

    public MCastResponseSenderThread(MessageData received_data, String type, PeerDataBase database){
        this.received_data = received_data;
        this.type = type;
        this.database = database;
    }

    public void run() {
        if(type.equals("CHUNK")){
            try {

                Random gerador2 = new Random();
                Thread.sleep(gerador2.nextInt(401));
                
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(!database.already_sent_restore(received_data.getFileID(), received_data.getChunkID())){
                UDPMessageSender.sendCHUNK(received_data.getFileID(), received_data.getChunkID(), database.get_other_chunk_body(received_data.getFileID(), received_data.getChunkID()) );
            }
        }
        else if(type.equals("PUTCHUNK")) {
            try {
                Random gerador = new Random();
                Thread.sleep(gerador.nextInt(401));
                if (!database.already_sent_reclaim(received_data.getFileID(), received_data.getChunkID())) {
                    long time = 1000;
                    for (int retries = 5; retries > 0; retries--) {
                        UDPMessageSender.sendPUTCHUNK(received_data.getFileID(), received_data.getChunkID(), database.obtain_other_chunk_rd(received_data.getFileID(), received_data.getChunkID()), database.get_other_chunk_body(received_data.getFileID(), received_data.getChunkID()));
                        try {
                            Thread.sleep(time);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (database.desired_other_rep_deg(received_data.getFileID(), received_data.getChunkID())) {
                            break;
                        }

                        time = time * 2;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else if(type.equals("STORED")){
            try{

                if(!received_data.getProtocol_version().equals("2.0")){
                    Random gerador = new Random();
                    Thread.sleep(gerador.nextInt(401));
                }
                UDPMessageSender.sendSTORED(received_data.getFileID(), received_data.getChunkID());

            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }

}
