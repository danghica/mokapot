package xyz.acygn.millr.messages;

/**
 * Signals that millr has no classes to mill, either because the user provided
 * no input files, or because all of the inputs provided by the user were rejected.
 *
 * @author Marcello De Bernardi
 */
public class NoClassesToMillException extends Exception {
    public NoClassesToMillException(String message) {
        super(message);
    }
}
