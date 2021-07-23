package xyz.acygn.mokapot;

import java.net.InetAddress;

/**
 * An implementation of a communication address that assumes that each system
 * involved in the distributed communication has an IP address via which all
 * other machines can contact them on a specified port. Because this assumption
 * is not true in general, even for machines that are networked together or
 * Internet-connected, this class is abstract, and subclasses are defined for
 * certain special cases where the assumption works correctly:
 * <ul><li><code>LoopbackCommunicationAddress</code> - Handles the situation
 * where the distributed communication runs entirely on a single physical
 * machine.</li>
 * <li><code>SiteLocalCommunicationAddress</code> - Handles the situation where
 * all the machines involved in the distributed are connected together via a
 * site-local network.</li>
 * <li><code>StaticInternetCommunicationAddress</code> - Handles the situation
 * where all the machines involved are connected to the Internet, and have a
 * static, externally visible IP address (i.e. inbound connections will be
 * routed to the machine, rather than being interrupted by a firewall or
 * NAT).</li></ul>
 *
 * @see LoopbackCommunicationAddress
 * @see SiteLocalCommunicationAddress
 * @see StaticInternetCommunicationAddress
 *
 * @author Alex Smith
 */
abstract class TCPCommunicationAddress extends CommunicationAddress
        implements TCPCommunicable {

    /**
     * Unique identifier for this class in serialization. This was originally
     * created as a random number, and should be set to a new random number
     * whenever the class is changed in an incompatible way.
     */
    private static final long serialVersionUID = 0x1835e09b0b2eb7d4L;

    /**
     * Creates a new TCP communication address.
     *
     * @param uid The unique ID of the communication address.
     */
    protected TCPCommunicationAddress(Number uid) {
        super(uid);
    }

    /**
     * Creates a TCPCommunicationAddress of an appropriate subclass, for
     * communicating with a machine at the given IP address and port. Note that
     * this assumes that all machines involved in the computation will
     * understand the given IP address <i>as written</i>; for example, if the
     * address is a loopback address, then only one machine can be involved in
     * the computation (as other machines would give a different interpretation
     * to the loopback address, treating it as referring to a different
     * machine). If the IP address to use for a connection varies from one
     * machine to another, you cannot use this factory method, and will need to
     * subclass <code>TCPCommunicationAddress</code> yourself.
     *
     * @param uid The unique identifier to use for the address.
     * @param address The IP address via which communication is made to the
     * machine you wish the return value to represent.
     * @param port The port via which communication is made to the VM you wish
     * the return value to represent.
     * @return A communication address that represents TCP communication via a
     * constant IP address and port.
     */
    static TCPCommunicationAddress fromInetAddress(
            Number uid, InetAddress address, int port) {
        if (address.isLoopbackAddress()) {
            return new LoopbackCommunicationAddress(uid, port);
        } else if (address.isSiteLocalAddress()) {
            return new SiteLocalCommunicationAddress(uid, address, port);
        } else {
            return new StaticInternetCommunicationAddress(uid, address, port);
        }
    }

    /**
     * Produces a string representation of this communication address.
     *
     * @return The stringification of the IP address, a colon, and the TCP port
     * in use.
     */
    @Override
    public String toString() {
        return asInetAddress() + ":" + getTransmissionPort();
    }
}
