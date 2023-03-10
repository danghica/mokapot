package xyz.acygn.millr.messages;

/**
 * Signals that a class name handled by millr has no associated path to a file.
 *
 * @author Marcello De Bernardi
 */
public class ClassNoAssociatedPathException extends Exception {
    public ClassNoAssociatedPathException(String message) {
        super(message);
    }
}
