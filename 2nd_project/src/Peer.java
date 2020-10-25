import communication.MessageBuilder;
import communication.TCPServerThread;
import data.PeerDataBase;
import chord.*;
import mainThreads.BackupMainThread;
import mainThreads.DeleteMainThread;
import mainThreads.ReclaimMainThread;
import mainThreads.RestoreMainThread;
import utility.Utility;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.io.*;
import java.net.UnknownHostException;

public class Peer implements PeerInterface{

    private static int peerID;
    private static String protocol_version;
    private static PeerDataBase database; 
    private static int port;
    private static Chord chord;
    private static InetAddress serverAddress;
    private static TCPServerThread server;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_CYAN = "\u001B[36m";

    public Peer(String[] args) throws IOException {
        
        chord = new Chord(peerID, port);
        server = new TCPServerThread(port, database, chord);
        server.start();

    
        switch(args.length){
            case 4:
                chord.create();
                break;
            case 6:
                if(!chord.canJoin(args[4],Integer.parseInt(args[5]))){
                    System.out.println(ANSI_RED + "Ups... Impossible to join. No active node in the chord with IP:PORT " +args[4] + ":" + args[5] + ANSI_RESET);
                    System.exit(0);
                }
                chord.join(args[4], Integer.parseInt(args[5]));
                break;
            default:
                break;
        }

    }

    public static void main(String[] args) throws NoSuchAlgorithmException, FileNotFoundException, IOException{

        if (args.length != 4 && args.length != 6){
            System.out.println(ANSI_RED + "Usage: java Peer <protocol_version> <peerID> <srvc_access_point> <port> <ipAddress_of_other> <port_of_other>" + ANSI_RESET);
            return;
        }

        protocol_version = args[0];
        peerID = Integer.parseInt(args[1]);
        port = Integer.parseInt(args[3]);
        MessageBuilder.setValues(peerID,protocol_version,port);

        load_database();
        System.out.println("" + database);

        PeerInterface stub = null;

        try{ 

            Peer obj = new Peer(args);
            stub = (PeerInterface) UnicastRemoteObject.exportObject(obj, 0);

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
        System.out.println(ANSI_CYAN + "\n----INITIATED BACKUP TASK ----" + ANSI_RESET);

        BackupMainThread bac = new BackupMainThread(path_of_file, replication_degree, database, chord, protocol_version);
        bac.start();
    }

    @Override
    public void restore(String path_of_file) throws RemoteException {
        System.out.println(ANSI_CYAN + "\n----INITIATED RESTORE TASK ----" + ANSI_RESET);

        RestoreMainThread res = new RestoreMainThread(path_of_file, database, peerID, chord);
        res.start();
    }

    @Override
    public void delete(String path_of_file) throws RemoteException{
        System.out.println(ANSI_CYAN + "\n----INITIATED DELETE TASK ----" + ANSI_RESET);

        DeleteMainThread del = new DeleteMainThread(path_of_file, database, peerID, chord, protocol_version);
        del.start();
    }

    @Override
    public void reclaim(long max_disk_space) throws RemoteException{
        System.out.println(ANSI_CYAN + "\n----INITIATED RECLAIM TASK ----" + ANSI_RESET);

        ReclaimMainThread rec = new ReclaimMainThread(max_disk_space, database, chord);
        rec.start();
    }

    @Override
    public String state() throws RemoteException{
        System.out.println(ANSI_CYAN + "\n----INITIATED STATE TASK ----" + ANSI_RESET);

        String state = database.state();
        return state;
    }

    @Override
    public String chordState() throws RemoteException{
        System.out.println(ANSI_CYAN + "\n----INITIATED CHORD STATE TASK ----" + ANSI_RESET);

        String chord_state = chord.stateChord();
        return chord_state;
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
            System.out.println("Read database");

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

 

    /*getters */
    public int get_peerID(){
        return peerID;
    }

    public InetAddress getAddress(){
        return serverAddress;
    }

    public  PeerDataBase getDatabase(){
        return database;
    }

    public int getPort(){
        return port;
    }

    public static String get_protocol_version(){
        return protocol_version;
    }

}