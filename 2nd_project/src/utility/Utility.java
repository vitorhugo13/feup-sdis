package utility;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.*;


public class Utility{
    
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static final int BUFFER_SIZE = 1024;

    public static byte[] hash_Key(String fileID, int chunk_id, int rd){

        String toHash = fileID + String.valueOf(chunk_id) + String.valueOf(rd);

        try{

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] msg_bytes = md.digest(toHash.getBytes());
            return msg_bytes;
           

        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
            return null;
        }
    }

    public static String convertHexadecimal(byte[] bytes) {
        
        char[] hexChars = new char[bytes.length * 2];

        for (int i = 0; i < bytes.length; i++){

            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];

        }

        String str = new String(hexChars);
        return str;
    }


    public static void sendMessage(String message, SSLSocket client) {
        try {
            OutputStream output = client.getOutputStream();
            output.write(message.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String readMessage(SSLSocket client) {
        try {
            InputStream input = client.getInputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            input.read(buffer);
            String message = new String(buffer);
            return message.trim();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    
}