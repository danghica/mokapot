package xyz.acygn.mokapot.rmiserver;

import java.io.Serializable;

/**
 * @author Marcello De Bernardi.
 */
@FunctionalInterface
public interface Executable<T> extends Serializable {
    T execute(int port);
}
