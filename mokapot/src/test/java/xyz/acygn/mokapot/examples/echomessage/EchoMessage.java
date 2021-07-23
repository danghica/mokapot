package xyz.acygn.mokapot.examples.echomessage;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.markers.Copiable;

/**
 * An example program that echoes messages between multiple systems.
 *
 * @author Alex Smith
 */
public class EchoMessage {

    /**
     * The distributed communicator to use when retrieving stream pairs on
     * remote systems.
     */
    private final DistributedCommunicator communicator;

    /**
     * The set of pairs to echo communications between.
     */
    private final Set<StreamPair> streamPairs = new HashSet<>();

    /**
     * Constructor. Used to set a distributed communicator for use in message
     * echoing. Also initialises the list of participating stream pairs to
     * standard input and standard output for this system.
     *
     * @param communicator The distributed communicator to use.
     */
    private EchoMessage(DistributedCommunicator communicator) {
        this.communicator = communicator;
        streamPairs.add(new StreamPair());
    }

    /**
     * Registers a stream pair from a remote system.
     *
     * @param address The remote system whose standard input and standard output
     * should be registered.
     */
    public void registerRemoteStreamPair(CommunicationAddress address) {
        streamPairs.add(communicator.runRemotely(
                () -> new StreamPair(), address));
    }

    /**
     * Main program of the message-echoing system.
     *
     * Assumes (for now) that a distributed server is running at the default
     * port on localhost.
     *
     * @param args Command line arguments, currently ignored.
     * @throws java.io.IOException If something goes wrong establishing
     * distributed communications
     * @throws java.security.KeyStoreException If client.p12 could not be read
     * @throws java.security.KeyManagementException If the cryptographic
     * framework could not be initialised
     */
    public static void main(String[] args)
            throws IOException, KeyStoreException, KeyManagementException {
        char[] password = {'t', 'e', 's', 't'};
        DistributedCommunicator communicator
                = new DistributedCommunicator("client.p12", password);
        communicator.startCommunication();

        EchoMessage echoer = new EchoMessage(communicator);
        echoer.registerRemoteStreamPair(
                communicator.lookupAddress(InetAddress.getLoopbackAddress(), 15238));
        echoer.start();
    }

    /**
     * Starts echoing messages between the stream pairs registered to this
     * message echoer.
     */
    @SuppressWarnings("Convert2Lambda")
    private void start() {
        for (StreamPair sp : streamPairs) {
            /* Note: these should not be converted to lambdas, because they
               might get sent remotely */
            sp.addExitListener(new Consumer<IOException>() {
                @Override
                public void accept(IOException ex) {
                    mostlyBroadcast(sp + " exited: " + ex.getMessage(), sp);
                }
            });
            sp.addInputListener(new Consumer<String>() {
                @Override
                public void accept(String message) {
                    mostlyBroadcast(message, sp);
                }
            });
        }
    }

    /**
     * Sends the given message to all stream pairs registered to this message
     * echoer, other than the specified stream pair.
     * <p>
     * This method is package-private (rather than private) due to a bug in
     * Javassist.
     *
     * @param message The message to send.
     * @param except The stream pair to not send the message to. This can be
     * <code>null</code>, to send the message to every stream pair.
     */
    void mostlyBroadcast(String message, StreamPair except) {
        streamPairs.stream().filter(
                (Predicate<StreamPair> & Copiable & Serializable) ((sp)
                -> (!sp.equals(except)))).forEach(
                        (Consumer<StreamPair> & Copiable & Serializable) ((sp)
                        -> sp.getOutputStream().println(message)));
    }
}
