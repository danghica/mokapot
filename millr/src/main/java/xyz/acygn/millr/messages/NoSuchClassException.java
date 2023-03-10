package xyz.acygn.millr.messages;

/**
 * A general exception for when a class by some given name should be present
 * in a collection, but isn't.
 *
 * @author Marcello De Bernardi, Thomas Cuvillier
 */
public class NoSuchClassException extends RuntimeException {
    public NoSuchClassException(String message) {
        super(message);
    }
    
    public NoSuchClassException(Exception ex, String message){
        super(message, ex);
    }
}
