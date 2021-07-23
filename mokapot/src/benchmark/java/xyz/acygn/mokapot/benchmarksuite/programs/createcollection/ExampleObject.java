package xyz.acygn.mokapot.benchmarksuite.programs.createcollection;

import java.io.Serializable;
import xyz.acygn.mokapot.markers.Copiable;

/**
 * An simple class for objects to be handled by mokapot over the network.
 *
 * @author Marcello De Bernardi
 */
public class ExampleObject implements Serializable, Copiable {

    private final String name;
    private final int value;

    public ExampleObject(String name, int value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return name + ": " + value;
    }

}
