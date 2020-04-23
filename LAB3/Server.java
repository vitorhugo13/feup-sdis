import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;


public class Server implements ServerInterface{

    private static String remote_object_name;
    private static HashMap<String, String> database= new HashMap<String, String>();

    public Server(){}

    public static void main(String[] args) {


        if(!check_arguments(args)){
            System.out.println("> java Server <remote_object_name>");
            return;
        }

        //set codebase property
        System.setProperty("java.rmi.server.codebase","file:///home/vitor/Desktop/UNIVERSIDADE/3ANO_2SEMESTRE/SDIS/LAB3");//this is the path for the codebase where are all classes files

        try{
            Server obj = new Server();
            ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(obj, 0); //0 could be any number
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(remote_object_name, stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }


	@Override
	public int register(String dns, String ip) throws RemoteException {

        int result;

        if(database.containsKey(dns)){
            result = -1;
        }
        else{
            database.put(dns, ip);
            result = database.size();
        }

        System.out.println("Server: register" + dns + " " + ip + " : " + result);

		return result;
	}

	@Override
	public String lookup(String dns) throws RemoteException {
        
        String result = new String();

        if(database.containsKey(dns)){
            result = database.get(dns);
        }
        else{
            result = "NOT_FOUND";
        }

        System.out.println("Server: lookup" + dns + " : " + result);

        return result;
    }
    

    public static boolean check_arguments(String[] args){

        if(args.length != 1)
            return false;

        remote_object_name = args[0];
        return true;
    }
}