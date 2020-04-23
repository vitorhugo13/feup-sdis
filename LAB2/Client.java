import java.util.Arrays;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.net.InetAddress;
import java.net.MulticastSocket;
import java.io.IOException;


public class Client{


    private static int mcast_port;
    private static String mcast_addr;

    //advertisement (information coming from server where the service is able)
    private static int adver_port;
    

    private static String operation;
    private static String dns, ip_address;

    public static void main(String[] args) throws IOException{

        if(!check_arguments(args)){
            System.out.println("> Error parsing arguments.");
            return;
        }

        MulticastSocket multicastSocket = new MulticastSocket(mcast_port);
        InetAddress group = InetAddress.getByName(mcast_addr);

        multicastSocket.joinGroup(group);

        byte[] buf = new byte[256];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        multicastSocket.receive(packet);

        //client receives a message from server, informing the ip and port of server
        String received = new String(packet.getData(),0,packet.getLength());
        String[] intermediate_parse = received.split(" ");

        adver_port = Integer.parseInt(intermediate_parse[0]);
        InetAddress adver_addr = InetAddress.getByName(intermediate_parse[1]);

        //------------------------------------------------//
        //depois em principio é tudo igual ao LAB1
        //------------------------------------------------//

        String message = new String(); //escrever a mensagem para mandar ao server aqui
       if(operation.equals("register")){
        message = operation +" " + dns +" "+ ip_address;
       }
       else if(operation.equals("lookup")){
        message = operation +" " + dns;

       }

       System.out.println(message);
       byte[] buffer = message.getBytes();

       /*DatagramPacket*/ packet = new DatagramPacket(buffer, buffer.length, adver_addr, adver_port);
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
        System.out.println("> " + operation + dns + " : " + message_received);
       }
       else{
        System.out.println("> " + operation + dns + ip_address + " : " + message_received);
       }
       socket.close();
    }


    public static boolean check_arguments(String[] args){

        if(args.length != 4){

            System.out.println("> java client <mcast_addr> <mcast_port> <oper> <opnd> ");
            return false;
        }

        mcast_addr = args[0];
        mcast_port = Integer.parseInt(args[1]);
        String aux_op = args[2].toLowerCase();

        if(aux_op.equals("lookup"))
            operation = "lookup";
        else if(aux_op.equals("register"))
            operation = "register";

        if(operation.equals("lookup")){
            java.util.List<String> args_list = Arrays.asList(args[3].split(","));
            
            if(args_list.size() != 1){
                System.out.println("> <DNS name> for lookup");
                return false;
            }
            
            dns = args_list.get(0);
        }
            
        else if(operation.equals("register")){
            java.util.List<String> args_list = Arrays.asList(args[3].split(","));
            
            if(args_list.size() != 2){
                System.out.println("> <DNS name> <IP address> for register");
                return false;
            }

            dns = args_list.get(0);
            ip_address= args_list.get(1);
        }

        return true;
    }

}