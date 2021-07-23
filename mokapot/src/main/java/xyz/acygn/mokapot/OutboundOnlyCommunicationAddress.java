package xyz.acygn.mokapot;

/**
 * A unique identifier for a virtual machine that can only make outbound
 * connections. When a machine makes such a connection, this identifier is used
 * for other machines to be able to identify it. (They can subsequently send
 * messages to it via the use of the return half of an existing connection;
 * those return halves are stored in a hash table, and this communication
 * address used as the identifier.)
 * <p>
 * Note that it's very important that these are globally unique within a given
 * computation. The typical approach is to rely on the fact that each machine
 * involved on the computation will be on a white-list, and to use an index into
 * the white-list to identify it.
 * <p>
 * It's also part of this class's contract that the "same" machine always uses
 * the same identifier, as long as it should be trusted by the same destination
 * machines; this means that other machines can be informed of it once, and then
 * know the identifier for the future so that they can identify it later.
 *
 * @author Alex Smith
 * @see ReverseConnectMessage
 */
class OutboundOnlyCommunicationAddress extends CommunicationAddress {

    /**
     * Unique identifier for this class in serialization. This was originally
     * created as a random number, and should be set to a new random number
     * whenever the class is changed in an incompatible way.
     */
    private static final long serialVersionUID = 0x1edabc3976582e1aL;

    /**
     * Creates a new out-bound only communication address.
     *
     * @param uid The unique identifier of the machine.
     */
    public OutboundOnlyCommunicationAddress(Number uid) {
        super(uid);
    }

    /**
     * Creates a string representation of this address.
     *
     * @return The address's unique ID, in decimal, enclosed in square brackets.
     */
    @Override
    public String toString() {
        return "[" + getUID().toString() + "]";
    }
}
