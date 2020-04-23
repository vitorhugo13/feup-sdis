import data.PeerDataBase;
import udp_connection.UDPMessageSender;

public class DeleteMainThread extends Thread {

    private String path_of_file;
    private String protocol_version;
    private PeerDataBase database;
    private int peerID;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";

    public DeleteMainThread(String path_of_file, String protocol_version, PeerDataBase database, int peerID){
        this.path_of_file = path_of_file;
        this.protocol_version = protocol_version;
        this.database = database;
        this.peerID = peerID;
    }

    public void run(){
        Boolean found = false;

        for(int i = 0; i < database.get_files().size(); i++){

            if(database.get_files().get(i).get_file().getPath().equals(path_of_file)){

                found = true;
                String fileID = database.get_files().get(i).get_fileID();

                for(int k = 0; k < 4 ; k++){

                    UDPMessageSender.sendDELETE(fileID);

                    try{
                        Thread.sleep(500);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }

                if(database.delete(fileID)){

                    System.out.println(ANSI_GREEN + "DELETED " + ANSI_RESET + path_of_file );

                }

                if(protocol_version.equals("2.0")){

                    database.add_to_delete(fileID);

                }


            }

        }

        if(!found){

            System.out.println(ANSI_RED + "> PEER" + peerID + " does not started a backup to " + path_of_file + ANSI_RESET);

        }
    }
}
