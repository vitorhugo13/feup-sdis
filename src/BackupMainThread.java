import data.DataFile;
import data.PeerDataBase;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class BackupMainThread extends Thread {

    private String path_of_file;
    private int replication_degree;
    private PeerDataBase database;
    private String protocol_version;

    public BackupMainThread(String path_of_file, int replication_degree, PeerDataBase database, String protocol_version){
        this.path_of_file = path_of_file;
        this.replication_degree = replication_degree;
        this.database = database;
        this.protocol_version = protocol_version;
    }

    public void run() {

        try {

            DataFile backup_file = new DataFile(path_of_file, replication_degree);
            database.add_file(backup_file);

            if(protocol_version.equals("2.0"))
                database.remove_from_delete(backup_file.get_fileID());

            backup_file.send_chunks(database);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
