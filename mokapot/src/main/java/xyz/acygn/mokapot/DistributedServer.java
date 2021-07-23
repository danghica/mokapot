package xyz.acygn.mokapot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.Integer.parseUnsignedInt;
import java.net.InetAddress;
import static java.net.InetAddress.getLocalHost;
import static java.net.InetAddress.getLoopbackAddress;
import java.net.NetworkInterface;
import static java.net.NetworkInterface.getNetworkInterfaces;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The main class of this application. Creates a new distributed server and
 * listens for commands from elsewhere.
 *
 * @author Alex Smith
 */
public class DistributedServer {

    /**
     * Starts a new server that will listen to requests from remote systems to
     * run code.
     * <p>
     * Connections made to the server will be authenticated and authorised using
     * a whitelist stored in a .p12 file (which must be given on the command
     * line). The password protecting the .p12 file will be read from standard
     * input, or from a specified file.
     * <p>
     * Because a connection to the server could cause your system to run
     * arbitrary code, it's strongly recommended that you use additional
     * security measures on top of the protection offered by the whitelist. For
     * example, you could consider firewalling the TCP port on which the server
     * listens to prevent unexpected connections, and using a restrictive
     * security policy.
     * <p>
     * Note that, because this program's fundamental purpose is to run arbitrary
     * code received over a network connection, its security policy defaults to
     * deny-all rather than allow-all in order to reduce the risk of accidental
     * unsafe use. As such, you will need to set a security policy via your JVM
     * (e.g. via the use of the command-line argument
     * <code>-Djava.security.policy</code> to the VM itself).
     * <p>
     * The first argument to this program must be the .p12 file that contains
     * the cryptographic material in use.
     * <p>
     * The program should typically also be given command-line arguments
     * specifying how other systems will communicate with this system.
     * Specifying an integer among the arguments will specify the port number on
     * which this VM will listen to communications from outside; if not given,
     * the port number defaults to 15238. Note that if you run more than one
     * copy of this program on the same physical machine, each copy will need a
     * different port number.
     * <p>
     * It's also possible to specify an IP address; the server will use this IP
     * address as a "return address", letting other systems involved in the
     * computation know how to communicate with it. If no IP address is given,
     * this program will make an educated guess at what sort of address might be
     * appropriate; however, it is likely better to specify an address
     * explicitly, in case it guesses incorrectly.
     * <p>
     * To read the password for the .p12 file from another file, use the
     * <code>-k</code> option followed by its filename. To preserve two
     * authentication factors, the "file" in question should usually be a
     * special file such as a pipe; if it's a regular file on the same
     * filesystem as the .p12 file, there's a risk that both could be
     * compromised simultaneously. (Bear in mind that anyone with access to the
     * .p12 file and its password can run arbitrary code on your computer.)
     * <p>
     * Finally, it's possible to turn on various options. So far, only two are
     * implemented: <code>-d</code> turns on a "debug monitor" option that
     * causes all messages sent to and by the server to be summarised on
     * standard output, making it possible to see what the server is doing; and
     * <code>-w</code> turns on a "watchdog" mode in which the server will shut
     * down in an unclean manner if no communication has been received or sent
     * for 40 seconds (this is typically used in a situation in which lack of
     * communication for that long implies that the client has crashed and the
     * server will never shut down cleanly).
     *
     * @param args An array of command-line arguments, as explained above. So
     * far, the only things that can be specified here are Boolean switches (in
     * the standard hyphen+letter form), the .p12 file's filename, the port
     * number to listen on, and the IP address that other systems involved in
     * the communication will use to access this machine. If no arguments are
     * given, the port number defaults to 15238, and the IP address will be a
     * guess based on host information and network interface information for the
     * current machine. Note that if you run more than one copy of this program
     * on the same physical machine, the port numbers for the various instances
     * will need to differ.
     */
    public static void main(String[] args) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        boolean debug = false;
        boolean useWatchdog = false;
        boolean first = true;
        boolean preReadPasswordFrom = false;
        int port = -1;
        InetAddress address = null;
        String p12filename = null;
        InputStream readPasswordFrom = System.in;

        for (String arg : args) {
            if (first) {
                p12filename = arg;
                first = false;
                continue;
            }

            if (preReadPasswordFrom) {
                preReadPasswordFrom = false;
                try {
                    readPasswordFrom = new FileInputStream(arg);
                } catch (FileNotFoundException ex) {
                    userInputFailure("Could not open password file '"
                            + arg + "': " + ex.toString());
                }
                continue;
            }

            if (arg.equals("-d")) {
                debug = true;
                continue;
            }
            if (arg.equals("-w")) {
                useWatchdog = true;
                continue;
            }
            if (arg.equals("-k")) {
                preReadPasswordFrom = true;
                continue;
            }

            try {
                /* If it looks like a number, it's a port number. */
                int newport = parseUnsignedInt(arg);
                if (port == -1) {
                    port = newport;
                } else {
                    userInputFailure("Two or more port numbers specified: "
                            + port + ", " + newport);
                }
            } catch (NumberFormatException nfe) {
                try {
                    InetAddress newaddress = InetAddress.getByName(arg);
                    if (address == null) {
                        address = newaddress;
                    } else {
                        userInputFailure("Two or more IP addresses specified: "
                                + address + ", " + newaddress);
                    }
                } catch (UnknownHostException ex) {
                    /* It's unrecognised; exit. */
                    userInputFailure("Unrecognised command line option '" + arg
                            + "'");
                }
            }
        }
        if (first) {
            userInputFailure("No .p12 file given");
        }
        if (preReadPasswordFrom) {
            userInputFailure("No password file given");
        }

        if (port == -1) {
            port = 15238;
        }
        if (address == null) {
            try {
                /* Find an appropriate network interface to use; guess the
                   most globally visible one that isn't Internet-wide */
                Enumeration<NetworkInterface> interfaces
                        = getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface i = interfaces.nextElement();
                    if (i != null) {
                        /* We list all the addresses */
                        Enumeration<InetAddress> addresses
                                = i.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            InetAddress a = addresses.nextElement();
                            /* site-local has priority over link-local, which
                               has priority over loopback */
                            if (a.isLoopbackAddress() && address == null) {
                                address = a;
                            }
                            if (a.isLinkLocalAddress()
                                    && (address == null
                                    || address.isLoopbackAddress())) {
                                address = a;
                            }
                            if (a.isSiteLocalAddress()
                                    && (address == null
                                    || address.isLoopbackAddress()
                                    || address.isLinkLocalAddress())) {
                                address = a;
                            }
                        }
                    }
                }
            } catch (SocketException | SecurityException ex) {
                try {
                    /* If we get an I/O or security error enumerating network
                       interfaces, just default to the local host */
                    address = getLocalHost();
                } catch (UnknownHostException ex1) {
                    /* If we can't even do that, go for the loopback */
                    address = getLoopbackAddress();
                }
            }
        }

        EndpointKeystore keyStore;
        try {
            /* Read the password from the appropriate stream. */
            byte[] passwordBuffer = new byte[1024];
            int passwordLength = readPasswordFrom.read(passwordBuffer);
            /* Remove trailing newlines from the password. They probably weren't
               intended to be part of it. */
            while (passwordLength > 0
                    && passwordBuffer[passwordLength - 1] == '\n') {
                passwordLength--;
            }
            char[] password = new char[passwordLength];
            for (int i = 0; i < passwordLength; i++) {
                password[i] = (char) passwordBuffer[i];
            }
            Arrays.fill(passwordBuffer, (byte) 0);
            /* Read the .p12 file.
               (This also clears the password from memory.) */
            keyStore = EndpointKeystore.fromFile(p12filename, password);
        } catch (KeyStoreException | KeyManagementException | IOException ex) {
            userInputFailure("Could not read the .p12 file (or the password for it): "
                    + ex.toString());
            /* Java doesn't know userInputFailure never returns, so... */
            return;
        }

        if (readPasswordFrom != System.in) {
            try {
                readPasswordFrom.close();
            } catch (IOException ex) {
                /* This should be impossible; we opened the file for reading,
                   not writing, so it shouldn't be possible for anything to go
                   wrong during the flush. */
                throw new RuntimeException(ex);
            }
        }

        CommunicationEndpoint endpoint
                = new SecureTCPCommunicationEndpoint(keyStore, address, port);
        DistributedCommunicator communicator
                = new DistributedCommunicator(endpoint);

        final boolean finalDebug = debug;
        final boolean finalUseWatchdog = useWatchdog;
        /* Create a debug monitor that shuts down the server if no messages have
           been received for a length of time equal to the timeout (and also
           copies messages to stdout if necessary). */
        communicator.setDebugMonitor(new DebugMonitor() {

            @Override
            public void warning(String message) {
                System.err.println("WARNING: " + message);
                System.err.flush();
            }

            /**
             * A timer task that shuts down the server, without cleanup, when
             * the task is run. TimerTask's API doesn't take a Runnable in the
             * constructor, and isn't a functional interface, so we have to
             * create an inner class for even one-off purposes like this. (This
             * class is named, not anonymous, because it's referenced from two
             * points in the code and you can't do that with an anonymous
             * class.)
             */
            class AbruptShutdownTask extends TimerTask {

                @Override
                public void run() {
                    try {
                        System.out.println(
                                "No communications received recently; "
                                + "shutting down without cleanup");
                    } catch (Exception ex) {
                        /* If something went wrong with the output, make sure
                           we exit the entire JVM and not just the thread, via
                           squelching the exception */
                    }
                    System.exit(75);
                }
            };

            /**
             * The Timer that schedules the watchdog tasks.
             */
            private final Timer watchdog = new Timer(true);
            /**
             * A task that will abruptly shut down the server if not cancelled
             * before its timeout.
             */
            private TimerTask watchdogTask = new AbruptShutdownTask();

            {
                if (finalUseWatchdog) {
                    watchdog.schedule(watchdogTask, 40000);
                }
            }

            @Override
            public void newMessage(MessageInfo mi) {
                if (finalUseWatchdog) {
                    synchronized (this) {
                        watchdogTask.cancel();
                        watchdogTask = new AbruptShutdownTask();
                        watchdog.schedule(watchdogTask, 40000);
                    }
                }

                if (finalDebug) {
                    System.err.println(mi);
                    System.err.flush();
                }
            }
        });

        try {
            System.out.println(
                    "Starting communications, listening on address "
                    + communicator.getMyAddress());
            communicator.startCommunication();
        } catch (IOException ex) {
            System.err.println("Error: Could not start communications: "
                    + ex.getMessage());
            if (ex instanceof java.net.BindException && port < 1024) {
                System.err.println(
                        "Hint: many operating systems disallow the use of "
                        + "ports below 1024 to regular users");
            }
            System.exit(74);
        }
        System.out.println("Server is now ready for communications.");
    }

    /**
     * Exits the program after incorrect command line arguments are given. This
     * method exists simply to unclutter the usage-parsing code in
     * {@link #main(String[]) main}.
     *
     * @param errorMessage The reason the function was called (i.e. the way in
     * which the command line arguments were incorrect). This will be
     * incorporated into the error message.
     */
    private static void userInputFailure(String errorMessage) {
        System.err.println("Error: bad command line arguments: "
                + errorMessage);
        System.exit(64);
    }

    /**
     * Inaccessible constructor. This class has no non-static methods and is not
     * meant to be instantiated.
     */
    private DistributedServer() {
    }
}
