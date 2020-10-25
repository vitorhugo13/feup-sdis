package chord;

import java.math.BigInteger;

public class NodeInfo{

    private BigInteger key;
    private String ip;
    private int port;

    public NodeInfo(BigInteger key, String ip, int port){
        this.key = key;
        this.ip = ip;
        this.port = port;
    }

    public void setKey(BigInteger key){
        this.key = key;
    }

    public void setIp(String ip){
        this.ip = ip;
    }

    public void setPort(int port){
        this.port = port;
    }

    public BigInteger getKey(){
        return this.key;
    }

    public String getIp(){
        return this.ip;
    }

    public int getPort(){
        return this.port;
    }

    @Override
    public String toString() {
        return "Ip address: " + this.ip + " Port: " + this.port;
    }
}