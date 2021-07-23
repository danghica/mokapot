package xyz.acygn.mokapot.benchmarksuite.programs.createcollection;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * @author Kelsey McKenna
 */
public interface ICollection<E> extends Remote, NonCopiable, Iterable<E>, Serializable {

    void add(E e) throws RemoteException;

    void clear() throws RemoteException;

}
