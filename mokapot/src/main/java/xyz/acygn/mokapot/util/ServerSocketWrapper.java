package xyz.acygn.mokapot.util;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * A wrapper around <code>ServerSocket</code> so that it implements the
 * <code>ServerSocketLike</code> interface.
 *
 * @author Alex Smith
 */
public class ServerSocketWrapper implements ServerSocketLike {

    /**
     * The server socket being wrapped by this wrapper.
     */
    private final ServerSocket wrappedSocket;

    /**
     * Creates a new <code>ServerSocketWrapper</code> wrapping a given server
     * socket.
     *
     * @param wrappedSocket The socket to wrap.
     */
    public ServerSocketWrapper(ServerSocket wrappedSocket) {
        this.wrappedSocket = wrappedSocket;
    }

    @Override
    public SocketLike accept() throws IOException {
        return new SocketWrapper(wrappedSocket.accept());
    }

    /**
     * Closes the wrapped server socket.
     *
     * @throws IOException If something goes wrong closing the server socket.
     */
    @Override
    public void close() throws IOException {
        wrappedSocket.close();
    }

}
