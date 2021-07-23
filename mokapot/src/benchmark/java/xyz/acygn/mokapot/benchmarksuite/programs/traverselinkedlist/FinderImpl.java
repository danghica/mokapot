package xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist;

import java.rmi.RemoteException;
import java.util.Objects;

/**
 * @author Kelsey McKenna
 */
public class FinderImpl<T extends Comparable<? super T>> implements Finder<T> {

    private final Cell<T> linkedList;

    public FinderImpl() {
        this.linkedList = null;
    }

    public FinderImpl(Cell<T> linkedList) {
        this.linkedList = linkedList;
    }

    @Override
    public <S extends Comparable<? super S>> S findLargestIndex(Cell<S> list) throws RemoteException {
        Objects.requireNonNull(list);

        S greatestSoFar = list.value();

        for (Cell<S> pointer = list; pointer != null; pointer = pointer.next()) {
            if (pointer.value().compareTo(greatestSoFar) > 0) {
                greatestSoFar = pointer.value();
            }
        }

        return greatestSoFar;
    }

    @Override
    public T findLargestIndex() throws RemoteException {
        Objects.requireNonNull(linkedList,
                "You must only call this method if a " +
                        "list was given for the construction of this object");

        return findLargestIndex(linkedList);
    }

}
