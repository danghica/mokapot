package xyz.acygn.mokapot;

import java.io.IOException;
import xyz.acygn.mokapot.util.ServerSocketLike;
import xyz.acygn.mokapot.util.SocketLike;

/**
 * An communication address together with authentication material and an
 * authorisation policy. The distinction between an address and an endpoint is
 * that a communication address is intended to just be plain data that can be
 * sent over a network literally, whereas an endpoint is not intended to be sent
 * from machine to machine and thus can contain machine-specific data (such as
 * private keys), can be much more heavy-weight, etc..
 * <p>
 * An endpoint is capable both of authenticating itself to remote systems (thus
 * guaranteeing to those systems that a particular machine is at the other end
 * of the connection from them), and of determining which inbound connections
 * are authorised (i.e. we can authenticate them to determine who made them, and
 * know which machines/people are authorised to make the connection).
 * <p>
 * Typically speaking, any given virtual machine would use only one endpoint at
 * a time, so that it only had one identity as seen by other machines. (This is
 * because each communicator can only use one endpoint, and only one
 * communicator can be used at a time.)
 *
 * @author Alex Smith
 */
public interface CommunicationEndpoint {

    /**
     * Returns the communication address that other machines can use to identify
     * this endpoint. If a listen socket is created on this machine using this
     * endpoint, and a send socket is created on another machine using the
     * returned address, the send socket should connect to the listen socket.
     * Additionally, if a send socket is created using this endpoint, remote
     * machines should be able to authenticate that connections made to them via
     * that send socket were indeed created by the virtual machine using the
     * corresponding address.
     *
     * @return The communication address corresponding to this endpoint.
     */
    public CommunicationAddress getAddress();

    /**
     * Opens a new connection to the virtual machine at a given communication
     * address, and returns a socket via which information can be sent to it.
     *
     * @param remoteAddress An identifier for the machine to connect to; this
     * will normally be its communication address, but in cases where the
     * address is unknown, will be some other description of how to connect.
     * @return An open, connected socket, such that information written to the
     * socket will be sent to the machine at the given communication address.
     * @throws IOException If something goes wrong trying to establish
     * communications
     * @throws IncompatibleEndpointException If this type of endpoint does not
     * know how to (or cannot) connect to the given sort of
     * <code>Communicable</code>.
     */
    public SocketLike newConnection(Communicable remoteAddress)
            throws IOException, IncompatibleEndpointException;

    /**
     * Creates a new ServerSocket that allows this machine to listen for
     * connections from other machines that are sent via this address.
     * Obviously, this should only be called if this communication address
     * refers to the local virtual machine.
     * <p>
     * This method does not have to be supported, typically because the endpoint
     * does not support connections over a network. In cases where it <i>is</i>
     * supported, <code>getAddress().monitorsArrivingMessages()</code> must be
     * <code>true</code> (otherwise, if a connection were created over the
     * network, the communicator would not be monitoring for incoming messages
     * via that connection, which is useless).
     *
     * @return A server socket that will be capable of receiving communications
     * that are sent via this address. The socket is initially bound, but not
     * listening.
     * @throws java.io.IOException If something goes wrong trying to create the
     * socket
     * @throws UnsupportedOperationException If this endpoint does not support
     * inbound connections
     */
    public ServerSocketLike newListenSocket() throws IOException,
            UnsupportedOperationException;

    /**
     * Ensures that the given socket is correctly authenticated and authorised
     * for communication via this endpoint. Endpoints are expected to put checks
     * on the sockets to prevent unauthorised connections even in the absence of
     * calling this method. However, this method allows for an explicit
     * double-check to guard against implementation mistakes in the checks in
     * question that might cause them not to run (e.g. forgetting to turn on
     * authentication). It also allows the checks to run at a predictable time
     * (rather than "when the first byte of data is sent").
     * <p>
     * This method will be called externally only immediately after an inbound
     * connection has been accepted. Endpoints should consider calling it on
     * themselves immediately after making an outbound connection.
     *
     * @param socket The socket to check.
     * @throws IOException If the socket is incorrectly authenticated or
     * incorrectly authorised, or the other end could not be communicated with
     * to verify this
     */
    public void initialVerifySocket(SocketLike socket) throws IOException;

    /**
     * A method that is called whenever a distributed communicator is started
     * using this endpoint. This enables endpoints to track their associated
     * communicator, if they need it for some reason.
     *
     * @param communicator The communicator that started.
     */
    public void informOfCommunicatorStart(DistributedCommunicator communicator);

    /**
     * A method that is called whenever a distributed communicator is stopped
     * while using this as its endpoint. This enables endpoint objects to track
     * their associated communicator, if they need it for some reason.
     *
     * @param communicator The communicator that stopped.
     */
    public void informOfCommunicatorStop(DistributedCommunicator communicator);

    /**
     * An exception thrown when an attempt is made to make a connection between
     * two incompatible endpoints.
     */
    public class IncompatibleEndpointException extends Exception {

        /**
         * A unique identifier for this class. This is used to ensure that the
         * class serialised correctly, and should be changed to a new random
         * value whenever the class is changed in an incompatible way.
         */
        private static final long serialVersionUID = 0xdba0c4fb5a90b78L;

        /**
         * Creates a new incompatible-endpoint exception with the specified
         * detail message.
         *
         * @param message The reason the endpoints are incompatible.
         */
        public IncompatibleEndpointException(String message) {
            super(message);
        }
    }
}
