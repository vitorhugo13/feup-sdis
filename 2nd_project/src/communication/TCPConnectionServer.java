package communication;

import chord.Chord;
import chord.NodeInfo;
import data.PeerDataBase;
import data.Chunk;
import utility.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;

public class TCPConnectionServer implements Runnable {
    private TcpInterface tcpInterface;
    private AtomicBoolean running;
    private PeerDataBase dataBase;
    private Chord chord;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";

    public TCPConnectionServer (TcpInterface tcpInterface, PeerDataBase database, Chord chord) {
        this.tcpInterface = tcpInterface;
        running = new AtomicBoolean();
        this.dataBase = database;
        this.chord = chord;
    }

    public void start(){
        Thread thread = new Thread(this);
        tcpInterface.start();
        thread.start();
    }

    public void stop() {
        running.set(false);
    }

    @Override
    public void run() {
        byte[] message = tcpInterface.receive(0);

        MessageData messageInfo = new MessageData(message, message.length);
        switch(messageInfo.getType()){
            case "PUTCHUNK":{
                putchunk(messageInfo);
                break;
            }
            case "GETCHUNK":{
                getchunk(messageInfo);
                break;
            }
            case "DELETE":{
                delete(messageInfo);
            }
            case "REMOVED":{
                removed(messageInfo);
            }
            case "HELLO":{
                hello();
                break;
            }
            case "NOTIFYSUCCESSOR":{
                notifySuccessor(messageInfo);
                break;
            }
            case "FINDSUCCESSOR":{
                findSuccessor(messageInfo);
                break;
            }
            case "YOURSUCCESSOR":{
                mySuccessor();
                break;
            }
            case "FINDPREDECESSOR":{
                findPredecessor();
                break;
            }
            default:{
                log("unexpected message type <" + messageInfo.getType() + ">, nothing will be done.");
                break;
            }
        }
    }

    public void hello(){
        log("received HELLO message, sending response.");
        byte[] response = MessageBuilder.buildHELLORESPONSE();
        tcpInterface.send(response);
    }

    public void notifySuccessor(MessageData data){
        log("received NOTIFYSUCCESSOR message.");
        NodeInfo successor = new NodeInfo(data.getKey(), data.getIpAddress(), data.getPort());
        chord.notify_nodes(successor);
    }

    public void findSuccessor(MessageData data){
        log("received FINDSUCCESSOR message.");
        BigInteger key = data.getKey();
        log("attempting to find successor <" + key + ">.");
        NodeInfo successor = chord.find_successor(key);

        if(successor != null){
            log("found successor, sending info.");
            byte[] response = MessageBuilder.buildSUCCESSOR(successor);
            tcpInterface.send(response);
        }
    }

    public void findPredecessor(){
        log("received FINDPREDECESSOR message, sending info.");
        NodeInfo predecessor = chord.getPredecessor();
        byte[] response = MessageBuilder.buildPREDECESSOR(predecessor);
        tcpInterface.send(response);
    }

    public void mySuccessor(){
        log("received YOURSUCCESSOR message, sending info.");
        NodeInfo succ = chord.getFingerTable().get(0);

        if(succ == null){
            succ = chord.getNode();
        }

        byte[] response = MessageBuilder.buildSUCCESSOR(succ);
        tcpInterface.send(response);
    }

    public void putchunk(MessageData data){
        log("received PUTCHUNK message, processing.");
        Chunk chunk = new Chunk(data.getChunkID(), data.getFileID(), data.getChunk(), data.getRep_deg());
        String ip = tcpInterface.getConnectionInfo().getIp();
        

        if(dataBase.add_chunk_from_other(chunk, ip, data.getSenderPort())){
            byte[] message = MessageBuilder.buildSTORED(data.getFileID(), data.getChunkID());
            tcpInterface.send(message);

            String chunk_path = "./peer_disk/peer" + chord.getPeerID() + "/backup/" + chunk.get_fileID() +  "/" + chunk.get_chunkID();
            File file = new File(chunk_path);

            try {
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }

                FileOutputStream fileOutput = new FileOutputStream(file);
                ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput);

                objectOutput.writeObject(chunk.get_body());

                objectOutput.close();
                fileOutput.close();
            }
            catch(IOException exception){
                exception.printStackTrace();
            }
        }

    }

    public void getchunk(MessageData data){
        log("received GETCHUNK message, processing.");

        String fileID = data.getFileID();
        int chunkID = data.getChunkID();
        //String ip = tcpInterface.getConnectionInfo().getIp();
        //int port = tcpInterface.getConnectionInfo().getPort();

        if(dataBase.have_other_chunk(fileID, chunkID)){
            byte[] message = MessageBuilder.buildCHUNK(data.getFileID(), data.getChunkID(), dataBase.get_other_chunk_body(fileID, chunkID));
            tcpInterface.send(message);
        }

    }

    public void delete(MessageData data){
        log("received DELETE message, processing.");
        String fileID = data.getFileID();

        if(dataBase.delete_chunk_of_other(fileID)){

            String file_path = "./peer_disk/peer" + chord.getPeerID() + "/backup/" + fileID;
            File file = new File(file_path);

            if (file.exists()){
                String[]entries = file.list();
                for(String s: entries){
                    File currentFile = new File(file.getPath(), s);
                    currentFile.delete();
                }

                file.delete();
            }

            System.out.println(ANSI_GREEN + "DELETED CHUNKS OF FILE WITH ID: " + ANSI_RESET + fileID );
        }

    }

    public void removed(MessageData data){
        log("received REMOVED message, processing.");
        String fileID = data.getFileID();
        int chunkID = data.getChunkID();
        byte[] bodyCK = data.getChunk();

        if(dataBase.my_chunk(fileID, chunkID)){
            dataBase.decrease_rep_deg(fileID, chunkID);

            if(!dataBase.desired_rep_degree(fileID, chunkID)){
                int rd = dataBase.obtain_my_chunk_rd(fileID, chunkID);
                Chunk chunk = new Chunk(chunkID, fileID, bodyCK, rd);

                TCPBackupSenderThread chunkSender = new TCPBackupSenderThread(chunk, dataBase, chord);
                chunkSender.start();
                
            }
        }
    }

    private void log(String logMessage){
        NodeInfo info = tcpInterface.getConnectionInfo();
        System.out.println("Server side (connected to " + info.getIp() + ":" + info.getPort() + "): " + logMessage);
    }
}
