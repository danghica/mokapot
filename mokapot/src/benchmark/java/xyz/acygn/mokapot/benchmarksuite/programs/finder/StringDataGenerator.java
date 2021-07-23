package xyz.acygn.mokapot.benchmarksuite.programs.finder;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * @author Kelsey McKenna
 */
public interface StringDataGenerator extends Remote {

    /**
     * Create a list with generated randomly words.
     *
     * @param size The number of words that are going to be added to the list.
     * @return A list of randomly generated words.
     */
    List<String> generateRandomWordList(int size) throws RemoteException;

    String generateRandomWord(int wordLength) throws RemoteException;

}
