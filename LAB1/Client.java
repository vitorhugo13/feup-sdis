import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

/*register
    to bind a DNS name to an IP address. It returns -1, if the name has already been registered, and the number of bindings in the service, otherwise.
lookup
    to retrieve the IP address previously bound to a DNS name. It returns the IP address in the dotted decimal format or the NOT_FOUND string, 
    if the name was not previously registered.
*/


/*
USEFUL EXAMPLE


    //example of a hashmap. Will be useful to save DNS an IP addresses

    Map<String, String> dns_table = new HashMap<>();
    dns_table.put("dns1", "1.2.333");
    System.out.println(dns_table.get("dns1"));
    System.out.println(dns_table);

    System.out.println(args.length); 

*/

public class Client{

    private static String host_name;
    private static int port_number;
    private static String operation;
    private static String dns, ip_address;
    public static void main(String[] args)throws IOException {

       if(!check_arguments(args)){
            System.out.println("> Error parsing arguments.");
            return;
       }

       InetAddress address = InetAddress.getByName(host_name);

       String message = new String(); //escrever a mensagem para mandar ao server aqui
       if(operation.equals("register")){
        message = operation +" " + dns +" "+ ip_address;
       }
       else if(operation.equals("lookup")){
        message = operation +" " + dns;

       }

       System.out.println(message);
       byte[] buffer = message.getBytes();

       DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port_number);
       DatagramSocket socket = new DatagramSocket();//socket do client nao ter argumentos, o do servidor tem o port_number para ouvir nessa porta

       System.out.println("> Opening socket");

       //sends request to server
       socket.send(packet);
       

       //receives response from server
       byte[] received_buffer= new byte[buffer.length];
       packet= new DatagramPacket(received_buffer, received_buffer.length);

       socket.receive(packet);

       String message_received = new String(packet.getData());

       //closes socket

       if(operation.equals("lookup")){
        System.out.println("> Client:" + operation + dns + " : " + message_received);
       }
       else{
        System.out.println("> Client:" + operation + dns + ip_address + " : " + message_received);
       }
       socket.close();
    }

    public static boolean check_arguments(String[] args){

        //number of arguments must be exactly 4
        if(args.length !=4){
            System.out.println("> java Client <host> <port> <oper> <opnd>");
            return false;
        }

        host_name = args[0];
        port_number = Integer.parseInt(args[1]);

        if(args[2].equals("LOOKUP") || args[2].equals("lookup")){
            operation = "lookup";
        }
        else if(args[2].equals("REGISTER") || args[2].equals("register")){
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

            dns = args_list.get(0);
            ip_address= args_list.get(1);
        }
        else if(operation.equals("lookup")){
             java.util.List<String> args_list = Arrays.asList(args[3].split(","));
            
            if(args_list.size() != 1){
                System.out.println("> <DNS name> for lookup");
                return false;
            }
            
            dns = args_list.get(0);
        }
        
        

        return true;
    }
}