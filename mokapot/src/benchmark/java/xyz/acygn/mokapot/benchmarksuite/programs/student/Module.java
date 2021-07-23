package xyz.acygn.mokapot.benchmarksuite.programs.student;

import java.rmi.Remote;
import java.rmi.RemoteException;
import xyz.acygn.mokapot.markers.Copiable;

/**
 * @author Kelsey McKenna
 */
public interface Module extends Remote, Copiable {

    String getName() throws RemoteException;

    int getId() throws RemoteException;

}
