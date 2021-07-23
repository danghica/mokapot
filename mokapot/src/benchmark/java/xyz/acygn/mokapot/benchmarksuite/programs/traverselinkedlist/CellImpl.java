package xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist;

import java.rmi.RemoteException;
import java.util.Objects;

/**
 * @author Kelsey McKenna
 */
public class CellImpl<T> implements Cell<T> {

    private final T value;
    private Cell<T> next;

    CellImpl(T value, Cell<T> next) {
        Objects.requireNonNull(value);

        this.value = value;
        this.next = next;
    }

    @Override
    public T value() {
        return value;
    }

    @Override
    public Cell<T> next() {
        return next;
    }

    @Override
    public void setNext(Cell<T> next) {
        this.next = next;
    }

    @Override
    public void clear() throws RemoteException {
        if (next != null) {
            next.clear();
        }

        next = null;
    }

}
