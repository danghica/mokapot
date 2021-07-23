package xyz.acygn.mokapot.benchmarksuite.programs.checkequality;

import java.rmi.Remote;
import java.rmi.RemoteException;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * This class is for the benefit of the RMI code, which
 * needs an interface extending Remote in order to cast
 * remote objects. The field representing a remote object
 * in the benchmarks should be of this type.
 *
 * @author Kelsey McKenna
 */
public interface ValueHolder extends Remote, NonCopiable {

    /**
     * Returns the value assigned to this object.
     *
     * @return value
     */
    int getValue() throws RemoteException;

}
