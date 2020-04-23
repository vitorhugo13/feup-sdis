package udp_connection;

import java.net.DatagramPacket;
import java.util.Arrays;

public class UDPMessageSender {

    private static int peerID; //unique for each peer
    private static String protocol_version;
    private static MCastChannel MC, MDB, MDR;

    public static final String ANSI_RESET = "\u001B[0m";

    public static final String ANSI_BLUE = "\u001B[34m";

    public static void setValues(int peer, String protocol, MCastChannel MCChannel, MCastChannel MDBChannel, MCastChannel MDRChannel){
        peerID = peer;
        protocol_version = protocol;
        MC = MCChannel;
        MDB = MDBChannel;
        MDR = MDRChannel;
    }

    public static void sendPUTCHUNK(String fileID, int chunkID, int rep_deg, byte[] chunk){

        byte[] data = build_message("PUTCHUNK", fileID, chunkID, rep_deg, chunk);
        MessageData msg_data = new MessageData(data, data.length);

        DatagramPacket message = new DatagramPacket(data, data.length, MDB.getAddress(), MDB.getPort());

        MDB.sendPacket(message);
        System.out.println(ANSI_BLUE + "Sent on MDB: " + ANSI_RESET + msg_data.getHeader());

    }

    public static void sendSTORED(String fileID, int chunkID){

        byte[] data = build_message("STORED", fileID, chunkID, 0, null);
        MessageData msg_data = new MessageData(data, data.length);

        DatagramPacket message = new DatagramPacket(data, data.length, MC.getAddress(), MC.getPort());

        MC.sendPacket(message);
        System.out.println(ANSI_BLUE + "Sent on MC: " + ANSI_RESET +msg_data.getHeader());
    }

    public static void sendGETCHUNK(String fileID, int chunkID){

        byte[] data = build_message("GETCHUNK", fileID, chunkID, 0, null);
        MessageData msg_data = new MessageData(data, data.length);

        DatagramPacket message = new DatagramPacket(data, data.length, MC.getAddress(), MC.getPort());

        MC.sendPacket(message);
        System.out.println(ANSI_BLUE +"Sent on MC: " +ANSI_RESET+ msg_data.getHeader());
    }

    public static void sendCHUNK(String fileID, int chunkID, byte[] chunk){

        byte[] data = build_message("CHUNK", fileID, chunkID, 0, chunk);
        MessageData msg_data = new MessageData(data, data.length);

        DatagramPacket message = new DatagramPacket(data, data.length, MDR.getAddress(), MDR.getPort());

        MDR.sendPacket(message);
        System.out.println(ANSI_BLUE + "Sent on MDR: " + ANSI_RESET+ msg_data.getHeader());
    }

    public static void sendDELETE(String fileID){

        byte[] data = build_message("DELETE", fileID, 0, 0, null);
        MessageData msg_data = new MessageData(data, data.length);

        DatagramPacket message = new DatagramPacket(data, data.length, MC.getAddress(), MC.getPort());

        MC.sendPacket(message);
        System.out.println(ANSI_BLUE + "Sent on MC: "+ ANSI_RESET + msg_data.getHeader());
    }

    public static void sendREMOVED(String fileID, int chunkID){

        byte[] data = build_message("REMOVED", fileID, chunkID, 0, null);
        MessageData msg_data = new MessageData(data, data.length);

        DatagramPacket message = new DatagramPacket(data, data.length, MC.getAddress(), MC.getPort());

        MC.sendPacket(message);
        System.out.println(ANSI_BLUE + "Sent on MC: " +ANSI_RESET+ msg_data.getHeader());
    }

    public static void sendCONNECTED(){
        byte[] data = build_message("CONNECTED", null, 0, 0, null);
        MessageData msg_data = new MessageData(data, data.length);

        DatagramPacket message = new DatagramPacket(data, data.length, MC.getAddress(), MC.getPort());

        MC.sendPacket(message);
        System.out.println(ANSI_BLUE + "Sent on MC: " + ANSI_RESET + msg_data.getHeader());
    }

    private static byte[] build_message(String message_type, String fileID, int chunkID, int rep_deg, byte[] chunk){
        String header_base;
        if(message_type.equals("CONNECTED")){
            header_base = protocol_version + " " + message_type + " " + peerID;
        }
        else {
            header_base = protocol_version + " " + message_type + " " + peerID + " " + fileID;
        }
        String header;
        if(message_type.equals("PUTCHUNK"))
            header = header_base + " " + chunkID + " " + rep_deg + "\r\n\r\n";
        else if (!(message_type.equals("DELETE") || message_type.equals("CONNECTED")))
            header = header_base + " " + chunkID + "\r\n\r\n";
        else
            header = header_base + "\r\n\r\n";

        byte[] message_data;

        if(message_type.equals("PUTCHUNK") || message_type.equals("CHUNK"))
            message_data = concat_arrays(header.getBytes(), chunk);
        else
            message_data = header.getBytes();

        return message_data;
    }

    private static byte[] concat_arrays(byte[] first, byte[] second) {
        byte[] result;
        if(second ==null){
            result = Arrays.copyOf(first, first.length);
        }
        else{
            result = Arrays.copyOf(first, first.length + second.length);
            System.arraycopy(second, 0, result, first.length, second.length);
        }
        return result;
    }
}
