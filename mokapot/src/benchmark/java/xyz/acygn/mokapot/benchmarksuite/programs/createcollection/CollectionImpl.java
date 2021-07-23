package xyz.acygn.mokapot.benchmarksuite.programs.createcollection;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author Kelsey McKenna
 */
public class CollectionImpl<E> implements ICollection<E> {

    private Collection<E> collection;

    public CollectionImpl(Collection<E> collection) {
        this.collection = collection;
    }

    @Override
    public void add(E e) throws RemoteException {
        collection.add(e);
    }

    @Override
    public void clear() throws RemoteException {
        collection.clear();
    }

    @Override
    public Iterator<E> iterator() {
        return collection.iterator();
    }

}
