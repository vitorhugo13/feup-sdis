package communication;

import chord.NodeInfo;

import java.math.BigInteger;
import java.util.Arrays;

public class MessageBuilder {

    private static int peerID; //unique for each peer
    private static String protocol_version;
    private static int serverPort;

    public static final String ANSI_RESET = "\u001B[0m";

    public static final String ANSI_BLUE = "\u001B[34m";

    public static void setValues(int peer, String protocol, int port){
        peerID = peer;
        protocol_version = protocol;
        serverPort = port;
    }

    public static byte[] buildPUTCHUNK(String fileID, int chunkID, int rep_deg, byte[] chunk){

        byte[] data = build_message("PUTCHUNK", fileID, chunkID, rep_deg, chunk);
        return data;
    }

    public static byte[] buildSTORED(String fileID, int chunkID){

        byte[] data = build_message("STORED", fileID, chunkID, 0, null);
        return data;
    }

    public static byte[] buildGETCHUNK(String fileID, int chunkID){

        byte[] data = build_message("GETCHUNK", fileID, chunkID, 0, null);
        return data;
    }

    public static byte[] buildCHUNK(String fileID, int chunkID, byte[] chunk){

        byte[] data = build_message("CHUNK", fileID, chunkID, 0, chunk);
        return data;
    }

    public static byte[] buildDELETE(String fileID){

        byte[] data = build_message("DELETE", fileID, 0, 0, null);
        return data;
    }

    public static byte[] buildREMOVED(String fileID, int chunkID, byte[] chunk){

        byte[] data = build_message("REMOVED", fileID, chunkID, 0, chunk);
        return data;
    }

    public static byte[] buildCONNECTED(){
        byte[] data = build_message("CONNECTED", null, 0, 0, null);
        return data;
    }

    public static byte[] buildSUCCESSOR(NodeInfo info){

        byte[] data = build_message("SUCCESSOR", info);
        return data;
    }

    //TODO: added to successors List
    public static byte[] buildYOURSUCCESSOR(){
        byte[] data = build_message("YOURSUCCESSOR", null, 0, 0, null);
        return data;
    }

    public static byte[] buildPREDECESSOR(NodeInfo info){
        byte[] data = build_message("PREDECESSOR", info);
        return data;
    }

    public static byte[] buildFINDSUCCESSOR(BigInteger key){
        byte[] data = build_message("FINDSUCCESSOR", key.toString(), 0, 0, null);
        return data;
    }

    public static byte[] buildFINDPREDECESSOR(){
        byte[] data = build_message("FINDPREDECESSOR", null, 0, 0, null);
        return data;
    }

    public static byte[] buildNOTIFYSUCCESSOR(NodeInfo info){
        byte[] data = build_message("NOTIFYSUCCESSOR", info);
        return data;
    }

    public static byte[] buildHELLO(){
        byte[] data = build_message("HELLO", null, 0, 0, null);
        return data;
    }

    public static byte[] buildHELLORESPONSE(){
        byte[] data = build_message("HELLORESPONSE", null, 0, 0, null);
        return data;
    }

    private static byte[] build_message(String message_type, NodeInfo info){
        String header;
        header = protocol_version + " " + message_type + " " + peerID + " " + serverPort + " " + info.getKey().toString() + " " + info.getIp() + " " + info.getPort() + "\r\n\r\n";
        
        byte[] message_data = header.getBytes();

        return message_data;
    }

    private static byte[] build_message(String message_type, String fileID, int chunkID, int rep_deg, byte[] chunk){
        String header_base;
        if(message_type.equals("CONNECTED") || message_type.equals("FINDPREDECESSOR") || message_type.equals("YOURSUCCESSOR") || message_type.equals("HELLO") || message_type.equals("HELLORESPONSE")){
            header_base = protocol_version + " " + message_type + " " + peerID;
        }
        else {
            header_base = protocol_version + " " + message_type + " " + peerID + " " + serverPort + " " + fileID;
        }
        String header;
        if(message_type.equals("PUTCHUNK"))
            header = header_base + " " + chunkID + " " + rep_deg + "\r\n\r\n";
        else if (!(message_type.equals("DELETE") || message_type.equals("CONNECTED") || message_type.equals("FINDSUCCESSOR") || message_type.equals("FINDPREDECESSOR") || message_type.equals("YOURSUCCESSOR") ||  message_type.equals("HELLO") || message_type.equals("HELLORESPONSE")))
            header = header_base + " " + chunkID + "\r\n\r\n";
        else
            header = header_base + "\r\n\r\n";

        byte[] message_data;

        if(message_type.equals("PUTCHUNK") || message_type.equals("CHUNK") || message_type.equals("REMOVED"))
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

    private static void log(String header){
        System.out.println("Builder: built <" + header + ">");
    }
}
