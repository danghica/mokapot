package xyz.acygn.mokapot.benchmarksuite.programs.naivebayes;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * Performs naive bayesian learning from the given set of training data.
 *
 * @author Kelsey McKenna
 */
public interface Learner extends Remote, NonCopiable, Serializable {

    void learn(int numberOfFeatures) throws RemoteException;

    List<Double> getResults() throws RemoteException;

}
