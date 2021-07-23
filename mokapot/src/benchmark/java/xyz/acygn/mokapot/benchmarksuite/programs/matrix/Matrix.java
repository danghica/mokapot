package xyz.acygn.mokapot.benchmarksuite.programs.matrix;

import java.rmi.Remote;
import java.rmi.RemoteException;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * @author Kelsey McKenna
 */
public interface Matrix extends Remote, NonCopiable {

    void clear() throws RemoteException;

    void add(Matrix other) throws RemoteException;

    int size() throws RemoteException;

    int[][] getElements() throws RemoteException;

    String prettyString() throws RemoteException;

}
