package xyz.acygn.mokapot.benchmarksuite.programs.naivebayes;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * @author Kelsey McKenna
 */
public interface Generator extends Remote, NonCopiable {

    Set<Email> generate(int seed, int sampleSize, int numberOfFeatures) throws RemoteException;

}
