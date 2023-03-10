package xyz.acygn.millr.messages;

/**
 * An exception signalling that a {@link ClassLoader} has failed to load
 * a class. The original exception throw is stored in this exception.
 *
 * @author Marcello De Bernardi
 */
public class ClassNotLoadedException extends Exception {
    public ClassNotLoadedException(Throwable cause, String nameOfTheClass) {
        super(cause);
    }
    
}
