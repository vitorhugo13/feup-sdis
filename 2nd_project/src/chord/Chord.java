//CHORD paper: https://dl.acm.org/doi/pdf/10.1109/TNET.2002.808407?download=true
//Node failure (slide 57): https://www.kth.se/social/upload/51647996f276545db53654c0/3-chord.pdf

package chord;

import communication.MessageBuilder;
import communication.MessageData;
import communication.TCPSocketThread;

import java.io.IOException;
import java.lang.Math;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class Chord{


    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_PURPLE = "\u001B[35m";


    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private final static int M = 128; 
    private final static int R = logarithmic(Math.pow(2, 128), 10);

    private NodeInfo node;
    private AtomicReferenceArray<NodeInfo> finger_table;
    private ArrayList<NodeInfo> successors_list;
    private NodeInfo predecessor;
    private int next = -1;
    private int peerID;
    protected ScheduledThreadPoolExecutor executorService = null;
    
    public Chord(int peerID, int port) throws UnknownHostException{

        String ip = InetAddress.getLocalHost().getHostAddress();
        byte[] hashed_md5 = hash_nodeID(ip, peerID, port);
        BigInteger nodeID = new BigInteger(1, hashed_md5);

        this.node = new NodeInfo(nodeID, ip, port);
        this.finger_table = new AtomicReferenceArray<>(M);
        this.successors_list = new ArrayList<>();
        this.peerID = peerID;
        
        for(int i = 0; i < M; i++){
            this.finger_table.set(i, this.node);
        }

        for(int i = 0; i < R; i++){
            this.successors_list.add(i, this.node);
        }    
    }

    public boolean canJoin(String ip, int port){
         boolean canJoin = false;

        try {
            TCPSocketThread successorConnection = new TCPSocketThread(ip, port);
            successorConnection.start();
            byte[] message = MessageBuilder.buildHELLO();

            successorConnection.send(message);
            byte[] response = successorConnection.receive(3000);

            if(response != null)
                canJoin = true;
        }
        catch(IOException e){
            System.out.println(e.getMessage());
        }

        return canJoin;
    }

    public void create(){
        this.predecessor = null;
        this.finger_table.set(0, this.node);
        this.successors_list.set(0, this.node);
        runPeriodically();
    }

    public void runPeriodically(){
        executorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(3);
        executorService.scheduleAtFixedRate(new Mantainer(this, "stabilize"), 2000, 2500, TimeUnit.MILLISECONDS);
        executorService.scheduleAtFixedRate(new Mantainer(this, "check_predecessor"), 2500, 5000, TimeUnit.MILLISECONDS);
        executorService.scheduleAtFixedRate(new Mantainer(this, "fix_fingers"), 3000, 3000, TimeUnit.MILLISECONDS);
    }

    /* when we join a ring we must pass ipAddress and port of a node already on the chord(and active) */
    public void join(String ip, int port){

        this.predecessor = null;
        NodeInfo successor = null;

        try {
            TCPSocketThread successorConnection = new TCPSocketThread(ip, port);
            successorConnection.start();
            byte[] message = MessageBuilder.buildFINDSUCCESSOR(this.node.getKey());

            successorConnection.send(message);
            byte[] response = successorConnection.receive(3000);
            MessageData responseData = new MessageData(response, response.length);

            if(response != null)
                successor =  new NodeInfo(responseData.getKey(), responseData.getIpAddress(), responseData.getPort());
        }
        catch(IOException e){
            e.printStackTrace();
        }

        this.finger_table.set(0, successor);
        this.successors_list.set(0, successor);
        runPeriodically();
    }

    public void fix_fingers(){

        System.out.println(ANSI_CYAN + "FIX FINGERS CALLED" + ANSI_RESET);
        next += 1;

        if(next > M)
            next = 0; 

        BigInteger k = BigDecimal.valueOf(Math.pow(2,next)).toBigInteger();
        BigInteger module = BigDecimal.valueOf(Math.pow(2,M)).toBigInteger();
        BigInteger identifier = this.node.getKey().add(k).mod(module);
        NodeInfo succ = find_successor(identifier);

        this.finger_table.set(next, succ);

    }

    
    public void stabilize(){

        System.out.println(ANSI_GREEN + "STABILIZE CALLED" + ANSI_RESET);

        this.updateSuccessorsList();

        NodeInfo succ_predecessor = null;

        if(this.finger_table.get(0).getIp() == this.node.getIp() && this.finger_table.get(0).getPort() == this.node.getPort() && this.finger_table.get(0).getKey() == this.node.getKey()){
            succ_predecessor = this.predecessor;
        }
        else{
            try {
                TCPSocketThread successorConnection = new TCPSocketThread(this.finger_table.get(0).getIp(), this.finger_table.get(0).getPort());
                successorConnection.start();
                byte[] message = MessageBuilder.buildFINDPREDECESSOR();

                successorConnection.send(message);
                byte[] response = successorConnection.receive(3000);

                if(response != null){
                    MessageData responseData = new MessageData(response, response.length);
                    succ_predecessor = new NodeInfo(responseData.getKey(), responseData.getIpAddress(), responseData.getPort());
                }
            }
            catch(NullPointerException | IOException e){
                e.printStackTrace();
            }
        }

        if(succ_predecessor != null){
            if(compareBigIntegers(this.node.getKey(), succ_predecessor.getKey(),this.finger_table.get(0).getKey())){
                this.finger_table.set(0, succ_predecessor);
                this.successors_list.set(0, succ_predecessor);
            }
        }

        try {

            TCPSocketThread successorConnection = new TCPSocketThread(this.finger_table.get(0).getIp(), this.finger_table.get(0).getPort());
            successorConnection.start();
            byte[] message = MessageBuilder.buildNOTIFYSUCCESSOR(this.node);
            successorConnection.send(message);
            
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    public void notify_nodes(NodeInfo n){
        System.out.println(ANSI_PURPLE + "NOTIFY CALLED" + ANSI_RESET);

        if(this.predecessor == null || compareBigIntegers(this.predecessor.getKey(), n.getKey(), this.node.getKey())){ 
            this.predecessor = n;
        }

            
    }

    public void check_predecessor(){
        System.out.println(ANSI_RED + "CHECK PREDECESSOR CALLED" + ANSI_RESET); 

        if(this.predecessor != null && node_failed(this.predecessor)){
            this.predecessor = null;
        }

    }


    public boolean node_failed(NodeInfo node){

        boolean failed = true;

        try {
            TCPSocketThread successorConnection = new TCPSocketThread(node.getIp(), node.getPort());
            successorConnection.start();
            byte[] message = MessageBuilder.buildHELLO();

            successorConnection.send(message);
            byte[] response = successorConnection.receive(500); 
            if(response != null)
                failed = false;
        }
        catch(IOException e){
            System.out.println(e.getMessage());
        }
        return failed;
    }

    /*lookup functions */
    public NodeInfo find_successor(BigInteger nodeID){

        if(compareBigIntegers(this.node.getKey(), nodeID, this.finger_table.get(0).getKey()) || nodeID.compareTo(this.finger_table.get(0).getKey()) == 0){
            return this.finger_table.get(0);
        }
        else{

            NodeInfo n = closest_preceding_node(nodeID);

            if(this.node.getKey().compareTo(n.getKey()) == 0){
                return this.node;
            }

            try {
                TCPSocketThread successorConnection = new TCPSocketThread(n.getIp(), n.getPort());
                successorConnection.start();
                byte[] message = MessageBuilder.buildFINDSUCCESSOR(nodeID);

                successorConnection.send(message);
                byte[] response = successorConnection.receive(3000); 
                MessageData responseData = new MessageData(response, response.length);

                return new NodeInfo(responseData.getKey(), responseData.getIpAddress(), responseData.getPort());
            }
            catch(IOException e){
                e.printStackTrace();
                return null;
            }
        }
    }

    public NodeInfo closest_preceding_node(BigInteger nodeID){
        NodeInfo fingerNode = null;
        NodeInfo rNode = null;

        for(int i = M - 1; i >= 0; i -= 1){

            if(this.finger_table.get(i) == null)
                continue;

            if(compareBigIntegers(this.node.getKey(),this.finger_table.get(i).getKey(), nodeID)){
                fingerNode = this.finger_table.get(i);
                if(node_failed(fingerNode)){
                    fingerNode = null;
                }
                else{
                    break;
                }
            }
        }

        
        for(int j = R - 1; j >= 0; j-=1){

            if(this.successors_list.get(j) == null)
                continue;

            if(compareBigIntegers(this.node.getKey(),this.successors_list.get(j).getKey(), nodeID)){
                rNode = this.successors_list.get(j);
                if(node_failed(rNode)){
                    rNode = null;
                }
                else{
                    break;
                }
            }
        }
        

        if(fingerNode == null && rNode != null)
            return rNode;
        else if(fingerNode != null && rNode == null)
            return fingerNode;
        else if(fingerNode != null && rNode != null){
            return closestToID(nodeID, fingerNode, rNode);
        }

        
        return this.node;
    }

    public NodeInfo closestToID(BigInteger nodeID, NodeInfo finger, NodeInfo succ){

        BigInteger fingerSub = finger.getKey().subtract(nodeID);
        BigInteger succSub = succ.getKey().subtract(nodeID);

        int result = fingerSub.compareTo(succSub);

        switch(result){
            case 1:
                return succ;
            case 0:
                return finger;
            case -1:
                return finger;
            default:
                return null;

        }

    }

    public void updateSuccessorsList(){

        NodeInfo my_succ;

        for(int succ = 0; succ < this.successors_list.size(); succ++){
           
            my_succ = this.successors_list.get(succ);

            if(my_succ != null){

                try {
                    TCPSocketThread successorConnection = new TCPSocketThread(my_succ.getIp(), my_succ.getPort());
                    successorConnection.start();
                    byte[] message = MessageBuilder.buildYOURSUCCESSOR(); 

                    successorConnection.send(message);

                    byte[] response = successorConnection.receive(3000); 
    
                    if(response != null){

                        MessageData responseData = new MessageData(response, response.length);

                        NodeInfo new_succ = new NodeInfo(responseData.getKey(), responseData.getIpAddress(), responseData.getPort());

                        if(new_succ.getKey().compareTo(my_succ.getKey()) == 0){
                            break;
                        }

                        if(succ < 37)
                            this.successors_list.set(succ + 1, new_succ);
                    }
                    else{
                        this.successors_list.remove(succ);
                        this.successors_list.add(this.node); 
                        succ--;
                    }
                    
                    successorConnection.stop();
                }
                catch(IOException e){
                    e.printStackTrace();
                    this.successors_list.remove(succ);
                    this.successors_list.add(this.node); 
                    succ--;
                }
            }
        }

        if(this.successors_list.isEmpty()){
            this.successors_list.add(0, this.node);
        }

        this.finger_table.set(0, this.successors_list.get(0));
    }

    /* getters*/
    public NodeInfo getPredecessor(){
        return this.predecessor;
    }

    public int getPeerID(){
        return  this.peerID;
    }

    public AtomicReferenceArray<NodeInfo> getFingerTable(){
        return this.finger_table;
    }

    public ArrayList<NodeInfo> getSuccessorsList(){
        return this.successors_list;
    }

    public NodeInfo getNode(){
        return this.node;
    }

    /* encrypt functions*/
    public byte[] hash_nodeID(String ip, int peerID, int port){

        String toHash = ip + String.valueOf(port) + String.valueOf(peerID);

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

    /* comparison between big integers cannot be done with normal operators */
    public static boolean compareBigIntegers(BigInteger b1, BigInteger b2, BigInteger b3){ //esq, meio, dir

        if(b1.compareTo(b3) == -1){ //b1 < b3
            return b2.compareTo(b1) == 1 && b2.compareTo(b3) == -1; //b2 > b1 e b2 < b3
        }
        else if(b1.compareTo(b3) == 0){
            return true;
        }
        else{
            return b2.compareTo(b1) == 1 || b2.compareTo(b3) == -1; //b2 > b1 ou b2 < b3
        }
        
    }

    public static int logarithmic(double a, int b){
        return (int) (Math.log(a) / Math.log(b));
    }

    /* print Chord state */
    public String stateChord(){
        String state = "";

        state = state + ANSI_CYAN + "NODE KEY: " + ANSI_RESET + this.node.getKey() + "\n";
        state = state + ANSI_CYAN + "IP: " + ANSI_RESET + this.node.getIp() + "\n";
        state = state + ANSI_CYAN + "PORT: " + ANSI_RESET + this.node.getPort() + "\n";

        state = state + ANSI_RED + "M VALUE: " + ANSI_RESET + M + "\n";
        state = state + ANSI_RED + "R VALUE: " + ANSI_RESET + R + "\n";

        state = state + ANSI_PURPLE + "SUCCESSOR: " + ANSI_RESET + this.finger_table.get(0) + "\n";
        state = state + ANSI_PURPLE + "PREDECCESSOR: " + ANSI_RESET + this.predecessor + "\n";

        state = state + "\n";

        for(int i = 0; i < M; i++){
            state = state + ANSI_CYAN + "FINGER(" + i + ")" +ANSI_RESET;
            state = state + this.finger_table.get(i) + "\n";
        }

        state = state + "\n";

        for(int i = 0; i < this.successors_list.size(); i++){
            state = state + ANSI_CYAN + "SUCC(" + i + ")" +ANSI_RESET;
            state = state + this.successors_list.get(i) + "\n";
        }

        return state;
    }
}