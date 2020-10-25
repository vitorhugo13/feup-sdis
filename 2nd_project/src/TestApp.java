import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;


public class TestApp{

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static void main(String[] args) throws RemoteException, NotBoundException {

        if(args.length < 2 || args.length > 4){
            System.out.println(ANSI_RED + ">USAGE: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2> " + ANSI_RESET);
            System.exit(-1);
        }

        String access_point = args[0];
        String subprotocol = args[1].toLowerCase(); 
        String path_of_file;
        long max_disk_space;
        int replication_degree;

        
        Registry registry = LocateRegistry.getRegistry("localhost");
        PeerInterface stub = (PeerInterface) registry.lookup(access_point);

        try{
        
            switch(subprotocol){
                case "backup":
                    if(args.length != 4){
                        System.out.println(ANSI_RED + "> USAGE: java TestApp <peer_ap> BACKUP <path_of_file> <desired_replication_degree> " + ANSI_RESET);
                        System.exit(-1);
                    }

                    path_of_file = args[2];
                    replication_degree = Integer.parseInt(args[3]);
                    
                    if(replication_degree > 9 || replication_degree < 1){

                        System.out.println(ANSI_RED + "> Replication Degree must be a number between 1 and 9" + ANSI_RESET);
                        System.exit(-1);
                    }

                    stub.backup(path_of_file, replication_degree);
                    break;

                case "restore":
                    if(args.length != 3){
                        System.out.println(ANSI_RED + "> USAGE: java TestApp <peer_ap> RESTORE <path_of_file>" + ANSI_RESET);
                        System.exit(-1);
                    }

                    path_of_file = args[2];

                    stub.restore(path_of_file);
                    break;

                case "delete":
                    if(args.length != 3){
                        System.out.println(ANSI_RED + "> USAGE: java TestApp <peer_ap> DELETE <path_of_file>" + ANSI_RESET);
                        System.exit(-1);
                    }

                    path_of_file = args[2];
                    stub.delete(path_of_file);

                    break;

                case "reclaim":
                    if(args.length != 3){
                        System.out.println(ANSI_RED + "> USAGE: java TestApp <peer_ap> RECLAIM <max_disk_space>" + ANSI_RESET);
                        System.exit(-1);
                    }

                    max_disk_space = Integer.parseInt(args[2]);

                    stub.reclaim(max_disk_space);
                    break;
                case "state":
                    if(args.length != 2){
                        System.out.println(ANSI_RED + "> USAGE: java TestApp <peer_ap> State" + ANSI_RESET);
                        System.exit(-1);
                    }

                    String state = stub.state();
                    System.out.println(state);
                    
                    break;
                case "chord":
                    if(args.length != 2){
                        System.out.println(ANSI_RED + "> USAGE: java TestApp <peer_ap> State" + ANSI_RESET);
                        System.exit(-1);
                    }

                    String chord = stub.chordState();
                    System.out.println(chord);
                    break;
                default:
                    System.out.println(ANSI_RED + "> Subprotocol not available. Choose between BACKUP, RESTORE, DELETE, RECLAIM, STATE, CHORD" + ANSI_RESET);
                    System.exit(-1);
            }
        }catch(Exception exception){
            System.out.println(ANSI_RED + "Exception found - TestApp" + ANSI_RESET);
            exception.printStackTrace();
        }
    }
}