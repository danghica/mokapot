package xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist;

import java.rmi.Remote;
import java.rmi.RemoteException;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * @author Kelsey McKenna
 */
public interface Generator<T extends Comparable<? super T>> extends Remote, NonCopiable {

    Cell<T> generateTestList(int listSize) throws RemoteException;

}
