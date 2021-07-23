package xyz.acygn.mokapot.util;

import java.io.Closeable;
import java.io.IOException;

/**
 * Something that acts like a <code>ServerSocket</code>. The basic operations
 * that it must support are listening for new connections, and producing a
 * <code>SocketLike</code> when they arrive.
 *
 * @author Alex Smith
 */
public interface ServerSocketLike extends Closeable, AutoCloseable {

    /**
     * Accepts one connection, returning a <code>SocketLike</code> for it.
     * "Accepting" a connection means listening for some external entity to
     * contact this server socket, and setting up a persistent two-way
     * connection with it.
     *
     * @return A <code>SocketLike</code> which allows communication with the
     * entity that contacted the server socket.
     * @throws IOException If something goes wrong while listening.
     */
    SocketLike accept() throws IOException;
}
