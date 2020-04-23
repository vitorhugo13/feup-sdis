import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote{

    int register(String dns, String ip) throws RemoteException;
    String lookup(String dns) throws RemoteException;

}

