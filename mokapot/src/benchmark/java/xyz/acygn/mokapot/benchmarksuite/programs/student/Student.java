package xyz.acygn.mokapot.benchmarksuite.programs.student;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Kelsey McKenna
 */
public interface Student extends Remote {

    void addModule(Module newModule) throws RemoteException;

    boolean hasModule(Module m) throws RemoteException;

}
