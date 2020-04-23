import data.DataFile;
import data.PeerDataBase;
import udp_connection.*;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.io.*;


public class Peer implements PeerInterface{

    private static int peerID; //unique for each peer
    private static String protocol_version;
    private static PeerDataBase database; //each peer has its own space
    private static MCastChannel MC, MDB, MDR;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";


    public Peer(){
        MCastListenerThread MC_Listener = new MCastListenerThread(peerID, protocol_version, "MC", MC, MDB, MDR, database);
        MCastListenerThread MDB_Listener = new MCastListenerThread(peerID, protocol_version, "MDB", MC, MDB, MDR, database);
        MCastListenerThread MDR_Listener = new MCastListenerThread(peerID, protocol_version, "MDR", MC, MDB, MDR, database);

        MC_Listener.start();
        MDB_Listener.start();
        MDR_Listener.start();

        if(protocol_version.equals("2.0")){
            UDPMessageSender.sendCONNECTED();
        }

    }

    public static void main(String[] args) throws NoSuchAlgorithmException, FileNotFoundException, IOException{

        if (args.length != 9){
            System.out.println(ANSI_RED + "Usage: java Peer <protocol_version> <peerID> <srvc_access_point> <MC_IP> <MC_PORT> <MDB_IP> <MDB_PORT> <MDR_IP> <MDR_PORT>" + ANSI_RESET);
            return;
        }

        protocol_version = args[0];
        peerID = Integer.parseInt(args[1]);
        MC = new MCastChannel(args[3], args[4]);
        MDB = new MCastChannel(args[5], args[6]);
        MDR = new MCastChannel(args[7], args[8]);
        UDPMessageSender.setValues(peerID, protocol_version, MC, MDB, MDR);
        load_database();


        PeerInterface stub = null;

        try{ 

            Peer obj = new Peer();
            stub = (PeerInterface) UnicastRemoteObject.exportObject(obj, 0); //0 could be any number

            
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(args[2], stub);

            System.err.println(ANSI_GREEN + "Peer ready" + ANSI_RESET);
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }



        Runtime.getRuntime().addShutdownHook(new Thread(Peer::save_database));
    }

    @Override
    public void backup(String path_of_file, int replication_degree) throws RemoteException{

        BackupMainThread bac = new BackupMainThread(path_of_file, replication_degree, database, protocol_version);
        bac.start();

    }

    @Override
    public void restore(String path_of_file) throws RemoteException {
        
        RestoreMainThread res = new RestoreMainThread(path_of_file, database, peerID);
        res.start();

    }

    @Override
    public void delete(String path_of_file) throws RemoteException{

        DeleteMainThread del = new DeleteMainThread(path_of_file, protocol_version, database, peerID);
        del.start();

    }

    @Override
    public void reclaim(long max_disk_space) throws RemoteException{

        ReclaimMainThread rec = new ReclaimMainThread(max_disk_space, database);
        rec.start();

    }

    @Override
    public String state() throws RemoteException{
        
        String state = database.state();
        return state;
        
    }



    public static int get_peerID(){

        return peerID;

    }

    public static String get_protocol_version(){

        return protocol_version;

    }

    public static PeerDataBase getDatabase(){
        return database;
    }





    /* load database from file system if exists */
    private static void load_database(){

        String path = "./peer_disk/peer" + peerID + "/backup/database.ser";
        File new_file = new File(path);

        try{

            if(!new_file.exists()){

                database = new PeerDataBase(500000000000000L, peerID);
                return;

            }

            ObjectInputStream input = new ObjectInputStream(new FileInputStream(path));

            database = (PeerDataBase) input.readObject();
            input.close();

        }catch(IOException exception){

            exception.printStackTrace();

        } catch (ClassNotFoundException exception) {
            exception.printStackTrace();
        }

    }

    /* save database to file system  */
    private static void save_database(){

        String path = "./peer_disk/peer" + peerID + "/backup/database.ser";
        File new_file = new File(path);

        try{

            if(!new_file.exists()){

                new_file.getParentFile().mkdirs();
                new_file.createNewFile();

            }

            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(path));
            output.writeObject(database);
            output.close();

        }catch(IOException exception){

            exception.printStackTrace();

        }

    }
}