package xyz.acygn.mokapot.rmiserver;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import xyz.acygn.mokapot.CopiableSupplier;

/**
 * @author Marcello De Bernardi
 */
public interface RMIServer extends Remote {

    <T> T execute(Executable<T> executable) throws RemoteException;

    @SuppressWarnings("unchecked")
    default <T extends Remote> T create(CopiableSupplier<T> supplier) throws RemoteException {
        return execute(port -> {
            final T t = supplier.get();

            try {
                return (T) UnicastRemoteObject.exportObject(t, port);
            } catch (RemoteException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

}
