package testing;

import data.Chunk;
import data.DataFile;
import data.PeerDataBase;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;




public class TestStorage{

    public static void main(String args[]) throws NoSuchAlgorithmException, FileNotFoundException, IOException{

        long nmr = 1000000000;
        PeerDataBase db = new PeerDataBase(nmr, 10);
        DataFile file = new DataFile("./TestFiles/test.pdf", 8);
        DataFile file2 = new DataFile("./TestFiles/proj1_sdis.pdf", 5);

        DataFile file3 = new DataFile("./TestFiles/13names.pdf", 5);

        System.out.println("MAX SPACE: " + db.get_maximum_space());
        System.out.println("SPACE USED: " + db.get_occupied_space());

        System.out.println("-----ADDING FILE-----");
        db.add_file(file2);
        db.add_file(file3);
        db.increase_rep_deg(file2.get_fileID(), 0);
        db.increase_rep_deg(file3.get_fileID(), 0);
        db.increase_rep_deg(file3.get_fileID(), 0);
        db.increase_rep_deg(file3.get_fileID(), 0);
        db.increase_rep_deg(file3.get_fileID(), 0);
        db.increase_rep_deg(file3.get_fileID(), 1);
        db.increase_rep_deg(file3.get_fileID(), 1);
        db.increase_rep_deg(file3.get_fileID(), 1);
        db.decrease_rep_deg(file3.get_fileID(), 1);

        byte[] a1 = "This is a test to occupied space.".getBytes();
        byte[] a2 = null;
        byte[] a3 = "This is a test.".getBytes();

        Chunk ck1 = new Chunk(65, file.get_fileID(), a1, file.get_replication_degree() );

        Chunk ck2 = new Chunk(37, file.get_fileID(), a2, file.get_replication_degree() );

        Chunk ck3 = new Chunk(3, file.get_fileID(), a3, file2.get_replication_degree() );


        db.add_chunk_from_other(ck1);
        System.out.println("SPACE USED: " + db.get_occupied_space());

        db.add_chunk_from_other(ck2);

        System.out.println("SPACE USED: " + db.get_occupied_space());

        db.add_chunk_from_other(ck3);
        System.out.println("SPACE USED: " + db.get_occupied_space());

        db.increase_other_rep_deg(file.get_fileID(), 65);
        db.increase_other_rep_deg(file.get_fileID(), 65);
        db.increase_other_rep_deg(file.get_fileID(), 65);
        db.increase_other_rep_deg(file.get_fileID(), 65);
        db.increase_other_rep_deg(file.get_fileID(), 65);
        db.decrease_other_rep_deg(file.get_fileID(), 65);
        db.increase_other_rep_deg(file.get_fileID(), 3);

        //db.delete_chunk_of_other(file.get_fileID());



        System.out.println("-----FILE ADDED-----");


        System.out.println("-----FILES-----");
        ArrayList<DataFile> files = db.get_files();
        for(int i = 0; i < files.size(); i++){
            System.out.println(files.get(i).get_fileID());
        }


        /* IMPORTANT: https://beginnersbook.com/2013/12/how-to-loop-hashmap-in-java/ */
        System.out.println("-----CHUNKS WITH REPLICATION DEGREE-----");
        ConcurrentHashMap<String, Integer> chunk_deg = db.get_chunk_rep_deg();

        Set set = chunk_deg.entrySet();
        Iterator iterator = set.iterator();
        while(iterator.hasNext()) {
            Map.Entry mentry = (Map.Entry) iterator.next();
            System.out.print("key is: "+ mentry.getKey()+ " & Value is: ");
            System.out.println(mentry.getValue());
        }
        




        System.out.println("-----OTHERS CHUNKS-----");
        ArrayList<Chunk> chunkOT = db.get_other_chunks();

        for(int i = 0; i < chunkOT.size(); i++){
            System.out.println(chunkOT.get(i).get_chunkID() + " - " + chunkOT.get(i).get_replication_degree());
        }



        System.out.println("----- OTHER CHUNKS WITH REPLICATION DEGREE-----");
        ConcurrentHashMap<String, Integer> chunk_deg2 = db.get_other_rep_deg();

        Set set2 = chunk_deg2.entrySet();
        Iterator iterator2 = set2.iterator();
        while(iterator2.hasNext()) {
            Map.Entry mentry2 = (Map.Entry) iterator2.next();
            System.out.print("key is: "+ mentry2.getKey()+ " & Value is: ");
            System.out.println(mentry2.getValue());
        }


       System.out.println("SPACE USED: " + db.get_occupied_space());


    }

}