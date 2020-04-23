import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;


public class Client{

    private static String localhost;
    private static String remote_object_name;
    private static String operation;
    private static String dns_string;
    private static String ip_address;

    public static void main(String[] args) {
        
        if(!check_arguments(args))
            return;



        try {
            Registry registry = LocateRegistry.getRegistry(localhost);
            ServerInterface stub = (ServerInterface) registry.lookup(remote_object_name);
            if(operation == "register"){
                int result = stub.register(dns_string, ip_address);

                System.out.println("Client: register" + dns_string + " " + ip_address + " : " + result);
            }
            else if(operation == "lookup"){

                String result = stub.lookup(dns_string);
                System.out.println("Server: lookup" + dns_string + " : " + result);
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        } 
    }

    public static boolean check_arguments(String[] args){
        
        if(args.length != 4){
            System.out.println(">java Client <host_name> <remote_object_name> <oper> <opnd>");
            return false;
        }

        localhost= args[0];
        remote_object_name = args[1];

        String aux_op = args[2].toLowerCase();

        if(aux_op.equals("lookup")){
            operation = "lookup";
        }
        else if(aux_op.equals("register")){
            operation = "register";
        }
        else{
            System.out.println("> oper must be REGISTER or LOOKUP");
            return false;
        }
        
        //args[3] depends on the operation 
        if(operation.equals("register")){
            java.util.List<String> args_list = Arrays.asList(args[3].split(","));
            
            if(args_list.size() != 2){
                System.out.println("> <DNS name> <IP address> for register");
                return false;
            }

            dns_string = args_list.get(0);
            ip_address= args_list.get(1);
        }
        else if(operation.equals("lookup")){
             java.util.List<String> args_list = Arrays.asList(args[3].split(","));
            
            if(args_list.size() != 1){
                System.out.println("> <DNS name> for lookup");
                return false;
            }
            
            dns_string = args_list.get(0);
        }
        return true;
    }
}