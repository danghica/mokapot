package xyz.acygn.mokapot;

import xyz.acygn.mokapot.util.ServerSocketLike;
import xyz.acygn.mokapot.util.SocketLike;

/**
 * A communication endpoint that does not support communication with other Java
 * virtual machines. Such an endpoint is only useful for testing or benchmarking
 * purposes, when running a test or benchmarking operation entirely on a single
 * virtual machine.
 * <p>
 * Communication via such an endpoint is only possible via the use of secondary
 * endpoints (which can communicate with anything on the same Java virtual
 * machine).
 *
 * @author Alex Smith
 */
public class IsolatedEndpoint implements CommunicationEndpoint {

    @Override
    public CommunicationAddress getAddress() {
        return new Address();
    }

    @Override
    public SocketLike newConnection(Communicable remoteAddress)
            throws IncompatibleEndpointException {
        throw new IncompatibleEndpointException(
                "IsolatedEndpoint cannot create connections");
    }

    @Override
    public ServerSocketLike newListenSocket() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "IsolatedEndpoint cannot listen for connections");
    }

    @Override
    public void initialVerifySocket(SocketLike socket) {
        // nothing to do
    }

    @Override
    public void informOfCommunicatorStart(DistributedCommunicator communicator) {
        // nothing to do
    }

    @Override
    public void informOfCommunicatorStop(DistributedCommunicator communicator) {
        // nothing to do
    }

    /**
     * The address used by an isolated endpoint. This has no functionality (as
     * isolated endpoints can't be referred to by other systems), and thus only
     * exists to be compared with itself to see if it's equal (functionality
     * implemented by default in <code>CommunicationAddress</code>). We only
     * allow one isolated endpoint per JVM, so this is effectively a singleton.
     */
    private static class Address extends CommunicationAddress {

        /**
         * Creates the isolated endpoint's address.
         */
        Address() {
            super(1);
        }

        /**
         * Unique identifier for this class in serialization. This was
         * originally created as a random number, and should be set to a new
         * random number whenever the class is changed in an incompatible way.
         */
        private static final long serialVersionUID = 0xe2ee3caade376437L;

        /**
         * The name of this communication address. This is "main".
         *
         * @return <code>"main"</code>.
         */
        @Override
        public String toString() {
            return "main";
        }
    }
}
