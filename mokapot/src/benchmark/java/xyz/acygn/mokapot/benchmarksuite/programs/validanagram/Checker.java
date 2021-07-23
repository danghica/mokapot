package xyz.acygn.mokapot.benchmarksuite.programs.validanagram;

import java.rmi.Remote;
import java.rmi.RemoteException;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * @author Kelsey McKenna
 */
public interface Checker extends Remote, NonCopiable {

    void pair(Checker partner) throws RemoteException;

    void unpair() throws RemoteException;

    void checkAnagram(String a, String b) throws RemoteException;

    boolean isAnagram() throws RemoteException;

    boolean verifyAgreement() throws RemoteException;

}
