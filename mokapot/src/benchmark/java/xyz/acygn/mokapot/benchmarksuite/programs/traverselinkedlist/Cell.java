package xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist;

import java.rmi.Remote;
import java.rmi.RemoteException;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * @author Kelsey McKenna
 */
public interface Cell<T> extends Remote, NonCopiable {

    T value() throws RemoteException;

    Cell<T> next() throws RemoteException;

    void setNext(Cell<T> cell) throws RemoteException;

    void clear() throws RemoteException;

}
