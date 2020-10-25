package communication;

import java.nio.ByteBuffer;

public class MessageDelimiter {
    static int length_delimit_max = 0xffff; //arbitrary size

    public static byte[] wrapLength(byte[] message) throws Exception {
        int length = message.length;
        
        System.out.flush();
        if(length > length_delimit_max){
            throw new Exception("Cannot send message: contents too large");
        }

        byte[] delimitedMessage = ByteBuffer.allocate(length + 4).putInt(length).array();

        System.arraycopy(message, 0, delimitedMessage, 4, length);

        return delimitedMessage;
    }
}
