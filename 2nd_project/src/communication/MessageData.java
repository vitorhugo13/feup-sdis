package communication;

import java.math.BigInteger;
import java.util.Arrays;

public class MessageData {

    private String protocol_version;
    private String type;
    private int peer;
    private String fileID;
    private int chunkID;
    private int rep_deg;
    private byte[] chunk;
    private String header;
    private int senderPort;

    //Chord specific fields
    private BigInteger key;
    private String ipAddress;
    private int port;

    public MessageData(byte[] message_data, int message_length){
        int header_length = message_length;
        int state = 0;
        for(int i = 0; i < message_length; i++){
            if(message_data[i] == 0xD && state == 0)
                state++;
            else if(message_data[i] == 0xA && state == 1)
                state++;
            else if(message_data[i] == 0xD && state == 2)
                state++;
            else if(message_data[i] == 0xA && state == 3){
                header_length = i + 1;
                break;
            }
            else if(state > 0)
                state = 0;
        }

        header = new String(Arrays.copyOf(message_data, header_length));

        if(header_length < message_length) {
            chunk = new byte[message_length - header_length];
            System.arraycopy(message_data, header_length, chunk, 0, chunk.length);
        }

        String[] header_lines = header.split("\r\n");
        String[] header_elements = header_lines[0].split("[ ]+");

        for (int i = 0; i < header_elements.length; i++){
            if(i == 0){
                protocol_version = header_elements[i];
            }
            if(i == 1){
                type = header_elements[i];
            }
            if(i == 2){
                peer = Integer.parseInt(header_elements[i]);
            }
            if(i == 3){
                senderPort = Integer.parseInt(header_elements[i]);
            }
            if(i == 4){
                if(type.equals("SUCCESSOR") || type.equals("FINDSUCCESSOR") || type.equals("PREDECESSOR") || type.equals("NOTIFYSUCCESSOR")){
                    //noinspection StringOperationCanBeSimplified
                    key = new BigInteger(new String(header_elements[i]));
                }
                else{
                    fileID = header_elements[i];
                }
            }
            if(i == 5){
                if(type.equals("SUCCESSOR") || type.equals("FINDSUCCESSOR") || type.equals("PREDECESSOR") || type.equals("NOTIFYSUCCESSOR")){
                    ipAddress = header_elements[i];
                }
                else {
                    chunkID = Integer.parseInt(header_elements[i]);
                }
            }
            if(i == 6){
                if(type.equals("SUCCESSOR") || type.equals("FINDSUCCESSOR") || type.equals("PREDECESSOR") || type.equals("NOTIFYSUCCESSOR")){
                    port = Integer.parseInt(header_elements[i]);
                }
                else {
                    rep_deg = Integer.parseInt(header_elements[i]);
                }

            }
        }

    }

    public String getProtocol_version() {
        return protocol_version;
    }

    public String getType() {
        return type;
    }

    public int getPeer() {
        return peer;
    }

    public String getFileID() {
        return fileID;
    }

    public int getChunkID() {
        return chunkID;
    }

    public int getRep_deg() {
        return rep_deg;
    }

    public byte[] getChunk() {
        return chunk;
    }

    public String getHeader() {
        return header;
    }

    public BigInteger getKey() {
        return key;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public int getSenderPort() {
        return senderPort;
    }
}
