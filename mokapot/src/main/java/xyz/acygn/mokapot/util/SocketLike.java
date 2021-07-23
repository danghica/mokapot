package xyz.acygn.mokapot.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An interface describing classes that support many of the same operations as
 * Sockets. This allows real sockets, and fake/testing sockets, to be treated in
 * a similar way.
 *
 * @author Alex Smith
 */
public interface SocketLike extends Closeable, AutoCloseable {

    /**
     * Returns an object describing what the other end of the socket is
     * connected to. The form of the object can vary, but can always be
     * converted to a human-readable string via <code>.toString()</code>.
     *
     * @return A description of the far end of the connection.
     */
    public Object getAddressDescription();

    /**
     * Returns an input stream via which the socket can be read from.
     * <p>
     * Calling this method multiple times should return the same stream each
     * time.
     *
     * @return An input stream.
     * @throws IOException If the stream could not be created.
     */
    public InputStream getInputStream() throws IOException;

    /**
     * Returns an output stream via which the socket can be written to.
     * <p>
     * Calling this method multiple times should return the same stream each
     * time.
     *
     * @return An output stream.
     * @throws IOException If the stream could not be created.
     */
    public OutputStream getOutputStream() throws IOException;
}
