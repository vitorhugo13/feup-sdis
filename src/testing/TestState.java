package testing;

import data.Chunk;
import data.DataFile;
import data.PeerDataBase;

import java.io.*;
import java.security.NoSuchAlgorithmException;


public class TestState{

  
    public static void main(String args[]) throws NoSuchAlgorithmException, FileNotFoundException, IOException{

        PeerDataBase storage = new PeerDataBase(10000000, 2);

       
        /* My files */
        DataFile file1 = new DataFile("./TestFiles/proj1_sdis.pdf", 2);
        DataFile file2 = new DataFile("./TestFiles/13names.pdf", 1);
        DataFile file4 = new DataFile("./TestFiles/cancelar.txt", 1);

        


        /* Stuff needed to store chunks of other files */
        DataFile file3 = new DataFile("./TestFiles/test.pdf", 4);

        byte[] a1 = "This is a test to state subprotocol.".getBytes();
        byte[] a2 = "This is a test.".getBytes();
        byte[] a3 = "I love SDIS. It's my favourite UC @FEUP".getBytes();
        byte[] a4 = null;

        Chunk ck1 = new Chunk(1, file3.get_fileID(), a1, file3.get_replication_degree());
        Chunk ck2 = new Chunk(5, file3.get_fileID(), a2, file3.get_replication_degree());
        Chunk ck3 = new Chunk(8, file3.get_fileID(), a3, file3.get_replication_degree());
        Chunk ck4 = new Chunk(14, file3.get_fileID(), a4, file3.get_replication_degree());


        /* Add to storage */
        storage.add_file(file1);
        storage.add_file(file2);
        storage.add_file(file4);


        storage.increase_rep_deg(file2.get_fileID(), 0);
        storage.increase_rep_deg(file2.get_fileID(), 0);
        storage.increase_rep_deg(file2.get_fileID(), 1);

        storage.add_chunk_from_other(ck1);
        storage.add_chunk_from_other(ck2);
        storage.add_chunk_from_other(ck3);
        storage.add_chunk_from_other(ck4);
        storage.increase_other_rep_deg(file3.get_fileID(), 8);
        storage.increase_other_rep_deg(file3.get_fileID(), 8);
        storage.decrease_other_rep_deg(file3.get_fileID(), 1);


        String state = storage.state();
        System.out.println(state);
    }
}