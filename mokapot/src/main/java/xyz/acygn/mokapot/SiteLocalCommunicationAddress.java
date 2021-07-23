package xyz.acygn.mokapot;

import java.net.InetAddress;

/**
 * A TCPCommunicationAddress designed for situations in which the communication
 * takes place over a site-local network, using site-local addresses.
 * <p>
 * The IP address given will be checked to see that it's in the site-local
 * range.
 *
 * @author Alex Smith
 */
class SiteLocalCommunicationAddress extends TCPCommunicationAddress {

    /**
     * Unique identifier for this class in serialization. This was originally
     * created as a random number, and should be set to a new random number
     * whenever the class is changed in an incompatible way.
     */
    private static final long serialVersionUID = 0xa80528299ba1c34aL;

    /**
     * The publicly visible Internet address used to communicate with the
     * machine at this communication address.
     */
    private final InetAddress inetAddress;
    /**
     * The TCP port on which to carry out communications via this address.
     */
    private final int port;

    /**
     * Creates a TCP communication address from a site-local IP address and a
     * port.
     *
     * @param uid The unique identifier to use for the address.
     * @param inetAddress A site-local IP address which is used to communicate
     * with the VM described by this communication address.
     * @param port The port on which to communicate.
     */
    SiteLocalCommunicationAddress(Number uid,
            InetAddress inetAddress, int port) {
        super(uid);
        if (!inetAddress.isSiteLocalAddress()) {
            throw new IllegalArgumentException("IP address "
                    + inetAddress + " is not a site local address");
        }
        this.inetAddress = inetAddress;
        this.port = port;
    }

    /**
     * The publicly visible Internet address at which connections via this
     * address are made.
     *
     * @return The address specified when this CommunicationAddress was
     * constructed.
     */
    @Override
    public InetAddress asInetAddress() {
        return inetAddress;
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
