import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerInterface extends Remote{

    void backup(String path_of_file, int replication_degree) throws RemoteException;
    void restore(String path_of_file) throws RemoteException;
    void delete(String path_of_file) throws RemoteException;
    void reclaim(long max_disk_space) throws RemoteException;
    String state() throws RemoteException;
    String chordState() throws RemoteException;
}