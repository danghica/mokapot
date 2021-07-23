package xyz.acygn.mokapot.benchmarksuite.programs.uglynumber;

import java.rmi.Remote;
import java.rmi.RemoteException;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * @author Kelsey McKenna
 */
public interface Checker extends Remote, NonCopiable {

    /**
     * Checks if the given number is ugly, i.e. only has any combination of 2s, 3s,
     * and 5s as factors.
     *
     * @param num potentially ugly number
     * @return true if ugly, false otherwise
     */
    boolean isUgly(int num) throws RemoteException;

}
