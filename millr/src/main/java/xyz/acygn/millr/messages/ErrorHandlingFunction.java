package xyz.acygn.millr.messages;

/**
 * A functional interface for error handling code to be passed to
 * an error handler.
 *
 * @author Marcello De Bernardi
 */
@FunctionalInterface
interface ErrorHandlingFunction<E extends Throwable> {
    void apply(E exception);
}