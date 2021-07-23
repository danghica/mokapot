package xyz.acygn.mokapot.test;

import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;

/**
 * A test routine to be run on the client half of a ClientServerTest. This
 * interface serves mostly to name the type of the routine, avoiding cluttering
 * up uses of it (and partly exists because Java doesn't have a
 * <code>TriConsumer</code> interface).
 *
 * @author Alex Smith
 */
@FunctionalInterface
public interface ClientTestCode {

    /**
     * Implements the client half of a client/server test.
     *
     * @param communicator The distributed communicator to communicate via.
     * @param addresses The addresses of the client and of the server.
     * @param testGroup The test group on which <code>ok()</code> calls should
     * be performed.
     * @throws java.lang.Exception If something goes wrong during the test.
     */
    void testCode(DistributedCommunicator communicator,
            AddressPair addresses, TestGroup testGroup) throws Exception;

    /**
     * A pair of a client address and a server address.
     */
    public static class AddressPair {

        /**
         * The client address.
         */
        private final CommunicationAddress clientAddress;
        /**
         * The server address.
         */
        private final CommunicationAddress serverAddress;

        /**
         * Creates a pair of two addresses.
         *
         * @param clientAddress The client address half of the pair.
         * @param serverAddress The server address half of the pair.
         */
        public AddressPair(CommunicationAddress clientAddress,
                CommunicationAddress serverAddress) {
            this.clientAddress = clientAddress;
            this.serverAddress = serverAddress;
        }

        /**
         * Returns the client address.
         *
         * @return The client address half of the pair.
         */
        public CommunicationAddress getClientAddress() {
            return clientAddress;
        }

        /**
         * Returns the server address.
         *
         * @return The server address half of the pair.
         */
        public CommunicationAddress getServerAddress() {
            return serverAddress;
        }
    }
}
