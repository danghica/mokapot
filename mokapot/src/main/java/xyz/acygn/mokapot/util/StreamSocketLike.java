package xyz.acygn.mokapot.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A SocketLike built field-by-field. This wraps a pair of streams and a
 * description of an address.
 *
 * @author Alex Smith
 */
public class StreamSocketLike implements SocketLike {

    /**
     * The input stream to use.
     */
    private final InputStream inputStream;
    /**
     * The output stream to use.
     */
    private final OutputStream outputStream;
    /**
     * The address description to use.
     */
    private final Object addressDescription;

    /**
     * Creates a SocketLike from individual fields.
     *
     * @param inputStream The input stream that simulates reading from the
     * simulated socket.
     * @param outputStream The output stream that simulates writing to the
     * simulated socket.
     * @param addressDescription The object to return if a request is made for
     * the socket's IP address (or an equivalent). Might not be an
     * <code>InetAddress</code> (the caller's only assumption is that
     * <code>toString</code> will produce something meaningful).
     */
    public StreamSocketLike(InputStream inputStream, OutputStream outputStream,
            Object addressDescription) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.addressDescription = addressDescription;
    }

    @Override
    public Object getAddressDescription() {
        return addressDescription;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Closes the input and output streams.
     *
     * @throws IOException If there is an exception closing a stream.
     */
    @Override
    public void close() throws IOException {
        try {
            inputStream.close();
        } finally {
            outputStream.close();
        }
    }
}
