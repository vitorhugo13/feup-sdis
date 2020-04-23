import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;


public class TestApp{
    public static void main(String[] args) throws RemoteException, NotBoundException { //exceptions asked when compiling

        if(args.length < 2 || args.length > 4){
            System.out.println("> USAGE: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2> ");
            System.exit(-1);
        }

        String access_point = args[0]; //using RMI
        String subprotocol = args[1].toLowerCase(); //allows user to introduce words like : backup, BACKUP, BackUp, Backup, etc.
        String path_of_file;
        long max_disk_space;
        int replication_degree;

        
        Registry registry = LocateRegistry.getRegistry("localhost");
        PeerInterface stub = (PeerInterface) registry.lookup(access_point);

        try{
        
            switch(subprotocol){
                case "backup":
                    if(args.length != 4){
                        System.out.println("> USAGE: java TestApp <peer_ap> BACKUP <path_of_file> <desired_replication_degree> ");
                        System.exit(-1);
                    }

                    path_of_file = args[2];
                    replication_degree = Integer.parseInt(args[3]);
                    
                    if(replication_degree > 9 || replication_degree < 1){

                        System.out.println("> Replication Degree must be a number between 1 and 9");
                        System.exit(-1);
                    }

                    //call subprotocol function
                    stub.backup(path_of_file, replication_degree);
                    break;

                case "restore":
                    if(args.length != 3){
                        System.out.println("> USAGE: java TestApp <peer_ap> RESTORE <path_of_file>");
                        System.exit(-1);
                    }

                    path_of_file = args[2];

                    stub.restore(path_of_file);
                    break;

                case "delete":
                    if(args.length != 3){
                        System.out.println("> USAGE: java TestApp <peer_ap> DELETE <path_of_file>");
                        System.exit(-1);
                    }

                    path_of_file = args[2];
                    stub.delete(path_of_file);

                    break;

                case "reclaim":
                    if(args.length != 3){
                        System.out.println("> USAGE: java TestApp <peer_ap> RECLAIM <max_disk_space>");
                        System.exit(-1);
                    }

                    max_disk_space = Integer.parseInt(args[2]);

                    stub.reclaim(max_disk_space);
                    break;
                case "state":
                    if(args.length != 2){
                        System.out.println("> USAGE: java TestApp <peer_ap> State");
                        System.exit(-1);
                    }

                    String state = stub.state();
                    System.out.println(state);
                    
                    break;

                default:
                    System.out.println("> Subprotocol not available. Choose between BACKUP, RESTORE, DELETE, RECLAIM, STATE");
                    System.exit(-1);
            }
        }catch(Exception exception){
            System.out.println("Exception found - TestApp");
            exception.printStackTrace();
        }
    }
}
