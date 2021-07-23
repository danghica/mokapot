package xyz.acygn.mokapot.examples.helloworld;

import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import xyz.acygn.mokapot.*;

/**
 * A very simple example program showing the minimum required to use the
 * distributed communication system.
 *
 * @author Alex Smith
 */
public class HelloWorld {

    /**
     * Creates a new <code>HelloWorld</code> object. Because the purpose of such
     * objects is to act as a demonstration of mutable state, no initialisation
     * is done in the constructor (rather, the field should be set
     * post-construction).
     */
    public HelloWorld() {
    }

    /**
     * The user's name. This is mutable so that this program can serve as a
     * test/demonstration of how the distributed communication system handles
     * mutable state.
     */
    private String name;

    /**
     * Set's the user's name.
     *
     * @param newName The name to set. Should not be <code>null</code>.
     */
    public void setName(String newName) {
        if (newName == null) {
            throw new NullPointerException("newName should not be null");
        }

        name = newName;
    }

    /**
     * Returns a greeting.
     *
     * @return The user's name, preceded by "Hello, " and followed by "!".
     * @throws IllegalStateException If the user's name has not been set yet
     */
    public String getGreeting() throws IllegalStateException {
        if (name == null) {
            throw new IllegalStateException();
        }
        return "Hello, " + name + "!";
    }

    /**
     * Main function. Asks the user their name, then prints a greeting. Note
     * that this currently uses hardcoded IPs, and assumes that there's a
     * distributed server on localhost:15238.
     *
     * @param args Command line arguments, ignored.
     * @throws IOException If there's a problem reading from the user
     * @throws KeyStoreException If client.p12 could not be read
     * @throws KeyManagementException If there's a problem initialising the Java
     * cryptographic libraries
     */
    @SuppressWarnings("UnusedAssignment")
    public static void main(String[] args) throws IOException,
            KeyStoreException, KeyManagementException {
        // start added code
        char[] password = {'t', 'e', 's', 't'};
        DistributedCommunicator communicator
                = new DistributedCommunicator("client.p12", password);
        communicator.startCommunication();
        CommunicationAddress serverAddress
                = communicator.lookupAddress(InetAddress.getLoopbackAddress(),
                        15238);
        // end added code

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));
        System.out.println("Enter your name:");
        String name;
        if (args.length == 0) {
            name = reader.readLine();
        } else {
            System.out.println(args[0]);
            name = args[0];
        }

        int runcount = 1;
        if (args.length >= 2) {
            try {
                runcount = Integer.valueOf(args[1]);
            } catch (NumberFormatException ex) {
                runcount = 1;
            }
        }

        HelloWorld greeter;

        for (int i = 0; i < runcount; i++) {
            // start original code
            // HelloWorld greeter = new HelloWorld();
            // end original code, setup modified code
            greeter = DistributedCommunicator.getCommunicator()
                    .runRemotely(() -> new HelloWorld(),
                            serverAddress);
            // end modified code

            greeter.setName(name);
            System.out.println(greeter.getGreeting());
        }

        // start added code
        greeter = null;
        DistributedCommunicator.getCommunicator().stopCommunication();
        // end added code
    }
}
