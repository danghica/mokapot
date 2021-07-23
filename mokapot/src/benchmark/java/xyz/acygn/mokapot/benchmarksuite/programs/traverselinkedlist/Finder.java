package xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist;

import java.rmi.Remote;
import java.rmi.RemoteException;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * @author Kelsey McKenna
 */
public interface Finder<T extends Comparable<? super T>> extends Remote, NonCopiable {

    /**
     * Traverses the given linked list and returns the largest element as defined by the natural order
     * of the element type.
     *
     * @param list list to traverse
     * @param <S>  element type with natural order
     * @return largest element
     * @throws NullPointerException if passed a null list
     * @throws RemoteException      if an error occurred processing a remote method invocation
     */
    <S extends Comparable<? super S>> S findLargestIndex(Cell<S> list) throws RemoteException;

    T findLargestIndex() throws RemoteException;

}