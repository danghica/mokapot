package xyz.acygn.mokapot.benchmarksuite.programs.transforminturns;

import java.rmi.Remote;
import java.rmi.RemoteException;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * @author Kelsey McKenna
 */
public interface Transformer extends Remote, NonCopiable {

    /**
     * Sets the remote transformer to which this transformer is paired. The call
     * only needs to be issued on one transformer with the other being passed as
     * argument, as it handles setup for both.
     *
     * @param transformerPair the remote transformer to pair to
     */
    void pair(Transformer transformerPair) throws RemoteException;

    /**
     * Removes the pairing to a remote tranformer if one exists.
     *
     * @throws RemoteException if there was an error performing a remote call.
     */
    void unpair() throws RemoteException;

    /**
     * Checks whether the transformer has been paired.
     *
     * @return true if paired, false if not paired
     */
    boolean paired() throws RemoteException;

    /**
     * Applies an arbitrary transformation on the string passed as argument, and then hands
     * the transformed string over to the paired transformer for further m. The code
     * is effectively recursive over the network.
     *
     * @param target string to transform
     * @param times  how many times to transform string (recursion countdown)
     * @return transformed string
     */
    String transform(String target, int times) throws RemoteException;

    Transformer getPartner() throws RemoteException;

}
