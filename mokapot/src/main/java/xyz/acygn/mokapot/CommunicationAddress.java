package xyz.acygn.mokapot;

import java.io.IOException;
import java.io.Serializable;
import xyz.acygn.mokapot.markers.Copiable;
import xyz.acygn.mokapot.util.ObjectUtils;
import xyz.acygn.mokapot.util.SocketLike;

/**
 * Information allowing a particular VM involved in a distributed computation to
 * be identified and contacted.
 * <p>
 * This is very similar in most ways to a combination of a socket address and
 * unique identifier, but has the added requirement of transitivity; if virtual
 * machine A can connect to virtual machine B via CommunicationAddress
 * <code>addressB</code>, and A can connect to C via CommunicationAddress
 * <code>addressC</code>, then B must also be able to connect to C via
 * CommunicationAddress <code>addressC</code>.
 * <p>
 * In order to ensure that communication addresses are correctly unique and that
 * they identify a particular machine (i.e. contain information that can be used
 * to verify that a particular machine is being contacted), it's not usual to
 * construct one of these directly (so although the class itself is
 * <code>public</code>, none of its predefined subclasses are). Rather, a
 * <code>CommunicationEndpoint</code> (which is responsible for authenticating
 * and authorising machines) will use its own addressing scheme in order to
 * ensure that it can reliably identify the machines involved in a computation.
 * This means that a machine's address should, in practice, be originally
 * received by asking that machine's endpoint for it.
 * <p>
 * In order to get at your own address, you can ask either your own endpoint (if
 * you constructed it manually), or your own communicator. In order to get at a
 * remote machine's address, you can ask your own communicator to connect to it
 * via IP/portname pair and ask it what its address is. It's also safe to store
 * communication addresses on disk and to reuse them later, if they happen to be
 * <code>Serialisable</code>; they should continue to uniquely identify a single
 * virtual machine even if it (or the computation as a whole) gets restarted.
 *
 * @author Alex Smith
 * @see DistributedCommunicator#getMyAddress()
 * @see DistributedCommunicator#lookupAddress(java.net.InetAddress, int)
 * @see CommunicationEndpoint#getAddress()
 */
public abstract class CommunicationAddress
        implements Copiable, Communicable, Serializable {

    /**
     * Unique identifier for this class in serialization. This was originally
     * created as a random number, and should be set to a new random number
     * whenever the class is changed in an incompatible way.
     */
    private static final long serialVersionUID = 0xec68f2160e05ab34L;

    /**
     * The unique identifier of this address. (That is, unique within the class
     * implementing the address.)
     */
    private final Number uid;

    /**
     * Creates a new communication address. The unique identifier must be unique
     * to within a specific <code>CommunicationAddress</code> implementation,
     * i.e. different classes can use different numbering schemes, but a single
     * class must give distinct numbers to distinct addresses (and the same
     * number for copies of the same address).
     *
     * @param uid The unique identifier of the address.
     * @see #getUID()
     */
    public CommunicationAddress(Number uid) {
        this.uid = uid;
    }

    /**
     * Returns a value such that equal addresses have equal hash codes. (The
     * reverse is not necessarily true; two different addresses can have the
     * same hash code, although the aim is to make the probability of that as
     * low as possible.)
     *
     * @return The hash code of this address.
     */
    @Override
    public final int hashCode() {
        return uid.hashCode();
    }

    /**
     * Returns true if and only if the given Object represents the same address
     * as this communication address. It's necessary to ensure that if a
     * communication object is cloned (e.g. by serialising and deserialising
     * it), then it will compare equal to the original. (Remember to also
     * override the <code>hashCode</code> in a consistent way!)
     * <p>
     * This is implemented via comparing the classes that the two addresses
     * belong to, and their unique identifiers. If both match, the addresses are
     * the same.
     *
     * @param obj The object to compare to. If this is not a communication
     * address, the comparison will return false.
     * @return Whether the two objects are equal.
     *
     * @see Object#hashCode()
     */
    @Override
    public final boolean equals(Object obj) {
        return ObjectUtils.equals(this, obj, CommunicationAddress::getUID);
    }

    /**
     * Returns a unique identifier for this communication address. Two addresses
     * refer to the same system if and only they have the same unique identifier
     * and belong to the same class, i.e. each class that implements
     * communication addresses specifies its own "namespace" for unique
     * identifiers.
     *
     * @return A unique identifier for the communication address.
     */
    public Number getUID() {
        return uid;
    }

    /**
     * Creates a socket that's open and connected to this communication address.
     * This is used only as a second attempt after first trying
     * <code>CommunicationEndpoint.newConnection()</code>, because this
     * alternative method does not allow for authentication of the device doing
     * the connection.
     * <p>
     * It's legal, and fairly common, for communication addresses not to support
     * this. The default implementation always throws an
     * <code>IncompatibleEndpointException</code>.
     *
     * @param connectingFrom The address from which the connection is being
     * made.
     * @return A socket, open and connected to the communicator at this
     * communication address.
     * @throws
     * xyz.acygn.mokapot.CommunicationEndpoint.IncompatibleEndpointException If
     * this sort of address does not allow arbitrary inbound connections without
     * authentication, or if <code>connectingFrom</code> is the wrong sort of
     * communication address
     * @throws IOException If something went wrong opening the connection
     * @see CommunicationEndpoint#newConnection(xyz.acygn.mokapot.Communicable)
     */
    public SocketLike connectHere(CommunicationAddress connectingFrom)
            throws CommunicationEndpoint.IncompatibleEndpointException,
            IOException {
        throw new CommunicationEndpoint.IncompatibleEndpointException(
                "this sort of address, " + toString()
                + ",  wants to be connected to via a suitable endpoint");
    }
}
