package xyz.acygn.mokapot;

import java.net.InetAddress;

/**
 * A TCPCommunicationAddress designed for running a "distributed" computation
 * entirely on a single machine, using TCP connections on the loopback
 * interface. Probably most useful for testing.
 *
 * @author Alex Smith
 */
class LoopbackCommunicationAddress extends TCPCommunicationAddress {

    /**
     * Unique identifier for this class in serialization. This was originally
     * created as a random number, and should be set to a new random number
     * whenever the class is changed in an incompatible way.
     */
    private static final long serialVersionUID = 0xa6398e63754165fcL;

    /**
     * The port used for connections to this address.
     */
    private final int port;

    /**
     * Creates a communication address for TCP connections, in a "distributed"
     * computation that takes place entirely on one machine. Note that it's
     * still possible to have multiple virtual machines participate in the
     * connection, so long as they're running on the same machine; in this case,
     * each virtual machine must use a different port number, in order to be
     * able to distinguish between them.
     *
     * @param uid The unique identifier to use for the address.
     * @param port The port via which connections to the virtual machine
     * specified by this connection are made.
     */
    LoopbackCommunicationAddress(Number uid, int port) {
        super(uid);
        this.port = port;
    }

    /**
     * Returns the TCP address used to communicate with this communication
     * address; that's always the loopback address for the current system.
     *
     * @return The current system's loopback address.
     *
     * @see InetAddress#getLoopbackAddress()
     */
    @Override
    public InetAddress asInetAddress() {
        return InetAddress.getLoopbackAddress();
    }

    /**
     * The TCP port via which connections via this address are made.
     *
     * @return The port specified when this CommunicationAddress was
     * constructed.
     */
    @Override
    public int getTransmissionPort() {
        return port;
    }

}
