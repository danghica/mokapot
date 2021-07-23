package xyz.acygn.mokapot;

import java.net.InetAddress;

/**
 * A TCPCommunicationAddress designed for situations in which the communication
 * takes place over the public Internet, and machines are contacted via
 * connecting to a predetermined public IP. This only works when the machines
 * involved have IPs that are static, at least for the duration of the
 * distributed computation.
 * <p>
 * The IP address given will be checked to ensure that it is likely to be
 * publicly visible (i.e. unicast, and not in the site-local or link-local
 * range).
 *
 * @author Alex Smith
 */
class StaticInternetCommunicationAddress extends TCPCommunicationAddress {

    /**
     * Unique identifier for this class in serialization. This was originally
     * created as a random number, and should be set to a new random number
     * whenever the class is changed in an incompatible way.
     */
    private static final long serialVersionUID = 0x91b2ed5d1dbb726eL;

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
     * Creates a communication address for communicating with a VM via the given
     * IP address and port.
     *
     * @param uid The unique identifier to use for the address.
     * @param inetAddress An IP address on the public Internet.
     * @param port The port via which to communicate.
     */
    StaticInternetCommunicationAddress(Number uid,
            InetAddress inetAddress, int port) {
        super(uid);
        if (inetAddress.isLinkLocalAddress()
                || inetAddress.isLoopbackAddress()
                || inetAddress.isSiteLocalAddress()
                || inetAddress.isMulticastAddress()) {
            throw new IllegalArgumentException(
                    "IP address " + inetAddress
                    + " is not a public Internet unicast address");
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
