//file that will be used as a test for the function file_into_chunks
package testing;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.*;


import data.DataFile;

public class TestSplit{
   
    public static void main(String args[]) throws NoSuchAlgorithmException, FileNotFoundException, IOException{

        DataFile file = new DataFile(args[0], Integer.parseInt(args[1]));

        String fileID = file.get_fileID();
        System.out.println("File ID: " + fileID);

        //iterate through chunks
        ArrayList<String> chunks = file.get_chunks();
        int number_chunks = chunks.size();
        System.out.println("Number of chunks: " + number_chunks);

        for(int i = 0; i < number_chunks; i++){

            System.out.println("-------------------------- ANALYZING  CHUNK " + i + " -------------------------------------");
            String ck = chunks.get(i);
            java.util.List<String> args_list = Arrays.asList(ck.split(":"));
            System.out.println("FILE ID: " + args_list.get(0));
            System.out.println("ID: " + args_list.get(1));
            System.out.println("REPLICATION DEGREE: " + file.get_replication_degree());
            
        }
    }
}