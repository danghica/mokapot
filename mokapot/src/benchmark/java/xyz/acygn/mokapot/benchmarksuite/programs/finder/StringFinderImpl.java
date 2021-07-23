package xyz.acygn.mokapot.benchmarksuite.programs.finder;

import java.util.List;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * A simple example class which contains the functionality required to search a
 * word in a given list. The list can be either read from the user or generated
 * randomly.
 * 
 * @author Alexandra Paduraru
 *
 */
public class StringFinderImpl implements NonCopiable, StringFinder {

    @Override
    public boolean searchWord(List<String> words, String w) {
        for (String s : words)
            if (s.equals(w)) {
                return true;
            }
        return false;
    }

}
