package xyz.acygn.millr.messages;

/**
 * Signals that millr has detected multiple class files with the same name.
 *
 * @author Marcello De Bernardi
 */
public class DuplicateClassNameException extends Exception {
    public DuplicateClassNameException(String message) {
        super(message);
    }
}
