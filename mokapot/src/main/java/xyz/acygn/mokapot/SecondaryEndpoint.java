package xyz.acygn.mokapot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import static xyz.acygn.mokapot.DistributedCommunicator.getMainCommunicator;
import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;
import xyz.acygn.mokapot.util.BlockingQueueInputStream;
import xyz.acygn.mokapot.util.BlockingQueueOutputStream;
import xyz.acygn.mokapot.util.DeterministicAutocloseable;
import xyz.acygn.mokapot.util.Pair;
import xyz.acygn.mokapot.util.ServerSocketLike;
import xyz.acygn.mokapot.util.SocketLike;
import xyz.acygn.mokapot.util.StreamSocketLike;
import xyz.acygn.mokapot.util.WeakValuedConcurrentMap;

/**
 * A communication endpoint that allows a secondary communicator to communicate
 * with a primary communicator on the same JVM. Communication is done via a
 * stream pair that's pumped synchronously (rather than using a separate
 * thread).
 *
 * @author Alex Smith
 */
class SecondaryEndpoint implements CommunicationEndpoint {

    /**
     * The main communicator corresponding to the secondary communicator which
     * uses this endpoint.
     */
    private final DistributedCommunicator primary;

    /**
     * The keepalive lock that prevents the primary communicator being stopped
     * while the secondary communicator is running.
     */
    private DeterministicAutocloseable primaryKeepalive;

    /**
     * The distributed communicator, if any, that is currently using this as its
     * endpoint.
     */
    private DistributedCommunicator secondary = null;

    /**
     * The unique name of this endpoint. Used to distinguish between secondary
     * endpoints, which are otherwise effectively identical.
     */
    private final String name;

    /**
     * An index of secondary endpoints by (primary, name) pair. This makes it
     * possible to send them over the "network" (actually just local simulated
     * streams).
     */
    private final static WeakValuedConcurrentMap<Pair<
                CommunicationAddress, String>, SecondaryEndpoint> BY_NAME
            = new WeakValuedConcurrentMap<>();

    /**
     * Creates a secondary endpoint for a given main communicator, with a given
     * name.
     *
     * @param primary The main communicator for which the communicator with this
     * endpoint is a secondary communicator.
     * @param name The unique name of the endpoint.
     */
    SecondaryEndpoint(DistributedCommunicator primary, String name) {
        this.primary = primary;
        this.name = name;
        /* TODO: The warning is correct here; we'd need some sort of wrapper to
           make this 100% safe. */
        BY_NAME.put(new Pair<>(primary.getMyAddress(), name), this);
    }

    @Override
    public CommunicationAddress getAddress() {
        return new Address(name, primary.getMyAddress());
    }

    /**
     * Creates a socket for communicating between this endpoint and another on
     * the same JVM, and injects a receive connection into that JVM to connect
     * the two.
     *
     * @param remoteAddress The communication address of the other endpoint.
     * @return A socket-like connection between this endpoint and that one.
     * @throws IOException If there is no communicator corresponding to the
     * given endpoint
     * @throws UnsupportedOperationException If the given endpoint is on a
     * different JVM
     */
    @Override
    public SocketLike newConnection(Communicable remoteAddress)
            throws IOException, IncompatibleEndpointException {
        return staticNewConnection(getAddress(), remoteAddress);
    }

    /**
     * Implementation of <code>newConnection</code> and
     * <code>connectHere</code>. This is static, i.e. it doesn't care about what
     * the individual endpoints are.
     *
     * @param localAddress The address which initiated the connection. Must be
     * on this JVM.
     * @param remoteAddress The address which the connection is being made to.
     * Must also be on this JVM.
     * @return A socket that the communicator with address
     * <code>localAddress</code> can use to build a send connection.
     * @throws IOException If something goes wrong building the receive half of
     * the connection
     * @throws IncompatibleEndpointException If <code>remoteAddress</code> does
     * not appear to refer to a communicator on this JVM
     */
    private static SocketLike staticNewConnection(
            CommunicationAddress localAddress, Communicable remoteAddress)
            throws IOException, IncompatibleEndpointException {
        /* Find the communicator matching the address. */
        DistributedCommunicator remoteCommunicator;
        if (getMainCommunicator().getMyAddress().equals(remoteAddress)) {
            remoteCommunicator = getMainCommunicator();
        } else if (remoteAddress instanceof SecondaryEndpoint.Address) {
            remoteCommunicator = ((Address) remoteAddress).getEndpoint().secondary;
            if (remoteCommunicator == null) {
                throw new SocketException(
                        "Cannot connect to " + remoteAddress
                        + " because no communicator is running on it");
            }
        } else {
            throw new IncompatibleEndpointException(
                    "Cannot connect to " + remoteAddress
                    + " because it does not appear to be on the local JVM");
        }

        /* Create a pair of connected socket-likes for relaying messages. */
        BlockingQueue<Byte> bq1 = new LinkedBlockingQueue<>();
        OutputStream os1 = new BlockingQueueOutputStream(bq1);
        InputStream is1 = new BlockingQueueInputStream(bq1);
        BlockingQueue<Byte> bq2 = new LinkedBlockingQueue<>();
        OutputStream os2 = new BlockingQueueOutputStream(bq2);
        InputStream is2 = new BlockingQueueInputStream(bq2);
        SocketLike receiverEndSocket
                = new StreamSocketLike(is2, os1, localAddress);

        /* Create a receive connection on that communicator. */
        try {
            GlobalID id = new GlobalID(remoteCommunicator.getMyAddress());
            Connection remoteConnection = new Connection(
                    receiverEndSocket, id, remoteCommunicator, localAddress,
                    false);
            remoteCommunicator.getAcceptor().registerConnection(remoteConnection);
            SocketLike senderEndSocket
                    = new PairedStreamSocketLike(
                            is1, os2, remoteAddress, remoteConnection);

            return senderEndSocket;
        } catch (AutocloseableLockWrapper.CannotLockException ex) {
            throw new ConnectException("Cannot connect to the remote secondary "
                    + "communicator because it is shutting down");
        }
    }

    /**
     * An unsupported method. Secondary endpoints have their connections
     * injected into the primary communicator's connection map directly, and
     * thus there's never a need to listen.
     *
     * @return Never returns.
     * @throws UnsupportedOperationException Always
     */
    @Override
    public ServerSocketLike newListenSocket() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Does nothing. Secondary endpoints are entirely local and thus have no
     * real security implications, thus there's no need to verify the
     * certificate of the socket.
     *
     * @param socket Ignored.
     */
    @Override
    public void initialVerifySocket(SocketLike socket) {
        /* nothing to do */
    }

    /**
     * Records the communicator that is using this as its endpoint.
     *
     * @param communicator The communicator that started.
     * @throws IllegalStateException If another communicator is already using
     * this as its endpoint.
     */
    @Override
    public synchronized void informOfCommunicatorStart(DistributedCommunicator communicator)
            throws IllegalStateException {
        if (secondary != null) {
            throw new IllegalStateException(
                    "starting two communicators with the same endpoint");
        }
        /* note: synchronized in case we have a stop followed shortly by a start
           from another thread, which is dubious but not illegal */
        secondary = communicator;
        try {
            primaryKeepalive = primary.maybeGetKeepaliveLock(
                    ShutdownStage.SECONDARY_COMMUNICATOR, this);
        } catch (AutocloseableLockWrapper.CannotLockException ex) {
            throw new IllegalStateException(
                    "starting a secondary communicator during "
                    + "main communicator shutdown", ex);
        }
    }

    /**
     * Records that no communicator is currently using this as its endpoint.
     *
     * @param communicator The communicator that stopped; currently ignored, as
     * we know what communicator is involved already.
     */
    @Override
    public synchronized void informOfCommunicatorStop(
            DistributedCommunicator communicator) {
        secondary = null;
        primaryKeepalive.close();
        primaryKeepalive = null;
    }

    /**
     * Outputs a string representation of the endpoint. This lists the name of
     * the endpoint, and the corresponding primary communicator.
     *
     * @return A string describing the endpoint.
     */
    @Override
    public String toString() {
        return "secondary endpoint '" + name + "' of " + primary;
    }

    /**
     * A <code>StreamSocketLike</code> that's linked to a particular
     * <code>Connection</code>. When a new <code>Connection</code> is generated
     * from such a <code>SocketLike</code>, it'll automatically be partnered
     * with the pre-existing <code>Connection</code>.
     */
    static class PairedStreamSocketLike extends StreamSocketLike {

        /**
         * The <code>Connection</code> that this socket-like is paired with.
         */
        private final Connection linkedConnection;

        /**
         * Returns the connection that this socket-like is linked to.
         *
         * @return The linked connection.
         */
        Connection getLinkedConnection() {
            return linkedConnection;
        }

        /**
         * Constructs a paired stream-based socket-like from its components.
         *
         * @param inputStream The input stream that acts as the "input from the
         * socket".
         * @param outputStream The output stream that acts as the "output from
         * the socket".
         * @param addressDescription An object that can be stringified to act as
         * a description of the socket's address.
         * @param linkedConnection The <code>Connection</code> object that
         * serves as the other side of the connection.
         */
        PairedStreamSocketLike(InputStream inputStream,
                OutputStream outputStream, Object addressDescription,
                Connection linkedConnection) {
            super(inputStream, outputStream, addressDescription);
            this.linkedConnection = linkedConnection;
        }
    }

    /**
     * The <code>CommunicationAddress</code> used by other communicators to
     * refer to this endpoint.
     */
    static class Address extends CommunicationAddress {

        /**
         * Unique identifier for this class in serialization. This was
         * originally created as a random number, and should be set to a new
         * random number whenever the class is changed in an incompatible way.
         */
        private static final long serialVersionUID = 0xd04a3052f9abaf7L;

        /**
         * The name of the endpoint.
         */
        private final String name;

        /**
         * Used to ensure that we have the correct primary communicator
         * corresponding to the secondary communicator. (It's not normally
         * possible for this to be wrong, as you can't have two primary
         * communicators at the same time, but may be important if a
         * communication address gets sent over a network or serialised to
         * disk.)
         */
        private final CommunicationAddress primaryAddress;

        /**
         * Creates the address that corresponds to the endpoint. All the
         * information inside the address is determined via reference to the
         * outer class.
         *
         * @param name The name of the endpoint.
         * @param primaryAddress The communication address of the primary
         * endpoint for which this address describes the secondary address.
         */
        Address(String name, CommunicationAddress primaryAddress) {
            super(uidFromString(name));
            this.name = name;
            this.primaryAddress = primaryAddress;
        }

        /**
         * Produces a textual representation of this communication address. This
         * is formed from that of the primary endpoint's address via adding a
         * prefix containing the secondary endpoint's name.
         *
         * @return A string describing the communication address.
         */
        @Override
        public String toString() {
            return name + "@" + primaryAddress;
        }

        /**
         * Gets the SecondaryEndpoint instance that created this address.
         *
         * @return The secondary endpoint.
         */
        SecondaryEndpoint getEndpoint() {
            SecondaryEndpoint rv = BY_NAME.get(new Pair<>(primaryAddress, name));
            if (rv == null) {
                throw new DistributedError(new IllegalStateException(),
                        "secondary endpoint " + name
                        + " on communicator " + primaryAddress
                        + " is not running, but its address was used");
            }
            return rv;
        }

        @Override
        public SocketLike connectHere(CommunicationAddress connectingFrom)
                throws IncompatibleEndpointException, IOException {
            return staticNewConnection(connectingFrom, this);
        }
    }

    /**
     * Converts a string into a number, such that different strings map to
     * different numbers. Used to create serial numbers for secondary endpoints.
     * This function is deterministic, i.e. the same string will always produce
     * the same number.
     *
     * @param s The string to encode.
     * @return A number, such that different strings map to different numbers.
     */
    private static Number uidFromString(String s) {
        /* Prepend a character that's nonzero and has the high bit clear; this
           prevents non-uniqueness due to leading zeroes, and also prevents
           negative numbers being created */
        byte[] bytes = ("\u0001" + s).getBytes(Charset.forName("UTF-8"));
        return new BigInteger(bytes);
    }
}
