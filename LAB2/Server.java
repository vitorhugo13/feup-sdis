import java.util.HashMap;
import java.util.Map;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class Server{

    private static Map<String, String> dns_table;

    private static int srvc_port;
    private static int mcast_port;
    private static String mcast_addr;

    public static void main(String[] args) throws IOException {
        
        if(!check_arguments(args)){
            System.out.println("> Error parsing arguments.");
            return;
        }

        dns_table = new HashMap<String, String>();// (DNS, IP)

        MulticastSocket multicastSocket = new MulticastSocket();
        multicastSocket.setTimeToLive(1);
        InetAddress addr = InetAddress.getByName(mcast_addr);
        DatagramSocket socket = new DatagramSocket(srvc_port);


		socket.setSoTimeout(1000); //SO_TIMEOUT is the timeout that a read() call will block. If the timeout is reached, a java.net.SocketTimeoutException will be thrown.

        long previous_time = System.currentTimeMillis();

        boolean working = true;

        while(working){

            //open a datagrampacket for each request 
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            try{
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
                    System.out.println(">" + operation+ " " + search_dns + " : " + response);
                }
                else{
                    System.out.println("> " + operation+ " "+ register_dns+ " " + register_ip + " : " + response);
                }

                socket.send(packet);

            }catch(SocketTimeoutException e){

            }
            //prints

        //-------------------------------send advertisement every second ----------------------

            long current_time = System.currentTimeMillis(); //This method returns the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC(coordinated universal time).
            long difference = 0;

            difference += current_time - previous_time;
            previous_time = current_time;

            if(difference >= 1000){ //if difference >= 1000ms ita time to update and send advertiser

                difference = difference-1000;
                String info_message = new String();

                info_message = srvc_port+ " " + InetAddress.getLocalHost().getHostAddress(); //falta mandar ip do server

                byte[] info_buffer = info_message.getBytes();
                packet = new DatagramPacket(info_buffer, info_buffer.length, addr, mcast_port);


                multicastSocket.send(packet);
                System.out.println("> multicast: " + mcast_addr + " " + mcast_port + " : "+ InetAddress.getLocalHost().getHostAddress()+" " + srvc_port);
            }

        }
            
            socket.close();
            multicastSocket.close();
    }

    public static boolean check_arguments(String[] args){

        if(args.length != 3){

            System.out.println("> java Server <srvc_port> <mcast_addr> <mcast_port>");
            return false;
        }

        srvc_port = Integer.parseInt(args[0]);
        mcast_addr = args[1];
        mcast_port = Integer.parseInt(args[2]);
        return true;
    }
}