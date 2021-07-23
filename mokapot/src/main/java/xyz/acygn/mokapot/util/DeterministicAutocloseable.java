package xyz.acygn.mokapot.util;

/**
 * Wrapper around AutoCloseable with tighter guarantees on exceptions. Makes it
 * possible to hide the details of how autoclosing works (thus avoiding the need
 * to create a public class to expose the autoclose rules), whilst also avoiding
 * the need for a spurious <code>catch</code> block.
 *
 * @author Alex Smith
 */
@FunctionalInterface
public interface DeterministicAutocloseable extends AutoCloseable {

    /**
     * Release the resource represented by this <code>AutoCloseable</code>.
     * Unlike in <code>AutoCloseable</code>, this is guaranteed not to throw a
     * checked exception.
     *
     * This method should only be called once; there is no expectation that it
     * is idempotent.
     */
    @Override
    void close();
}
