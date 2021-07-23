package xyz.acygn.mokapot.benchmarksuite.programs.createcollection;

import java.rmi.Remote;
import java.rmi.RemoteException;
import xyz.acygn.mokapot.markers.NonCopiable;

public interface Generator extends Remote, NonCopiable {

    /**
     * Generates a new collection of ExampleObjects.
     *
     * @return collection of ExampleObjects
     */
    ICollection<ExampleObject> generate(int seed, int maxCollectionSize) throws RemoteException;

}
