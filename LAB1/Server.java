import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/*register
    to bind a DNS name to an IP address. It returns -1, if the name has already been registered, and the number of bindings in the service, otherwise.
lookup
    to retrieve the IP address previously bound to a DNS name. It returns the IP address in the dotted decimal format or the NOT_FOUND string, 
    if the name was not previously registered.
*/

public class Server {

    private static int port_number;
    private static Map<String, String> dns_table;

    public static void main(String[] args) throws IOException {

        if(!check_arguments(args)){
            System.out.println("> Error parsing arguments");
            return;
        }

        dns_table = new HashMap<String, String>();// (DNS, IP)


        System.out.println("> Opening socket");
        DatagramSocket socket = new DatagramSocket(port_number);

        boolean working = true;

        while(working){

            //open a datagrampacket for each request 
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            socket.receive(packet);
            String request = new String(packet.getData(),0, packet.getLength()); //2nd argument is the offset(dtstance between 2 points)

            System.out.println("> REQUEST RECEIVED: "+ request);

            //parse request
            String[] intermediate_parse = request.split(" ");

            String operation = intermediate_parse[0];
            String response = "";
            String search_dns = "";
            String register_ip = "";
            String register_dns = "";

            switch(operation){
                case "register":
                    
                    register_dns = intermediate_parse[1];
                    register_ip = intermediate_parse[2];

                    if(dns_table.containsKey(register_dns)){
                        response = "-1";
                    }
                    else{
                        dns_table.put(register_dns, register_ip);
                        response = Integer.toString(dns_table.size());
                    }

                    break;
                case "lookup":
                    search_dns = intermediate_parse[1];

                    if(dns_table.containsKey(search_dns)){
                        response = dns_table.get(search_dns);
                    }
                    else{
                        response = "NOT_FOUND";
                    }
                    break;
                default:
                    break;
            }
            //send response back to client
            byte[] to_send = response.getBytes();

            InetAddress address = packet.getAddress();
            int port_number = packet.getPort();
            packet = new DatagramPacket(to_send, to_send.length, address, port_number);
            
            if(operation.equals("lookup")){
                System.out.println("> Server: " + operation+ " " + " " + search_dns);
            }
            else{
                System.out.println("> Server: " + operation+ " "+ register_dns+ " " + register_ip);
            }

            socket.send(packet);
        }

        
        System.out.println("> Closing socket");
        socket.close();
    
    }

    public static boolean check_arguments(String[] args){

        if(args.length !=1){
            System.out.println("> java Server <port number>");
            return false;
        }

        port_number = Integer.parseInt(args[0]);
        
        return true;
    }
}