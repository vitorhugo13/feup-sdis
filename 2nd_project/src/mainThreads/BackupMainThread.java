package mainThreads;

import data.DataFile;
import data.PeerDataBase;
import chord.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class BackupMainThread extends Thread {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";

    private String path_of_file;
    private int replication_degree;
    private PeerDataBase database;
    private Chord chord;
    private String protocol_version;


    public BackupMainThread(String path_of_file, int replication_degree, PeerDataBase database, Chord chord, String protocol_version){
        this.path_of_file = path_of_file;
        this.replication_degree = replication_degree;
        this.database = database;
        this.chord = chord;
        this.protocol_version = protocol_version;
    }

    public void run() {

        try {

            DataFile backup_file = new DataFile(path_of_file, replication_degree, chord, database);
            database.add_file(backup_file);
            backup_file.send_chunks(database);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
