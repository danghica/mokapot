package xyz.acygn.mokapot.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * A wrapper around Socket to make it conform to the SocketLike interface.
 *
 * @author Alex Smith
 */
public class SocketWrapper implements SocketLike {

    /**
     * The socket that's being wrapped.
     */
    private final Socket socket;

    /**
     * A lazily-initialized copy of the socket's input stream.
     */
    private InputStream inputStream = null;

    /**
     * A lazily-initialized copy of the socket's output stream.
     */
    private OutputStream outputStream = null;

    /**
     * Returns the actual Socket object that's being wrapped.
     *
     * @return The socket.
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Creates a socket wrapper to wrap the given socket. Closing the wrapper
     * will close the socket.
     *
     * @param socket The socket to wrap.
     */
    public SocketWrapper(Socket socket) {
        this.socket = socket;
    }

    /**
     * Returns the IP address of the other end of the socket.
     *
     * @return An IP address.
     */
    @Override
    public InetAddress getAddressDescription() {
        return socket.getInetAddress();
    }

    @Override
    public synchronized InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            inputStream = socket.getInputStream();
        }
        return inputStream;
    }

    @Override
    public synchronized OutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = socket.getOutputStream();
        }
        return outputStream;
    }

    /**
     * Closes the socket.
     *
     * @throws IOException If something goes wrong closing the socket.
     */
    @Override
    public void close() throws IOException {
        socket.close();
    }
}
