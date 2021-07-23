package xyz.acygn.mokapot.test;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.CommunicationEndpoint;
import xyz.acygn.mokapot.CopiableRunnable;
import xyz.acygn.mokapot.DebugMonitor;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.EndpointKeystore;
import xyz.acygn.mokapot.IsolatedEndpoint;
import xyz.acygn.mokapot.SecureTCPCommunicationEndpoint;
import xyz.acygn.mokapot.util.Lazy;
import xyz.acygn.mokapot.util.Pair;
import xyz.acygn.mokapot.util.Stopwatch;
import xyz.acygn.mokapot.whitelist.KeytoolWhitelistControllerEngine;
import static xyz.acygn.mokapot.whitelist.WhitelistController.generateEndpointKeystoreSet;

/**
 * Class that handles one run of a test of the distributed communication system
 * that tests communication between the client and server (and possibly other
 * things). This class handles the behaviour common to all such tests; the
 * behaviour specific to the test is specified via a constructor argument.
 * <p>
 * All the tests work via creating a separate JVM for the server, and running
 * the client side of the tests on the current JVM.
 *
 * @author Valentin Blot, Alex Smith
 */
public class ClientServerTest extends ParallelTests {

    /**
     * Whether to disable timeouts during testing done with this class.
     */
    private static boolean disableTimeouts = false;

    /**
     * Whether the server should be run within our Java Virtual Machine, rather
     * than being run on a separate JVM.
     */
    private static boolean localServer = false;

    /**
     * The keystores to use when using a separate server. Global, because
     * keystore generation is slow; and lazy, because we don't always need it.
     */
    private static final Lazy<List<Pair<File, char[]>>> KEYSTORES
            = new Lazy<>(() -> {
                try {
                    /* The doPrivileged is required because Lazy is in Mokapot's
                       namespace, but its payload needs to be run with the
                       testsuite's permissions. */
                    return AccessController.doPrivileged(
                            (PrivilegedExceptionAction<List<Pair<File, char[]>>>) () -> generateEndpointKeystoreSet(
                                    new KeytoolWhitelistControllerEngine(), 2, 1));
                } catch (PrivilegedActionException ex) {
                    throw new RuntimeException(
                            "could not generate keystores", ex);
                }
            });

    /**
     * Set whether to disable timeouts in the distributed communicators made
     * while testing with this class. Disabling timeouts enables different
     * code-paths to be tested than testing with timeouts enabled.
     *
     * @param disableTimeouts Whether to disable timeouts.
     */
    static void setDisableTimeouts(boolean disableTimeouts) {
        ClientServerTest.disableTimeouts = disableTimeouts;
    }

    /**
     * Set whether to use a secondary communicator as opposed to a separate Java
     * executable. This enables testing of the secondary communicator code, and
     * may be faster and/or work in environments which have difficulty setting
     * up a separate server.
     *
     * @param localServer Whether to use a local server; <code>true</code> to
     * use a server within this JVM, <code>false</code> to spawn a separate JVM
     * to run the server.
     */
    static void setLocalServer(boolean localServer) {
        ClientServerTest.localServer = localServer;
    }

    /**
     * Return whether a secondary communicator is in use (as opposed to a
     * separate Java executable).
     *
     * @return <code>true</code> if the server addresses used in this test refer
     * to a communicator on the same JVM as the caller (which must be the
     * client); <code>false</code> if the test is using a separate JVM.
     */
    static boolean isLocalServer() {
        return localServer;
    }

    /**
     * Creates the test group object for the client/server test.
     *
     * @param testGroupName The name of the test group.
     * @param testCount The number of individual tests run by
     * <code>clientTest()</code>.
     * @param clientTestCode The code specific to this test.
     * @param testServerShutdown <code>true</code> if the clientTestRoutine
     * shuts the server down itself; <code>false</code> if the test driver
     * should shut the server down once the client is finished
     *
     * @see ClientTest
     * @see ServerTest
     *
     * @throws java.io.IOException If there was insufficient disk space to
     * generate the cryptographic material (or some comparable issue)
     * @throws java.security.KeyManagementException If the Java cryptography
     * libraries in use do not support the kind of cryptography required
     */
    protected ClientServerTest(String testGroupName, int testCount,
            ClientTestCode clientTestCode, boolean testServerShutdown)
            throws IOException, KeyManagementException {
        super(testGroupName,
                newTestPair(testGroupName, testCount,
                        clientTestCode, testServerShutdown));
    }

    /**
     * Helper function for the constructor that creates a ClientTest and
     * ServerTest with shared latches for specifying that the server is ready
     * and that the client is finished.
     *
     * @param testGroupName The name of the test group.
     * @param testCount The number of individual tests run by
     * <code>clientTest()</code>.
     * @param clientTestCode The code specific to this test; it will be given
     * the client's distributed communicator and the server's address as
     * arguments.
     * @param testServerShutdown <code>true</code> if the clientTestCode shuts
     * the server down itself; <code>false</code> if the test driver should shut
     * the server down once the client is finished.
     * @return A ClientTest/ServerTest pair that together implement the
     * ClientServerTest.
     * @throws java.io.IOException If there was insufficient disk space to
     * generate the cryptographic material (or some comparable issue)
     * @throws java.security.KeyManagementException If the Java cryptography
     * libraries in use do not support the kind of cryptography required
     */
    private static TestGroup[] newTestPair(String testGroupName, int testCount,
            ClientTestCode clientTestCode, boolean testServerShutdown)
            throws IOException, KeyManagementException {
        if (localServer) {
            /* In this situation, we don't have a server test group at all. */
            CountDownLatch serverReadyLatch = new CountDownLatch(0);
            CountDownLatch clientFinishedLatch = new CountDownLatch(1);
            return new TestGroup[]{
                new ClientTest(testGroupName, testCount, serverReadyLatch,
                clientFinishedLatch, clientTestCode, null, !testServerShutdown)
            };
        } else {
            CountDownLatch serverReadyLatch = new CountDownLatch(1);
            CountDownLatch clientFinishedLatch = new CountDownLatch(1);
            List<Pair<File, char[]>> keystores = KEYSTORES.get();
            try {
                return new TestGroup[]{
                    new ClientTest(testGroupName, testCount, serverReadyLatch,
                    clientFinishedLatch, clientTestCode,
                    EndpointKeystore.fromFile(
                    keystores.get(0).getFirst().toString(),
                    keystores.get(0).getSecond().clone()), false),
                    new ServerTest(testGroupName, serverReadyLatch,
                    clientFinishedLatch, keystores.get(1), testServerShutdown)};
            } catch (KeyStoreException ex) {
                throw new RuntimeException("invalid keystore was created", ex);
            }
        }
    }

    /**
     * The test group for the client half of the client/server test.
     */
    private static class ClientTest extends TestGroup {

        /**
         * A latch that will be reduced to 0 when the server is ready.
         */
        private final CountDownLatch serverReady;

        /**
         * A latch that the client sets to 0 when it's finished testing.
         */
        private final CountDownLatch clientFinished;

        /**
         * The code to run inside this half of the test.
         */
        private final ClientTestCode clientTest;

        /**
         * The keystore to use for the client.
         */
        private final EndpointKeystore keystore;

        /**
         * Whether <code>runRemotely</code> should be used to shut down the
         * server before stopping communication. This is necessary in cases
         * where we have a local server that isn't stopped by the test code.
         */
        private final boolean shutDownServerViaRunRemotely;

        /**
         * Creates the client half of a ClientServerTest.
         *
         * @param testGroupName The name of the ClientServerTest.
         * @param testCount The number of tests on the client half of the test.
         * @param serverReady A latch that signifies when the server is ready.
         * @param clientFinished A latch that will be set to 0 when the client
         * has finished testing.
         * @param clientTest The code to run inside this half of the test.
         * @param keystore The keystore to use, if communicating with a separate
         * server; or <code>null</code> if a local server should be created.
         * @param shutDownServerViaRunRemotely Whether to use
         * <code>runRemotely</code> to shut down the server via its
         * communicator.
         */
        ClientTest(String testGroupName, int testCount,
                CountDownLatch serverReady, CountDownLatch clientFinished,
                ClientTestCode clientTest, EndpointKeystore keystore,
                boolean shutDownServerViaRunRemotely) {
            super(testCount + 3, testGroupName + ": client");
            this.serverReady = serverReady;
            this.clientFinished = clientFinished;
            this.clientTest = clientTest;
            this.keystore = keystore;
            this.shutDownServerViaRunRemotely = shutDownServerViaRunRemotely;
        }

        /**
         * Implements the test actions of the client half of the
         * ClientServerTest. This creates a communicator, then delegates to the
         * parent's <code>clientTest</code> routine.
         *
         * @throws Exception If something goes wrong during testing.
         */
        @Override
        protected void testImplementation() throws Exception {
            System.out.println("# client: Constructing commmunicator");
            final CommunicationEndpoint endpoint
                    = keystore == null ? new IsolatedEndpoint()
                            : new SecureTCPCommunicationEndpoint(keystore);
            final DistributedCommunicator communicator
                    = new DistributedCommunicator(endpoint, !disableTimeouts);
            final CommunicationAddress clientAddress
                    = communicator.getMyAddress();
            final Stopwatch stopwatch = new Stopwatch(null).start();

            final DebugMonitorImpl monitor = new DebugMonitorImpl(
                    stopwatch, "client");
            communicator.setDebugMonitor(monitor);

            try {
                System.out.println("# client: Starting communication");
                communicator.enableTestHooks();
                communicator.startCommunication();
                communicator.getTestHooks().setEnableSlowDebugOperations(false);
                okEq(DistributedCommunicator.getCommunicator(), communicator,
                        "communicator is the global communicator");

                try {
                    System.out.println("# client: Waiting for server to initialize");
                    serverReady.await();

                    CommunicationAddress serverAddress;

                    if (keystore == null) {
                        serverAddress = communicator.getTestHooks()
                                .createSecondaryCommunicator(
                                        "test-server",
                                        new DebugMonitorImpl(
                                                stopwatch, "server"));
                    } else {
                        System.out.println("# client: lookup server address");
                        serverAddress = communicator.lookupAddress(
                                InetAddress.getLoopbackAddress(), 15239);
                    }

                    try {
                        System.out.println("# client: running test script");
                        clientTest.testCode(communicator,
                                new ClientTestCode.AddressPair(
                                        clientAddress, serverAddress), this);
                    } finally {
                        if (shutDownServerViaRunRemotely) {
                            System.out.println("# client: shutting down server");
                            /* The inner class is used here to avoid the need to
                               serialise a lambda over the network. */
                            communicator.runRemotely(
                                    new ServerShutdown(), serverAddress);
                        }
                    }
                } catch (Exception | Error ex) {
                    reportClientException(ex, "");
                    /* Make sure a test failure is reported. */
                    throw new RuntimeException(
                            "failure due to an earlier exception");
                } finally {
                    System.out.println("# client: shutting down communicator");
                    try {
                        communicator.stopCommunication();
                        ok(true, "no exception during shutdown");
                    } catch (Exception | Error ex) {
                        /* We might have had an exception thrown from above;
                           swallow this one so that that one doesn't get
                           overwritten. */
                        ok(false, "no exception during shutdown");
                        reportClientException(ex, " (during shutdown)");
                    }
                }
            } finally {
                System.out.println("# client: test finished");
                clientFinished.countDown();
                ok(monitor.getWarningCount() == 0, "no warnings");
            }
        }

        /**
         * Reports an exception to System.out. This is used in cases where we
         * can't just throw the exception, either because we need to report
         * <i>two</i> exceptions or because we need to stop a communicator that
         * may contain some of the exception's state.
         *
         * @param ex The exception to report.
         * @param s Additional text to be added to the message. This should be
         * parenthesised with a space before it, unless it's the empty string.
         */
        private static void reportClientException(Throwable ex, String s) {
            /* ex can't be allowed to be thrown past stopCommunication,
            as it may be a remote object. */
            System.out.println("# client: exception" + s + ": "
                    + ex.toString());
            for (StackTraceElement ste : ex.getStackTrace()) {
                System.out.println("# client: stack trace: "
                        + ste.toString());
            }
            Throwable t = ex.getCause();
            while (t != null) {
                System.out.println("# client: exception cause: " + t);
                for (StackTraceElement ste : t.getStackTrace()) {
                    System.out.println("# client: stack trace: "
                            + ste.toString());
                }
                t = t.getCause();
            }
        }

        /**
         * Debug monitor implementation for the client (maybe also server).
         */
        private static class DebugMonitorImpl implements DebugMonitor {

            /**
             * A stopwatch measuring time since the start of the test.
             */
            private final Stopwatch timeInTest;

            /**
             * What is being monitored: "client" or "server".
             */
            private final String where;

            /**
             * The number of observed warnings.
             */
            private final AtomicInteger warningCount;

            /**
             * Constructs a debug monitor.
             *
             * @param timeInTest A stopwatch measuring time spent in the test.
             * @param where What this monitors: "client" or "server".
             */
            DebugMonitorImpl(Stopwatch timeInTest, String where) {
                this.timeInTest = timeInTest;
                this.where = where;
                this.warningCount = new AtomicInteger(0);
            }

            /**
             * Returns the number of warnings this debug monitor has observed
             *
             * @return A count of warnings.
             */
            int getWarningCount() {
                return warningCount.get();
            }

            @Override
            public void warning(String message) {
                System.out.println("# " + where + ": WARNING: " + message);
                System.out.flush();
                warningCount.incrementAndGet();
            }

            @Override
            public void newMessage(MessageInfo mi) {
                String debugString = mi.toString();
                System.out.println("# " + where + ": message: (@"
                        + timeInTest.time(ChronoUnit.MILLIS)
                        + ") " + debugString + " ("
                        + mi.getCopiableDataBytes() + "b + "
                        + mi.getNoncopiableReferenceCount() + "o, "
                        + mi.getMarshalTimeNanos() + " ns)");
                System.out.flush();
            }
        }

        /**
         * A class that's used as an argument to runRemotely to shut down the
         * server over the network. This is a concrete, non-lambda,
         * non-anonymous class in order to make it as simple to send as possible
         * (thus, ideally, meaning that mistakes found during testing will be
         * found in the tests that are meant to test for them rather than in the
         * server shutdown).
         */
        private static class ServerShutdown implements CopiableRunnable {

            /**
             * A number that uniquely identifies this class. This is used only
             * to comply with the API for <code>Serializable</code>, which is
             * implemented by <code>CopiableRunnable</code> to ensure that the
             * created objects will have factory methods; in this particular
             * case, though (not being a lambda), we don't need one, so this is
             * actually irrelevant. It's set to a random number in case it ever
             * becomes relevant in the future, though.
             */
            private static final long serialVersionUID = 0x2e484329f476424aL;

            /**
             * Shuts down the currently running communicator.
             */
            @Override
            public void run() {
                DistributedCommunicator serverCommunicator
                        = DistributedCommunicator.getCommunicator();
                serverCommunicator.asyncStopCommunication();
            }
        }
    }

    /**
     * The test group for the server half of the client/server test.
     */
    private static class ServerTest extends TestGroup {

        /**
         * A latch that the server will reduce to 0 when it's ready.
         */
        private final CountDownLatch serverReady;
        /**
         * A latch that the client reduces 0 when it's finished.
         */
        private final CountDownLatch clientFinished;

        /**
         * The keystore to use, as a filename/password pair.
         */
        private final Pair<File, char[]> keystore;

        /**
         * True if the client shuts down the server; false if we do it ourself.
         */
        private final boolean clientShutsDownServer;

        /**
         * Creates the server half of the ClientServerTest.
         *
         * @param testGroupName The ClientServerTest's group name.
         * @param serverReady A latch that will be set to 0 when the server is
         * ready.
         * @param clientFinished A latch that the client will set to 0 when it's
         * finished.
         * @param keystore The keystore to use on the server (expressed as a
         * filename/password pair).
         * @param clientShutsDownServer True if the client is responsible for
         * shutting down the server (which can be tested)
         */
        ServerTest(String testGroupName, CountDownLatch serverReady,
                CountDownLatch clientFinished, Pair<File, char[]> keystore,
                boolean clientShutsDownServer) {
            super(clientShutsDownServer ? 2 : 1, testGroupName + ": server");
            this.serverReady = serverReady;
            this.clientFinished = clientFinished;
            this.keystore = keystore;
            this.clientShutsDownServer = clientShutsDownServer;
        }

        /**
         * Starts the server process, and waits for it to exit, or for 1 minute.
         * Also contains tests that the server process exited naturally and
         * didn't report an error.
         *
         * @throws java.io.IOException If there was a failure to setup the
         * server process
         * @throws java.lang.InterruptedException If the thread running the test
         * was externally interrupted while waiting for the server to end
         */
        @Override
        protected void testImplementation()
                throws IOException, InterruptedException {
            // TODO: This isn't safe against spaces in filenames.
            Process server = new ProcessBuilder("java",
                    "-classpath", System.getProperty("java.class.path"),
                    "-Djava.security.policy=mokapot/src/main/resources/localhost-only.policy",
                    "-Djava.security.manager",
                    "-Dmokapot.jar=" + System.getProperty("mokapot.jar"),
                    "-ea", "-Xdebug", "-Xnoagent",
                    "-Xrunjdwp:transport=dt_socket,server=y,address=15234,suspend=n",
                    "xyz.acygn.mokapot.DistributedServer",
                    keystore.getFirst().toString(),
                    "-w", "127.0.0.1", "15239").redirectError(
                    ProcessBuilder.Redirect.INHERIT).start();
            try (OutputStream serverInput = server.getOutputStream()) {
                for (char c : keystore.getSecond()) {
                    serverInput.write((int) c);
                }
            }
            BufferedReader serverOutput
                    = new BufferedReader(
                            new InputStreamReader(
                                    new BufferedInputStream(server.getInputStream())));

            /* Wait for the server to setup. */
            String serverLine;
            do {
                serverLine = serverOutput.readLine();
                System.out.println("# server says: " + serverLine);
            } while (!serverLine.startsWith("Server is now ready"));

            /* Tell the client that the server's ready. */
            serverReady.countDown();

            /* Wait for the client to finish. */
            clientFinished.await();

            try {
                /* Who's shutting down the server? */
                if (clientShutsDownServer) {
                    ok(server.waitFor(10, TimeUnit.SECONDS),
                            "server terminates naturally without timing out");
                }
            } finally {
                server.destroy();
                server.waitFor(5, TimeUnit.SECONDS);
                server.destroyForcibly();
                server.waitFor();
                okEq(server.exitValue(), clientShutsDownServer ? 0 : 143,
                        "server exit code");
            }
        }
    }
}
