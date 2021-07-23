package xyz.acygn.mokapot;

import java.net.InetAddress;

/**
 * Something that we can communicate with via TCP. This includes communication
 * addresses for endpoints that use TCP, and port/hostname pairs (which allow us
 * to contact a system whose address we don't know).
 *
 * @author Alex Smith
 */
interface TCPCommunicable extends Communicable {

    /**
     * Returns an Internet address via which the machine at this communication
     * address can be contacted. Unlike a CommunicationAddress, where a machine
     * must always be referred to by a constant address no matter which machine
     * is doing the referring, the return value of this method may depend on the
     * machine on which it is called. (Or to think of it another way: within a
     * particular distributed cluster, there's a unique, 1-to-1 relationship
     * between a virtual machine and its communication address, but
     * <code>asInetAddress</code> returns information for <i>this VM</i> to
     * contact the VM at the other end of the connection, which does not
     * necessarily have to work for other connections.)
     * <p>
     * If this method does not return a locally deterministic value (i.e. the
     * same value every time it is called from a single specific machine), you
     * must override Object#equals() and Object#hashCode() in order to ensure
     * that your communication addresses compare correctly.
     *
     * @return An Internet address that can be used to contact the machine at
     * this communication address.
     * @throws UnsupportedOperationException If this address represents a
     * machine that does not accept inbound communication
     */
    InetAddress asInetAddress() throws UnsupportedOperationException;

    /**
     * Returns the port number to use when sending a message to the virtual
     * machine at this address. As with <code>asInetAddress()</code>, this can
     * in theory depend on the machine on which the method is called (although
     * in practice, this is unlikely).
     * <p>
     * If this method does not return a locally deterministic value (i.e. the
     * same value every time it is called from a single specific machine), you
     * must override Object#equals() and Object#hashCode() in order to ensure
     * that your communication addresses compare correctly.
     *
     * @see #asInetAddress()
     * @return A TCP port number that can be used together with the return value
     * of <code>asInetAddress()</code> to contact the virtual machine at this
     * communication address over TCP.
     * @throws UnsupportedOperationException If this address represents a
     * machine that does not accept inbound communication
     */
    int getTransmissionPort() throws UnsupportedOperationException;
}
