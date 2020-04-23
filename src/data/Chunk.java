package data;

import java.io.Serializable;


@SuppressWarnings("serial")
public class Chunk implements Serializable{


    //maximum size of a chunk is 64000 bytes
    private int chunkID;
    private String fileID;
    private byte[] body;
    private int replication_degree;

    public Chunk(int chunkID, String fileID, byte[] body, int replication_degree){

        this.chunkID = chunkID;
        this.fileID = fileID;
        this.body = body;
        this.replication_degree = replication_degree;

    }

    public int get_chunkID(){

        return this.chunkID;

    }

    public String get_fileID(){

        return this.fileID;
        
    }

    public byte[] get_body(){

        return this.body;
        
    }

    public int get_replication_degree(){

        return this.replication_degree;
        
    }
}