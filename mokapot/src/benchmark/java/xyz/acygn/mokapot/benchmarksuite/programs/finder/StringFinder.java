package xyz.acygn.mokapot.benchmarksuite.programs.finder;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * @author Kelsey McKenna
 */
public interface StringFinder extends Remote {

    /**
     * Check to see if a given word is in a List.
     *
     * @param words The list of words on which the search is carried out.
     * @param w     The word that has to be searched in the list.
     * @return Whether or not the word is in the ArrayList.
     */
    boolean searchWord(List<String> words, String w) throws RemoteException;

}
