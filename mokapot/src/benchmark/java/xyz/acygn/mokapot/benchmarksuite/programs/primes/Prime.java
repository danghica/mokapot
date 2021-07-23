package xyz.acygn.mokapot.benchmarksuite.programs.primes;

import java.rmi.Remote;
import java.rmi.RemoteException;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * @author Kelsey McKenna
 */
public interface Prime extends NonCopiable, Remote {

    boolean isPrime(long n) throws RemoteException;

}
