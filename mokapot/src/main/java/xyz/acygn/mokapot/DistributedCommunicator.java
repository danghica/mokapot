package xyz.acygn.mokapot;

import java.io.IOException;
import java.io.OutputStream;
import static java.lang.Thread.currentThread;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import static xyz.acygn.mokapot.Authorisations.UNRESTRICTED;
import static xyz.acygn.mokapot.ClassKnowledge.knowledgeForActualClass;
import static xyz.acygn.mokapot.GlobalID.getCurrentThreadID;
import static xyz.acygn.mokapot.GlobalID.setCurrentThreadID;
import static xyz.acygn.mokapot.NonCopiableKnowledge.StandinFactoryPurpose.STANDIN_WRAPPER;
import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.markers.NonMigratable;
import xyz.acygn.mokapot.skeletons.ExposedMethods;
import xyz.acygn.mokapot.skeletons.InvokeByCode;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.skeletons.StandinStorage;
import xyz.acygn.mokapot.skeletons.TrivialStandinStorage;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;
import xyz.acygn.mokapot.util.BackgroundGarbageCollection;
import xyz.acygn.mokapot.util.CrossThreadReadWriteLock;
import xyz.acygn.mokapot.util.DeterministicAutocloseable;
import xyz.acygn.mokapot.util.DoublyWeakConcurrentMap;
import xyz.acygn.mokapot.util.ExpirableMap;
import xyz.acygn.mokapot.util.Holder;
import xyz.acygn.mokapot.util.Lazy;
import xyz.acygn.mokapot.util.Pair;
import xyz.acygn.mokapot.util.ResettableThreadLocal;
import xyz.acygn.mokapot.util.SocketLike;
import xyz.acygn.mokapot.util.Stopwatch;
import static xyz.acygn.mokapot.util.ThreadUtils.delayInterruptions;
import static xyz.acygn.mokapot.util.ThreadUtils.delayInterruptionsRv;
import xyz.acygn.mokapot.util.WeakConcurrentSet;
import xyz.acygn.mokapot.wireformat.FakeDescriptionStream;

/**
 * An object that handles communication to and from remote systems.
 * <p>
 * At any time, there is up to one "main distributed communicator" for any given
 * Java virtual machine; one of these must be active in order to be able to
 * carry out distributed communications. As such, it's sensible to talk about
 * "the main distributed communicator" for a particular JVM.
 * <p>
 * Starting up communications creates a listener that other systems can use to
 * communicate with and run code on this system; this is a necessary part of
 * running programs on more than one system. (That is, starting outbound
 * communications starts inbound communications at the same time.)
 * <p>
 * It is possible, for testing purposes, to run multiple distributed
 * communicators on the same Java virtual machine, with some restrictions;
 * however, this is not a normal configuration. See
 * <code>TestHooks#createSecondaryCommunicator</code> for more information.
 * <p>
 * <b>This class currently does no sandboxing of its own</b>, so ensure that the
 * port is correctly secured via other means, e.g. installing a restrictive
 * security manager, setting up appropriate firewalls, and using an endpoint
 * implementation that rejects unauthorised connections. As a precaution,
 * attempts to construct instances of the class will fail unless a security
 * manager is installed.
 *
 * @author Alex Smith
 * @see TestHooks#createSecondaryCommunicator(java.lang.String,
 * xyz.acygn.mokapot.DebugMonitor)
 */
public class DistributedCommunicator {

    /* Static initialisation that needs to be done before any use of the
       communicator is made. */
    static {
        ExposedMethods.setSingleton(new ExposedMethodsImpl());
    }

    /**
     * The main distributed communicator that is active on this JVM. There can
     * be at most one of these at a time; and the main distributed communicator
     * cannot be shut down while additional communicators are active. If there
     * is no active distributed communicator, then most distributed operations
     * will be unavailable.
     *
     * All write access to this field must be synchronized on
     * <code>DistributedCommunicator.class</code>. Read access must also be
     * synchronized the same way unless the current thread holds a lock that
     * prevents the distributed communicator in question being stopped.
     */
    private static DistributedCommunicator mainCommunicator;

    /**
     * Returns the main distributed communicator, if one exists. Only one main
     * distributed communicator can be active on a particular JVM at any given
     * time.
     *
     * @return The main distributed communicator for this Java virtual machine.
     */
    static DistributedCommunicator getMainCommunicator() {
        synchronized (DistributedCommunicator.class) {
            return mainCommunicator;
        }
    }

    /**
     * Specifies whether this is being used as a secondary communicator. If not,
     * it's a main communicator. Secondary communicators don't need to be
     * specified as active in <code>mainCommunicator</code>, and in general have
     * less functionality.
     */
    private final boolean secondary;

    /**
     * Returns the active distributed communicator with which the calling thread
     * is allowed to interact. This will, in most cases, be the main distributed
     * communicator, except when this method is called indirectly by code that
     * was run via a remote call via a secondary communicator.
     * <p>
     * Use of this method is preferable to storing the main communicator in a
     * global variable, because it prevents accidental sharing of data between
     * threads that are associated with different communicators. However, the
     * two techniques will be equivalent unless you are running multiple
     * communicators on the same machine for testing purposes.
     *
     * @return The appropriate distributed communicator for this thread, or
     * <code>null</code> if there are no active communicators or if this
     * thread's communicator has been shut down.
     */
    public static DistributedCommunicator getCommunicator() {
        synchronized (DistributedCommunicator.class) {
            DistributedCommunicator myCommunicator
                    = PooledThread.getCommunicator();
            if (myCommunicator != null) {
                return myCommunicator;
            }
            return mainCommunicator;
        }
    }

    /**
     * Whether test hooks should remain enabled once the communicator is
     * started. If this is <code>false</code>, <code>testHooks</code> will be
     * forever set to <code>null</code> in <code>startCommunication()</code>;
     * this is a security mechanism to prevent the test hooks being used to
     * bypass the security manager.
     */
    private boolean testHooksEnabled = false;

    /**
     * Whether test hooks have been permanently disabled. This happens when
     * communications are started without first enabling them.
     */
    private final static AtomicBoolean testHooksPermanentlyDisabled
            = new AtomicBoolean(false);

    /**
     * Allows <code>startCommunication()</code> to be called on this
     * communicator without permanently disabling the test hooks.
     */
    public void enableTestHooks() {
        this.testHooksEnabled = true;
    }

    /**
     * Returns a set of testing hooks for this communicator. These enable
     * low-level operations to be accessed directly, possibly in insecure ways.
     * As such, this method may only be called if, for all communicators on the
     * current Java virtual machine, <code>enableTestHooks()</code> was called
     * on that communicator prior to starting it.
     * <p>
     * This method may only be called while the communicator is running (and not
     * currently in the process of being stopped). It will prevent the
     * communicator being stopped until the resulting <code>TestHooks</code>
     * object is deallocated (thus you may need to explicitly <code>null</code>
     * it out).
     *
     * @return A set of test hooks.
     * @throws IllegalStateException If any communicator has been started
     * without a previous call to <code>enableTestHooks()</code> on that
     * communicator; or if the communicator is not running
     */
    public TestHooks getTestHooks() throws IllegalStateException {
        if (testHooksPermanentlyDisabled.get()) {
            throw new IllegalStateException("getTestHooks() has been "
                    + "permanently disabled by startCommunication()");
        }
        if (this != mainCommunicator && !secondary) {
            throw new IllegalStateException("this communicator is not running");
        }

        TestHooks th = new Marshalling(this);
        try {
            DeterministicAutocloseable da = maybeGetKeepaliveLock(
                    ShutdownStage.TEST_HOOKS, "getTestHooks");
            BackgroundGarbageCollection.addFinaliser(th, da::close);
            return th;
        } catch (AutocloseableLockWrapper.CannotLockException ex) {
            throw new IllegalStateException(
                    "getTestHooks() during stopCommunication()");
        }
    }

    /**
     * Package-private version of <code>getTestHooks()</code>. Allows hooks that
     * would allow unsafe operations to be returned even if the user hasn't
     * enabled those operations being exposed to the user (the assumption is
     * that the caller will ensure that they're only used in a safe manner).
     *
     * @return A set of functions that enable marshalling and/or other unsafe
     * operations using this communicator.
     */
    Marshalling getMarshalling() {
        return new Marshalling(this);
    }

    /**
     * The number of seconds before a remote reference is considered to have
     * broken if the remote system doesn't tell us it's still alive.
     * <p>
     * Keepalive messages, indicating that a remote object is still alive, are
     * sent in a number of seconds equal to half this value, in order to ensure
     * that they arrive before expiry.
     */
    public static final int LIFETIME_TIMEOUT = 30;

    /**
     * The number of seconds a network connection is held open for before it's
     * closed due to lack of use.
     */
    public static final int CONNECTION_TIMEOUT = 60;

    /**
     * The debug monitor used for this communicator.
     */
    private DebugMonitor debugMonitor = null;

    /**
     * Sets the debug monitor for this communicator. Installing a new debug
     * monitor will replace the old one. In debug monitor mode, every message
     * sent by or received by the communicator will call the debug monitor,
     * giving it some basic information on the message in question.
     *
     * @param debugMonitor The new debug monitor. This can be <code>null</code>,
     * in which case debug monitoring will be turned off.
     */
    public void setDebugMonitor(DebugMonitor debugMonitor) {
        this.debugMonitor = debugMonitor;
    }

    /**
     * A method called when an exception is triggered handling a request from
     * another system, and we can't communicate it back to the caller. This
     * could be because it was a request to perform a background task that
     * failed (with the caller not identified), or because the caller couldn't
     * be contacted.
     *
     * @param ex The exception that occurred.
     */
    void asyncExceptionHandler(Throwable ex) {
        // TODO: This should probably be customizable.
        System.err.println("Exception (on " + myEndpoint
                + ") handling a remote request:");
        ex.printStackTrace(System.err);
    }

    /**
     * Sends a warning via the debug monitor, if there is one.
     *
     * @param message The natural-language description of the warning.
     */
    void sendWarning(String message) {
        if (debugMonitor != null) {
            debugMonitor.warning(message);
        }
    }

    /**
     * The communication endpoint via which this virtual machine communicates
     * with other virtual machines. As it's the distributed communicator itself
     * that is contacted, this is effectively a statement of how the distributed
     * communicator should send communications to outside, and how and whether
     * it should listen to communications from outside.
     */
    private final CommunicationEndpoint myEndpoint;

    /**
     * The thread used for incoming distributed communications. This is
     * nullable; it's <code>null</code> while the communicator is inactive, and
     * set to the communication thread while the communicator is active. When
     * the communicator is deactivated, the old acceptor is destroyed, as each
     * such thread can only be used once.
     * <p>
     * Access to this field should only be done from within methods that are
     * synchronized on the monitor of this <i>class</i>.
     */
    private ConnectionManager acceptor = null;

    /**
     * A table of threads that have call stack elements that go via this
     * distributed communicator on their call stack. The table is used to
     * implement threads that "exist on multiple machines"; a distributed thread
     * (represented by a <code>GlobalID</code>) is made out of multiple local
     * threads (represented by a <code>Thread</code> object): one for each
     * machine that's involved in the thread's call stack.
     * <p>
     * The keys of the table are global IDs of distributed threads. The values
     * are blocking queues: if the distributed thread is currently running on
     * this machine, the queue is idle (nothing blocking on take nor on put),
     * and if the distributed thread is currently running on some other machine,
     * this machine's corresponding local thread will block on the queue by
     * attempting to take a message from it. The message it will take will be
     * the next message destined for that thread (whether a followup method
     * call, or a return value).
     * <p>
     * Once a thread's call stack no longer goes through a particular
     * distributed communicator, i.e. the call that originally added the thread
     * to <code>activeThreadIds</code> returns, the thread will be removed from
     * the map (and if the distributed thread didn't start on this system, the
     * corresponding local thread will be held in thread projection tracker mode
     * in case it has local state).
     *
     * @see #sendMessageSync(xyz.acygn.mokapot.SynchronousMessage,
     * xyz.acygn.mokapot.CommunicationAddress)
     * @see #labelThreadActive(boolean)
     */
    private final ConcurrentMap<GlobalID, ActiveThreadInfo> activeThreadIDs
            = new ConcurrentHashMap<>();

    /**
     * The thread projection tracker for the current thread, if any.
     */
    private final ResettableThreadLocal<ThreadProjectionTracker> projectionTrackers
            = new ResettableThreadLocal<>();

    /**
     * The set of all thread projection trackers in use by any thread.
     */
    private final WeakConcurrentSet<ThreadProjectionTracker> allProjectionTrackers
            = new WeakConcurrentSet<>();

    /**
     * An index of which wrapper standins belong to which non-standin objects.
     * This is used for performing standin operations on objects that weren't
     * originally allocated as standins. (In such a case, it's important that
     * each object only has one standin because otherwise the same object could
     * end up with two different location managers.)
     */
    private final DoublyWeakConcurrentMap<Object, Standin<?>> wrapperStandins
            = new DoublyWeakConcurrentMap<>();

    /**
     * A set of standins that must not be taken offline. The most likely reason
     * for this is that there are references to the standin's referent that
     * don't go via the standin, meaning that the referent could get
     * accidentally cloned by the offlining process. Another possibility is that
     * the user has requested that the standin is locked to this JVM, for some
     * reason.
     * <p>
     * Standins with <code>NonMigratable</code> referents also fall into the
     * non-offlinable category; however, as this information can be statically
     * determined by a single <code>instanceof</code>, these objects are left
     * out of this set in order to save on memory and on CPU time adding and
     * removing them.
     */
    private final WeakConcurrentSet<Standin<?>> nonofflinableStandins
            = new WeakConcurrentSet<>();

    /**
     * A table of lifetime managers that refer to local location managers. Used
     * allow an existing lifetime manager to be re-weighted rather than needing
     * to create a new one, and to enumerate all lifetime managers on the
     * system. This is also used to provide visibility to location managers for
     * remote systems. (Normally a location manager is accessed via standin
     * storage, but in the case of remote-to-local references, the standin won't
     * be visible and might not even exist.)
     */
    private final ExpirableMap<ObjectLocation, LifetimeManager> allLifetimeManagers;

    /**
     * An executor service which is alive for as long as the communicator is.
     */
    private ScheduledExecutorService executorService = null;

    /**
     * A set of locks that track whether this distributed communicator is
     * currently in use. Used to handle graceful shutdown (i.e. shutting down
     * the communicator once it's idle). Anything that requires the communicator
     * to stay active (e.g. threads running via the communicator, objects to
     * which long references exist via this communicator) takes the read side of
     * the lock. That means that taking the write side of the lock means that
     * the communicator can safely be shut down.
     * <p>
     * Each shutdown stage has its own lock; there's an invariant that these are
     * write-locked "from the top", i.e. some prefix of the shutdown stages will
     * be write-locked at any given time. A fully shut-down communicator will
     * have every stage write-locked; a running communicator will have no stage
     * write-locked (it may have some stages read-locked, depending on what it's
     * doing at the time).
     */
    private final EnumMap<ShutdownStage, CrossThreadReadWriteLock> busy;

    /**
     * Creates a new distributed communicator. The communicator will not start
     * communicating immediately, and cannot be used until it is.
     * <p>
     * When creating the communicator, it's necessary to specify a communication
     * endpoint. This will be used for three purposes: extracting the address
     * from it to tell other systems how to connect to this system; determining
     * what settings should be used when listening for communications from
     * outside (including how to authenticate and authorise these connections);
     * and authenticating the connector when it makes outbound connections to
     * other communicators. Thus, the address contained within the endpoint will
     * be determined to some extent by the conditions of the local system (e.g.
     * if TCP is being used for the communication, then any IP address contained
     * within the communication address as an "address via which the endpoint
     * can be contacted" must actually refer to the local system). However,
     * there may be more flexibility in other parts of the address (e.g. if TCP
     * is being used for the communication, then any port that can receive
     * messages from the other machines involved can be used as part of the
     * address).
     *
     * @param myEndpoint The endpoint which will be used for communication with
     * other machines (creating outbound connections and, if supported,
     * receiving inbound exceptions).
     * @throws SecurityException If there is no security manager installed
     *
     * @see #startCommunication()
     */
    public DistributedCommunicator(CommunicationEndpoint myEndpoint) {
        this(myEndpoint, true);
    }

    /**
     * Creates a new distributed communicator, potentially with custom timeout
     * behaviour. The communicator will not start communicating immediately, and
     * cannot be used until it is. (Most users will not need to specify custom
     * timeout behaviour, and should use a different constructor; disabling
     * timeouts can be useful for testing or to save on memory, but reduces the
     * ability to recover from errors. Note that a timer will still be used to
     * send "object is still alive" messages, so that communicators with
     * timeouts enabled and with timeouts disabled are compatible with each
     * other.)
     * <p>
     * When creating the communicator, it's necessary to specify a communication
     * endpoint. This will be used for three purposes: extracting the address
     * from it to tell other systems how to connect to this system; determining
     * what settings should be used when listening for communications from
     * outside (including how to authenticate and authorise these connections);
     * and authenticating the connector when it makes outbound connections to
     * other communicators. Thus, the address contained within the endpoint will
     * be determined to some extent by the conditions of the local system (e.g.
     * if TCP is being used for the communication, then any IP address contained
     * within the communication address as an "address via which the endpoint
     * can be contacted" must actually refer to the local system). However,
     * there may be more flexibility in other parts of the address (e.g. if TCP
     * is being used for the communication, then any port that can receive
     * messages from the other machines involved can be used as part of the
     * address).
     *
     * @param myEndpoint The endpoint which will be used for communication with
     * other machines (creating outbound connections and, if supported,
     * receiving inbound exceptions).
     * @param useTimeouts If <code>true</code>, does normal timeout behaviour;
     * if <code>false</code>, does not use timeouts at all for normal operations
     * (connections will stay open indefinitely, and objects stored elsewhere
     * will never expire), except that remote systems will still be periodically
     * informed that local objects are still in use. Timeouts may also be used
     * to detect exceptional or unusual conditions (such as objects not being
     * deallocated even though there should be no references left to them),
     * regardless of the setting of this option.
     * @throws SecurityException If there is no security manager installed
     *
     * @see #startCommunication()
     */
    public DistributedCommunicator(CommunicationEndpoint myEndpoint,
            boolean useTimeouts) {
        this(myEndpoint, useTimeouts, false);
    }

    /**
     * Fully general, package-private constructor. This contains the actual
     * implementation of the constructor, and is the only way to create a
     * secondary communicator.
     *
     * @param myEndpoint The endpoint to use.
     * @param useTimeouts Whether to enable timeouts.
     * @param secondary Whether this should be a secondary communicator.
     */
    DistributedCommunicator(CommunicationEndpoint myEndpoint,
            boolean useTimeouts, boolean secondary) {
        if (System.getSecurityManager() == null) {
            throw new SecurityException(
                    "Attempting to use DistributedCommunicator with a "
                    + "lax security policy is a bad idea; "
                    + "install a security manager.");
        }

        this.allLifetimeManagers = new ExpirableMap<>(LIFETIME_TIMEOUT,
                useTimeouts ? SECONDS : null);
        this.myEndpoint = myEndpoint;
        this.secondary = secondary;

        /* While the communicator is idle, <code>busy</code> is write-locked.
           That's our invariant, so we need to take the lock now to enforce
           it. */
        busy = new EnumMap<>(ShutdownStage.class);
        for (ShutdownStage s : ShutdownStage.values()) {
            CrossThreadReadWriteLock l = new CrossThreadReadWriteLock();
            l.writeLock().lock();
            busy.put(s, l);
        }
    }

    /**
     * Creates a new distributed communicator for two-way communication, using
     * default settings to create the endpoint. This is a convenience
     * constructor that means you don't have to create endpoints, addresses,
     * etc. yourself.
     *
     * @param p12FileName The filename of the .p12 file that holds the
     * cryptographic material to use for the connection. (This consists of a
     * certificate to identify this communicator, plus a white-list of
     * certificates that are allowed to communicate with it.)
     * @param password The password for the .p12 file. This will be mutated to
     * be all-zeroes before the method returns.
     * @param ipAddress The IP address of the computer on which this
     * communicator is running. This is required so that it can tell other
     * computers how to connect to it.
     * @param port The port number on which this computer should listen.
     * @throws KeyStoreException If something is wrong with the content of the
     * given .p12 file.
     * @throws KeyManagementException If something goes wrong trying to
     * configure the TLS library to make use of the provided cryptographic
     * material.
     * @throws IOException If something goes wrong attempting to load the given
     * .p12 file from disk.
     */
    public DistributedCommunicator(String p12FileName, char[] password,
            InetAddress ipAddress, int port)
            throws KeyStoreException, KeyManagementException, IOException {
        this(new SecureTCPCommunicationEndpoint(
                EndpointKeystore.fromFile(p12FileName, password),
                ipAddress, port));
    }

    /**
     * Creates a new distributed communicator for one-way communication, using
     * default settings to create the endpoint. This is a convenience
     * constructor that means you don't have to create the endpoint yourself.
     * <p>
     * The communicator will only be able to make outbound connections, i.e. any
     * inbound data can only be received via the machine you connected to. This
     * is useful to bypass firewalls that allow outbound connections only, or in
     * cases where your machine does not have a public IP address.
     *
     * @param p12FileName The filename of the .p12 file that holds the
     * cryptographic material to use for the connection. (This consists of a
     * certificate to identify this communicator, plus a white-list of
     * certificates that are allowed to communicate with it.)
     * @param password The password for the .p12 file. This will be mutated to
     * be all-zeroes before the method returns.
     * @throws KeyStoreException If something is wrong with the content of the
     * given .p12 file.
     * @throws KeyManagementException If something goes wrong trying to
     * configure the TLS library to make use of the provided cryptographic
     * material.
     * @throws IOException If something goes wrong attempting to load the given
     * .p12 file from disk.
     */
    public DistributedCommunicator(String p12FileName, char[] password)
            throws KeyStoreException, KeyManagementException, IOException {
        this(new SecureTCPCommunicationEndpoint(
                EndpointKeystore.fromFile(p12FileName, password)));
    }

    /**
     * Starts the communicator's ability to communicate. Until the communicator
     * is told to stop communicating (and successfully shuts down), it will
     * accept remote commands to run code, sent via the given port. Only one
     * main communicator per JVM may be communicating at any given time. (It is
     * possible to have additional "secondary" communicators via the use of test
     * hooks, but this is an unusual configuration.)
     * <p>
     * Starting the communicator is also necessary before using it for outbound
     * requests, to run code on other systems; this is because running code
     * remotely may sometimes require some of the code to run on the local
     * system (e.g. accesses to local objects), and thus the remote system will
     * need to be able to run code here.
     *
     * @throws java.io.IOException If the operating system reported a failure to
     * start listening for external connections
     * @throws IllegalStateException If communications are already running
     */
    public void startCommunication()
            throws IOException, IllegalStateException {

        synchronized (DistributedCommunicator.class) {

            if (acceptor != null) {
                throw new IllegalStateException(
                        "Starting communication twice without stopping it in between");
            }

            if (mainCommunicator != null && !secondary) {
                throw new IllegalStateException(
                        "Starting two main communicators at once");
            }

            if (mainCommunicator == null && secondary) {
                throw new IllegalStateException(
                        "Starting a secondary communicator with no main communicator");
            }

            if (!testHooksEnabled) {
                testHooksPermanentlyDisabled.set(true);
            }

            /* We must set communicator before creating the acceptor, as the
               acceptor assumes that the value will be set correctly for its
               entire lifetime */
            if (!secondary) {
                mainCommunicator = this;
            }
            myEndpoint.informOfCommunicatorStart(this);

            /* As long as the acceptor isn't running, we can unlock the locks
               in any order; we have other locks meaning that nothing will be
               looking at these ones while we're busy starting the
               communicator. */
            busy.values().stream().forEach((l) -> l.writeLock().unlock());

            acceptor = new ConnectionManager(this);
            acceptor.startListenLoop();

            executorService = newSingleThreadScheduledExecutor();

            BackgroundGarbageCollection.startKeepalive(this);
        }
    }

    /**
     * Checks whether this communicator is currently running.
     *
     * @return <code>true</code> if the communicator is running.
     */
    public synchronized boolean isRunning() {
        return acceptor != null;
    }

    /**
     * Stops the communicator's ability to communicate.
     * <p>
     * Will not return (or stop the communicator) until the communicator is
     * idle. At present, this method does not put much effort into to trying to
     * <i>make</i> the communicator idle (e.g. migrating away objects, or
     * refusing to accept new method calls). This may change in the future.
     * <p>
     * Must only be called from a thread that is "outside" the communicator,
     * i.e. has had no contact with it. (This may be a thread that was
     * <i>created</i> by a method call via the communicator, so long as it was
     * not created by the communicator itself, e.g. a <code>Thread.start</code>
     * call via the communicator can create a thread to shut the communicator
     * down.) Note that shutting down a communicator causes it to lose its
     * identity; when restarted, it will be a fresh start, with no knowledge of
     * its previous time running.
     *
     * @throws IllegalStateException If the communicator is already stopped, or
     * already being stopped, or something is locally allocated that makes it
     * impossible to stop
     * @see Thread#start()
     */
    @SuppressWarnings("UseSpecificCatch")
    public void stopCommunication() throws IllegalStateException {
        int stageReached = 0;
        try {
            for (; stageReached < ShutdownStage.values().length;
                    stageReached++) {
                ShutdownStage stage = ShutdownStage.values()[stageReached];
                int failCount = 0;
                Lock lock = busy.get(stage).writeLock();
                if (!lock.tryLock()) {
                    stage.tryToMakePossible(this);
                    while (!delayInterruptionsRv(()
                            -> lock.tryLock(100, TimeUnit.MILLISECONDS))) {
                        stage.tryToMakePossible(this);
                        failCount++;
                        if (failCount % 30 == 0) {
                            sendWarning("stopCommunication() is blocked a "
                                    + "suspiciously long time; on shutdown stage "
                                    + stage + ", lock status: " + busy.get(stage));
                        }
                        if (failCount >= 119) {
                            throw new IllegalStateException(
                                    "could not shut down communicator (shutdown failed at stage "
                                    + stage + "), it may still be in use");
                        }
                    }
                }
            }
        } catch (Throwable ex) {
            /* This is basically a finally block, except that it doesn't run on
               successfull termination. The basic idea's to unlock all the
               write locks we locked, in reverse order, to get back into a
               stable state. */
            for (stageReached--; stageReached > 0; stageReached--) {
                ShutdownStage stage = ShutdownStage.values()[stageReached];
                Lock lock = busy.get(stage).writeLock();
                lock.unlock();
            }
            throw ex;
        }

        /* Clear the map holding the lifetime managers, because it's designed
           to be cleared when not in use. (It must be empty by this point
           anyway; a lifetime manager needs to be associated with a location
           manager, and none of those can exist at this point.) */
        allLifetimeManagers.clear();

        BackgroundGarbageCollection.endKeepalive(this);

        /* Make sure that we aren't holding any Java threads alive; we no longer
           need to leave them around to prevent thread creation overhead in our
           future thread use because there won't be any, and they may be
           preventing VM shutdown. */
        AccessController.doPrivileged(
                (PrivilegedAction<Void>) () -> {
                    PooledThread.rotateThreadPool();
                    executorService.shutdown();
                    return null;
                });
        /* note: don't wait for the executor to actually <i>finish</i>; it may
           be shutting us down, and if it actually needed our resources it would
           have been holding the keepalive lock */
        executorService = null;

        synchronized (DistributedCommunicator.class) {
            myEndpoint.informOfCommunicatorStop(this);

            acceptor = null;
            if (!secondary) {
                mainCommunicator = null;
            }
        }
    }

    /**
     * Discards thread-local state that's being held for threads on other
     * communicators.
     * <p>
     * When a remote communicator runs a method locally on this communicator,
     * the thread that it used needs to be held alive in case it set a
     * thread-local variable (and the thread still exists on that communicator,
     * so that it can subsequently retrieve the value it set). In most cases,
     * the threads will be held alive to no purpose, because thread-local
     * variables are rarely used. In this case, this method can be used to
     * discard the unnecessary threads.
     * <p>
     * Residual thread-local state is automatically dropped when the
     * communicator shuts down.
     */
    void dropResidualThreadLocalState() {
        activeThreadIDs.forEach((id, ati) -> {
            if (id.equals(GlobalID.getCurrentThreadID(getMyAddress()))) {
                sendWarning("Attempting to shut down a communicator via "
                        + "thread " + id + ", which has had contact with it");
            } else {
                MessageEnvelope envelope = new MessageEnvelope(
                        new MessageAddress(id, true, getMyAddress(),
                                false, true),
                        new ActiveThreadInfo.LocalThreadShutdownMessage(),
                        this, getMyAddress());
                /* note: ignore return value; if the attempt fails then the
                   thread is already shutting down or has already shut down, so
                   we don't need to revive it merely to tell it to shut down a
                   second time */
                sendEnvelopeToActiveThread(id, envelope);
            }
        });
    }

    /**
     * Eventually stops the communicator's ability to communicate, in the
     * background. Unlike <code>stopCommunication</code>, this may be called
     * from any thread, as it stops the communication in the background on a
     * separate thread. Also unlike <code>stopCommunication</code>, it does not
     * wait for the communication to stop (which can take a long time waiting
     * for other communicators to report that they've released references to
     * objects stored on this communicator); rather, it returns an object that
     * can be used to determine when the stopping is complete.
     *
     * @return A <code>Future</code> that represents the background process of
     * stopping the communication, and can be used to determine when
     * communication has stopped.
     */
    public Future<?> asyncStopCommunication() {
        return getExecutorService().submit(this::stopCommunication);
    }

    /**
     * Returns an executor service whose lifetime is tied to that of the
     * communicator.
     *
     * @return An executor service that can execute things for as long as the
     * communicator is running.
     * @throws IllegalStateException If the communicator is currently stopped
     */
    ScheduledExecutorService getExecutorService() throws IllegalStateException {
        if (executorService == null) {
            throw new IllegalStateException(
                    "Requested an executor service from an inactive communicator");
        }
        return executorService;
    }

    /**
     * Sends a distributed message to the given machine, synchronously. The
     * message will be placed into a default envelope that requests that the
     * message will be run on, and send a reply to, the global extension of the
     * current thread. When the message's execution completes, it will send a
     * reply, causing this function to return the resulting value or throw the
     * resulting exception.
     *
     * @param <T> The expected type of the reply.
     * @param message The message to send. This must be a synchronous message
     * (one which requires a reply).
     * @param address The communication address of the system to send it to.
     * @return The return value of the message. This will automatically be
     * unmarshalled, if necessary.
     * @throws Throwable If an exception occurs on the remote machine while
     * handling a synchronous message
     * @throws DistributedError If an exception occurs in communication between
     * this system and another system
     * @throws AutocloseableLockWrapper.CannotLockException If the
     * message-sending infrastructure has been shut down
     * @throws IllegalArgumentException If the given distributed message is
     * intended for a specific thread other than the currently executing thread,
     * or is asynchronous
     */
    <T> T sendMessageSync(SynchronousMessage<T> message,
            CommunicationAddress address)
            throws DistributedError, IllegalArgumentException,
            AutocloseableLockWrapper.CannotLockException, Throwable {
        GlobalID myThreadID = getCurrentThreadID(getMyAddress());

        try (DeterministicAutocloseable holdBusy
                = maybeGetKeepaliveLock(ShutdownStage.MESSAGE, "sendMessageSync")) {
            try (DeterministicAutocloseable keepThreadActive
                    = labelThreadActive(false, null)) {
                Stopwatch timer = new Stopwatch(Lazy.TIME_BASE.get()).start();
                MessageEnvelope envelope = new MessageEnvelope(
                        new MessageAddress(getMyAddress(),
                                message instanceof SynchronousMessage.BorrowOnly,
                                message.isUnimportant()), message,
                        this, address);
                if (debugMonitor != null) {
                    debugMonitor.newMessage(
                            envelope.asMessageInfo(this,
                                    timer.time(ChronoUnit.NANOS),
                                    message, address));
                }
                sendToAddress(address, envelope);

                /* Due to type erasure, we might not necessarily know what T is,
                   so the best we can do is an unchecked cast. (The remote end
                   of the connection should have verified that the type is a
                   desired one, although obviously you can't prove that over the
                   network.) */
                @SuppressWarnings("unchecked")
                T rv = (T) receiveMessagesToThread(
                        myThreadID, address, null, false);
                return rv;
            }
        }
    }

    /**
     * Sends an asynchronous distributed message to the given machine. Because
     * the message is asynchronous, this method will return without waiting for
     * the message to be handled.
     *
     * @param message The message to send.
     * @param threadID The thread that will process the message; this could be
     * an existing thread on the target system, a new thread (a fresh GlobalID),
     * or <code>null</code> to process the message directly on the communication
     * thread of the recipient (only suitable for lightweight-safe messages).
     * @param address The communication address of the system to send it to.
     * @throws DistributedError If an exception occurs in communication between
     * this system and another system
     * @throws IllegalArgumentException If the given distributed message is not
     * asynchronous
     * @throws AutocloseableLockWrapper.CannotLockException If the communicator
     * has shut down past the point at which it sends messages
     */
    void sendMessageAsync(DistributedMessage message, GlobalID threadID,
            CommunicationAddress address)
            throws DistributedError, IllegalArgumentException,
            AutocloseableLockWrapper.CannotLockException {
        /* Note: we might not be able to get the lock if we're sending something
           like a background GC message to a communicator, trying to get it to
           exit, and it's already exited naturally; the caller will have to be
           able to deal with this case */
        try (DeterministicAutocloseable holdBusy
                = maybeGetKeepaliveLock(ShutdownStage.MESSAGE,
                        "sendMessageAsync")) {
            Stopwatch timer = new Stopwatch(Lazy.TIME_BASE.get()).start();
            MessageEnvelope envelope = new MessageEnvelope(
                    new MessageAddress(threadID, false, getMyAddress(), false,
                            message.isUnimportant()),
                    message, this, address);
            if (debugMonitor != null) {
                debugMonitor.newMessage(
                        envelope.asMessageInfo(this,
                                timer.time(ChronoUnit.NANOS), message, address));
            }
            sendToAddress(address, envelope);
        }
    }

    /**
     * Send the given message envelope to the system at the given address. This
     * is a very low-level operation that does not handle waiting for replies,
     * locking the communicator, etc., and should thus only be called from
     * `sendMessageSync` and `sendMessageAsync`.
     *
     * @param address The address of the virtual machine to send the message to.
     * @param envelope The message envelope to send.
     * @throws DistributedError If something went wrong sending the message
     */
    private void sendToAddress(
            CommunicationAddress address, MessageEnvelope envelope)
            throws DistributedError {
        try {
            acceptor.sendMessageTo(envelope, address);
        } catch (IOException | CommunicationEndpoint.IncompatibleEndpointException ex) {
            if (envelope.getAddress().isUnimportant()) {
                /* If the message is unimportant, and we can't send it, it's
                   very likely that the recipient has shut down and doesn't care
                   about it. Being unimportant gives us permission to drop it
                   without an exception in this case. */
                // TODO: If we see the same error on multiple attempts, that
                // indicates a larger problem that we should report; it means
                // that the connection dropped, not that the other side has
                // already shut down.
                return;
            }
            throw new DistributedError(ex, "sendToAddress");
        }
    }

    /**
     * Attempt to determine the communication address of the system running on
     * the given address and port. This is a very slow operation, which can
     * involve network access, so it's best to cache the result.
     * <p>
     * Note that as long as the remote system continues to use the same keys to
     * authenticate itself, the address will stay the same. As such, you can,
     * e.g., serialise the resulting communication address to disk and use it in
     * the future to ensure that you're always communicating with the same
     * system.
     *
     * @param address The IP address on which the remote system is listening for
     * inbound communications.
     * @param port The port on which the remote system is listening for inbound
     * communications.
     * @return A communication address that can be used to communicate with that
     * system.
     * @throws IOException If the system in question could not be contacted to
     * ask it its address, or something else went wrong (e.g. the system in
     * question could not identify itself correctly)
     * @throws UnsupportedOperationException If this communicator's endpoint is
     * not of a type that's capable of making outbound network connections
     */
    public CommunicationAddress lookupAddress(
            InetAddress address, int port) throws IOException,
            UnsupportedOperationException {
        IdentityMessage message = new IdentityMessage();
        GlobalID myThreadID = getCurrentThreadID(getMyAddress());
        Communicable recipient = new TCPCommunicable() {
            @Override
            public InetAddress asInetAddress() {
                return address;
            }

            @Override
            public int getTransmissionPort() {
                return port;
            }
        };

        try (SocketLike socket = myEndpoint.newConnection(recipient)) {
            OutputStream os = socket.getOutputStream();
            os.write(ConnectionManager.ADDRESS_LOOKUP_CODE);
            os.flush();
            FakeDescriptionStream fds = new FakeDescriptionStream(
                    socket.getInputStream());
            CommunicationAddress commAddress
                    = (CommunicationAddress) Marshalling.rCAOStatic(fds, null, null);
            if (commAddress.equals(getMyAddress())) {
                throw new javax.net.ssl.SSLKeyException(
                        "Server at " + address + ":" + port
                        + " is claiming to be this client "
                        + "(is it using the same .p12 file?)");
            }
            return commAddress;
        } catch (CommunicationEndpoint.IncompatibleEndpointException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    /**
     * This method is used to handle the behaviour of local threads which are
     * part of a distributed thread that's currently running on a different
     * machine. The method will not return until an OperationCompleteMessage is
     * received; when it is, the message will determine the return value. In the
     * meantime, other sorts of messages may be sent to the thread; it will wake
     * up to process the message, and then go back to being blocked afterwards.
     *
     * @param activeThread The local thread that's executing remotely.
     * @param recipientListener Can be <code>null</code>. If it isn't, this will
     * be called with the communication address that the final reply was sent
     * from. Used in cases where the sender didn't originally know the
     * recipient's address.
     * @param threadIsRunningOn A remote location that is executing a higher-up
     * portion of this thread. Can be <code>null</code> in cases where this is
     * not known.
     * @param residualStateOnly Whether this message-receiving loop only exists
     * to hold residual thread-local state alive in case a remote system needs
     * it. (If the thread only exists for its residual state, it can be assumed
     * that if the user calls <code>stopCommunication()</code> on either system,
     * the state is no longer required and thus there's no need to block waiting
     * for the thread to exit.)
     * @return The return value received via an OperationCompleteMessage.
     * @throws Throwable If an OperationCompleteMessage specifies that while
     * running remotely, the thread encountered an exception, this method throws
     * the same exception
     */
    private Object receiveMessagesToThread(GlobalID activeThread,
            CommunicationAddress threadIsRunningOn,
            Consumer<CommunicationAddress> recipientListener,
            boolean residualStateOnly)
            throws Throwable {
        ActiveThreadInfo threadInfo = activeThreadIDs.get(activeThread);
        Thread currentThread = currentThread();
        Thread interruption = null;
        boolean delayedInterruption = false;
        while (true) {
            /* If we've only been called to handle residual state, but that
               residual state isn't needed, exit. residualStateNecessary can
               only be set by messages, so we check it once at the start of the
               loop (in case it was set already), then again after each message
               has been processed. */
            if (residualStateOnly && !threadInfo.residualStateNecessary) {
                return new ThreadProjectionTracker.ThreadHasEnded();
            }

            MessageEnvelope followup = null;
            try {
                /* Get the return value via the queue. (We take the
                   entire message from the queue, not just the return
                   value directly, in case there was an exception; that
                   causes the exception to be thrown on the correct
                   thread. Likewise, we don't try to process the message
                   until <i>after</i> the try/catch has finished, so
                   that we can correctly propagate an
                   InterruptedException between machines. */
                followup = threadInfo.messageQueue.take();
            } catch (InterruptedException ex) {
                /* If we get an InterruptedException, then some thread
                   is trying to interrupt activeThread, but it's
                   executing on a different machine. Attempt to interrupt it
                   there.

                   Note that it's possible for the thread to return from the
                   other machine while this machine is busy asking that machine
                   to interrupt it. In this case, either we'll interrupt the
                   thread lower down (in which case it'll send an interrupt
                   message right back), or it won't know what thread we're
                   talking about (in which case it'll tell us that we're going
                   to have the thread running here very soon, and to try
                   again). */
                if (interruption != null) {
                    try {
                        interruption.join();
                    } catch (InterruptedException ex1) {
                        /* Under Java semantics, double-interrupting a thread
                           could be the same as interrupting it only once. We're
                           about to implement one interruption; we received a
                           second one, so we can safely ignore it. */
                    }
                    interruption = null;
                }
                if (threadIsRunningOn != null) {
                    interruption = new Thread(() -> {
                        try {
                            if (!sendMessageSync(
                                    new InterruptMessage(activeThread),
                                    threadIsRunningOn)) {
                                currentThread.interrupt();
                            }
                        } catch (Throwable ex1) {
                            asyncExceptionHandler(ex1);
                        }
                    });
                } else {
                    delayedInterruption = true;
                }
            }
            if (followup != null) {
                if (followup.getAddress().sendsReply()) {
                    /* The common case: we got a followup message. Run the code
                       it wants us to execute. (If there's an exception, wrap it
                       in a DistributedError, because exceptions that are meant
                       to be propagated should be propagated via run() itself,
                       and thus most likely there was some sort of internal
                       error. Don't wrap errors; in particular, we don't want to
                       re-wrap a DistributedError that occurs during the
                       followup. Note that all these exceptional cases should
                       only happen if there's something wrong in our code; they
                       shouldn't be triggerable by the user.) */
                    try {
                        followup.processMessage(debugMonitor, this, false);
                    } catch (Exception ex) {
                        throw new DistributedError(ex,
                                "processing received message envelope");
                    }
                } else {
                    /* If we got an <i>asynchronous</i> message as a
                       reply, it must be an OperationCompleteMessage.

                       This is a special case that requires us to
                       unwind the stack (to prevent it overflowing
                       indefinitely), but luckily, doing so is as
                       simple as extracting and returning the message's
                       return value. */
                    if (recipientListener != null) {
                        recipientListener.accept(followup.getAddress()
                                .getSenderAddress());
                    }
                    if (interruption != null || delayedInterruption) {
                        /* Make sure any attempt to interrupt this thread has
                           finished before returning. */
                        delayInterruptions(interruption::join);
                    }
                    return followup.getOCMReturnValue(debugMonitor, this);
                }
            }
        }
    }

    /**
     * Returns the best available keepalive lock for a new thread projection.
     * This will be a "remnant thread state" lock if there's a possibility of
     * remnant thread-local state <i>and</i> we're not shutting down (or are
     * early enough in the shutdown process that we haven't discarded such state
     * yet), and a "message" lock otherwise.
     *
     * @param tid The thread ID of the new thread projection.
     * @param envelope The message envelope in which the request to create the
     * projection was sent
     * @param needsTracking An out parameter, set to <code>false</code> if the
     * remnant thread state can be discarded, <code>true</code> if it needs to
     * be kept around, calculated based on the envelope and shutdown state
     * @return A keepalive lock (which must be autoclosed at some point to drop
     * the lock, it doesn't have a finalizer).
     * @throws AutocloseableLockWrapper.CannotLockException If the shutdown
     * state has moved past the point at which message processing is possible
     */
    private DeterministicAutocloseable bestLockForThreadProjection(
            GlobalID tid, MessageEnvelope envelope, Holder<Boolean> needsTracking)
            throws AutocloseableLockWrapper.CannotLockException {
        if (envelope.getAddress().sendsReply()) {
            try {
                needsTracking.accept(true);
                return maybeGetKeepaliveLock(ShutdownStage.THREAD_PROJECTION,
                        "handleArrivingMessage:" + tid);
            } catch (AutocloseableLockWrapper.CannotLockException ex) {
                /* move on to the next attempt, below */
            }
        }
        needsTracking.accept(false);
        return maybeGetKeepaliveLock(ShutdownStage.MESSAGE,
                "handleArrivingMessage:" + tid);
    }

    /**
     * Decode and process an arriving message. This is intended to be called via
     * connection threads once they receive a message from another system. In
     * most cases, the thread calling this method will decipher the message only
     * as far as the outside of the envelope; that lists a thread to do the rest
     * of the deciphering on, and the message will be passed to that thread in
     * order to do the rest of the operation asynchronously.
     * <p>
     * In the case of a lightweight message &ndash; one which specifically
     * states that important threads can be blocked to decode and process it
     * because both the decode and process are fast and avoid time-consuming or
     * blocking operations &ndash; the message will be decoded and processed on
     * the current thread before this function returns.
     *
     * @param envelope The arriving message envelope (i.e. undecoded message).
     */
    void handleArrivingMessage(MessageEnvelope envelope) {
        GlobalID tid = envelope.getAddress().getThreadID();
        if (tid == null) {
            /* Lightweight messages are those which are guaranteed to be safe to
               deserialise and process on the spot, on any thread (even one
               that's currently being used for something important). */
            envelope.processMessage(debugMonitor, this, false);
            return;
        }

        /* Can we simply handle it on an existing thread? */
        if (sendEnvelopeToActiveThread(tid, envelope)) {
            return;
        }

        /* Can we just drop it entirely? */
        if (envelope.getAddress().isUnimportant()) {
            return;
        }

        /* The thread is not currently running on this machine, or else its
           projection onto this machine is shutting down. Create a new thread to
           serve as the projection onto this machine. While the thread exists,
           it holds the communicator busy. */
        Holder<Boolean> needsTracking = new Holder<>();
        new PooledThread(() -> {
            try (final DeterministicAutocloseable holdBusy
                    = bestLockForThreadProjection(tid, envelope, needsTracking)) {
                setCurrentThreadID(tid);
                try (DeterministicAutocloseable keepThreadActive
                        = labelThreadActive(true,
                                envelope.getAddress().getSenderAddress())) {
                    ActiveThreadInfo newInfo = activeThreadIDs.get(tid);
                    envelope.processMessage(
                            debugMonitor, this, needsTracking.getValue());

                    /* Note that we don't track thread shutdown if it's
                       asynchronous, i.e. the caller shouldn't use a thread ID
                       that's visible to the user in this case as it won't know
                       when it exits. That's fundamental in the behaviour of
                       asynchronous threads, though. */
                    if (!needsTracking.getValue()) {
                        return;
                    }

                    /* We may have thread-local state. Wait until the caller
                       lets us know it's OK to release that (and handle any
                       further messages that may arrive for the thread
                       meanwhile). */
                    try {
                        Object o = receiveMessagesToThread(
                                tid, null, null, true);
                        if (!(o instanceof ThreadProjectionTracker.ThreadHasEnded)) {
                            throw new DistributedError(new IllegalStateException(
                                    "unexpectedly received an object of "
                                    + o.getClass()),
                                    "waiting for thread termination");
                        }
                    } catch (Throwable ex) {
                        throw new DistributedError(
                                ex, "waiting for thread termination");
                    }
                }
            } catch (AutocloseableLockWrapper.CannotLockException ex) {
                // TODO: Tell the sender of the message that we've shut down
                // and can't handle it
                sendWarning(
                        "Received a message while shutting down, cannot handle it");
            }
        }).start(this);
    }

    /**
     * Attempts to send an envelope to an active thread. If the thread is
     * shutting down, or has no active thread info, or doesn't exist, the
     * attempt will fail; if the caller still cares about delivering the
     * message, it'll have to fix those problems themselves.
     *
     * @param tid The thread ID to which to send the message.
     * @param envelope The envelope to send.
     * @return <code>true</code> if the message was sent; <code>false</code> if
     * the message could not be sent.
     */
    private boolean sendEnvelopeToActiveThread(
            GlobalID tid, MessageEnvelope envelope) {
        /* Attempt to pass the thread the message, if it's currently running on
           the machine and not shutting down. */
        ActiveThreadInfo info = activeThreadIDs.get(tid);
        if (info != null) {
            boolean firstIteration = true;
            while (true) {
                /* Note: we can't hold info's monitor while sending the message;
                   if nothing's reading the other end, it may be attempting to
                   set info into shutting-down mode, in which case we need to
                   drop the monitor to let it do that */
                synchronized (info) {
                    if (info.shuttingDown) {
                        break;
                    }
                }
                if (!firstIteration) {
                    /* 1 second should surely be enough to get from the end
                       of SynchronousMessage#process back to the take() in
                       receiveMessagesToThread - hardly any code - so
                       presumably something else went wrong, such as the
                       thread not running despite being listed in
                       activeThreadIDs. */
                    sendWarning("Suspicious delay in delivering message: "
                            + envelope);
                }
                firstIteration = false;
                boolean received;
                received = delayInterruptionsRv(()
                        -> info.messageQueue.offer(envelope, 1, SECONDS));
                if (received) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Attempts to interrupt the global thread with a given global ID. The
     * attempt in question will only succeed if the global thread has a
     * projection onto the current virtual machine, and (to avoid race
     * conditions) if that portion of the thread is not currently in the process
     * of returning.
     *
     * @param threadID The global ID of the thread to interrupt.
     * @return <code>true</code> if the thread was interrupted;
     * <code>false</code> if the thread is not running here, or is so late in
     * its life that it could not be interrupted without risking a race
     * condition.
     */
    boolean interruptThreadById(GlobalID threadID) {
        ActiveThreadInfo info = activeThreadIDs.get(threadID);

        if (info == null) {
            return false;
        }
        synchronized (info) {
            if (info.shuttingDown) {
                return false;
            }
            info.threadProjection.interrupt();
            return true;
        }
    }

    /**
     * Returns the communication address used by this distributed communicator.
     * This address can be passed to another system involved in the distributed
     * computation, to enable that system to send messages to this communicator.
     *
     * @return The communication address being used by this communicator,
     */
    public CommunicationAddress getMyAddress() {
        return myEndpoint.getAddress();
    }

    /**
     * Returns the communication endpoint used by this distributed communicator.
     * This would typically be used when creating sockets that listen to inbound
     * connections.
     *
     * @return The communication endpoint being used by this communicator,
     */
    CommunicationEndpoint getEndpoint() {
        return myEndpoint;
    }

    /**
     * Returns the connection manager that accepts connections for this
     * communicator.
     *
     * @return The connection manager.
     */
    ConnectionManager getAcceptor() {
        return acceptor;
    }

    /**
     * Runs the specified code on the specified machine, and returns its return
     * value. The code may not throw checked exceptions, but may throw unchecked
     * exceptions.
     * <p>
     * Note that the object <code>code</code> that's used to specify the code
     * must be effectively immutable, because the other machine will be working
     * with a copy of the object (it couldn't work with the original, because
     * then the code would have to run on the local machine, which defeats the
     * point of running it remotely).
     *
     * @param <T> The type of the returned value.
     * @param code The code to run.
     * @param onMachine The machine on which to run the function.
     * @return The value supplied by the code being run, if any.
     */
    public <T> T runRemotely(CopiableSupplier<T> code,
            CommunicationAddress onMachine) {
        if (onMachine.equals(getMyAddress())) {
            /* a trivial special case */
            return code.get();
        }

        MethodMessage mm;
        try {
            mm = new MethodMessage(
                    CopiableSupplier.class.getMethod("get"), code);
        } catch (NoSuchMethodException ex) {
            /* Should never happen. */
            throw new RuntimeException(ex);
        }

        try {
            /* We use an unchecked cast here because otherwise the caller would
               have to specify a type, complicating the API. */
            @SuppressWarnings("unchecked")
            T rv = (T) sendMessageSync(mm, onMachine);
            return rv;
        } catch (RuntimeException | Error ex) {
            /* Unchecked exceptions/errors; we can throw these directly. */
            throw ex;
        } catch (Throwable ex) {
            /* We got a checked throwable even though we really shouldn't.
               Wrap it in an UndeclaredThrowableException. */
            throw new java.lang.reflect.UndeclaredThrowableException(ex);
        }
    }

    /**
     * An overloaded version of {@link DistributedCommunicator#runRemotely(CopiableSupplier,
     * CommunicationAddress)}. The given Runnable is converted to a Supplier
     * that returns null, then the overloaded method is called with this
     * Supplier.
     *
     * @param code The code to run.
     * @param onMachine The machine on which to run the function.
     * @see DistributedCommunicator#runRemotely(CopiableSupplier,
     * CommunicationAddress)
     */
    public void runRemotely(CopiableRunnable code,
            CommunicationAddress onMachine) {
        runRemotely(() -> {
            code.run();
            return null;
        }, onMachine);
    }

    /**
     * Adds a "keepalive lock" to this distributed communicator, an object that
     * prevents the communicator from shutting down until that object is
     * autoclosed. Used to ensure that the communicator will not be shut down
     * while it's still in use for, e.g., storing objects migrated from another
     * system; and used to determine what parts of shutdown still need to be
     * run.
     * <p>
     * In order to prevent the communicator being left permanently unable to
     * shut down, this object must be autoclosed at some point before it's
     * disposed of. (It does not have a finalizer.)
     * <p>
     * This method should only be called while communication is actually active
     * (it doesn't make sense to hold the communicator as being in use if it
     * isn't even active at the moment). If it's called while the communicator
     * is stopped, the method will block until the communicator is started.
     * <p>
     * Unlike a typical Lock, this is not attached to any particular thread; you
     * can create it on one thread, and remove it on another. This also means
     * that attempting to shut down the communicator from the thread that called
     * <code>getKeepaliveLock</code> not automatically illegal (because it is
     * not necessarily a deadlock).
     * <p>
     * This method can succeed during earlier stages of shutdown, but will start
     * failing as soon as an attempt is made to take a keepalive lock for
     * something that should have shut down already.
     *
     * @param stage The shutdown stage in which this keepalive should be
     * eliminated.
     * @param reason The reason why the lock is being locked, for debugging
     * purposes.
     * @return A keepalive lock corresponding to this communicator.
     * @throws AutocloseableLockWrapper.CannotLockException If the communicator
     * is shut down, or in the later stages of shutting down.
     * @see #getKeepaliveLock(java.lang.Object)
     */
    DeterministicAutocloseable maybeGetKeepaliveLock(
            ShutdownStage stage, Object reason)
            throws AutocloseableLockWrapper.CannotLockException {
        return new AutocloseableLockWrapper(
                busy.get(stage).readLock(), false, reason);
    }

    /**
     * Gets the map of all lifetime managers. This can be modified by the caller
     * to keep it up to date, or simply used to locate lifetime managers; note
     * that this is the actual map itself, not a copy!
     *
     * @return The map of all lifetime managers.
     */
    ExpirableMap<ObjectLocation, LifetimeManager> getAllLifetimeManagers() {
        return allLifetimeManagers;
    }

    /**
     * Gets the set of all thread projection trackers. This would typically be
     * used by <code>ShutdownStage</code> to shut them down in bulk. Note that
     * this is the actual map itself, not a copy!
     *
     * @return The map of all thread projection trackers.
     */
    WeakConcurrentSet<ThreadProjectionTracker> getAllProjectionTrackers() {
        return allProjectionTrackers;
    }

    /**
     * Creates an entry in the active thread IDs table for the current thread.
     * Returns an <code>Autocloseable</code> that will remove the entry again
     * when required. (Note that the autoclosing should occur on the same thread
     * as the original labelling-as-active.)
     *
     * @param force If <code>true</code>, create the entry unconditionally,
     * overwriting any previous entry. If <code>false</code>, create the entry
     * only if there is no current entry.
     * @param projectedFrom The address of the communicator immediately below
     * this one on the thread's stack. If <code>force</code> is false, this
     * needs to be accurate only in the case when there is no current entry
     * (which in turn means that <code>null</code> is likely to be the correct
     * value).
     * @return An <code>Autocloseable</code> that will remove the created entry
     * (if any) from the active thread IDs table.
     */
    private DeterministicAutocloseable labelThreadActive(
            boolean force, CommunicationAddress projectedFrom) {
        final GlobalID activeThread = getCurrentThreadID(getMyAddress());

        if (activeThreadIDs.containsKey(activeThread) && !force) {
            /* The open is a no-op in this case; thus so is the close. */
            return () -> {
            };
        }

        /* It's vital that activeThreadIDs operations are correctly paired:
           in particular, we need to create a blocking queue for the thread
           before a synchronous call (unless it's a nested call and the blocking
           queue already exists), and if we create it, we need to ensure it's
           destroyed exactly once, when the synchronous operation ends or fails.
           Thus, we create an <code>Autocloseable</code> so that the caller can
           use a try-with-resources block to guarantee it gets closed at the
           right time. */
        ActiveThreadInfo ati = new ActiveThreadInfo(
                currentThread(), projectedFrom);
        activeThreadIDs.put(activeThread, ati);

        return () -> {
            synchronized (ati) {
                ati.shuttingDown = true;
            }
            activeThreadIDs.remove(activeThread, ati);
        };
    }

    /**
     * Returns the set of all local objects that are referenced from a remote
     * system. This operation works via checking the garbage collection metadata
     * for the objects in question.
     * <p>
     * The result will be instantaneously valid, but may immediately become
     * outdated due to changes to long references made in different threads. As
     * such, this should probably only be used for debugging purposes.
     * <p>
     * If an object is in the process of being migrated, it's possible that the
     * return values will contain long references to an object on a remote
     * system. (This implies that remote systems believe the object in question
     * is still on this system, but are incorrect.)
     *
     * @return A set of pairs consisting of an object, and a system referencing
     * it. (If an object is referenced by multiple remote systems, it may be
     * returned multiple times.)
     */
    public Set<Pair<CommunicationAddress, Object>> listAllRemotelyReferencedLocalObjects() {
        HashSet<Pair<CommunicationAddress, Object>> rrlo = new HashSet<>();
        allLifetimeManagers.forEachKey((l) -> {
            allLifetimeManagers.runMethodOn(l, (lm) -> {
                rrlo.add(new Pair<>(l.getLocatedVia(),
                        lm.getLocationManager().getBestReference()));
            }, null);
        });
        return rrlo;
    }

    /**
     * Finds/creates the standin for a given object. This method will ensure
     * that only one standin at a time is created for any given object; however,
     * if the standin becomes unreferenced, it will be deallocated and a new
     * standin may be created to replace it.
     * <p>
     * The object in question must be noncopiable (i.e. use
     * <code>NonCopiableKnowledge</code> as its class knowledge).
     *
     * @param <T> The actual class of <code>o</code>.
     * @param o The object to return the standin for.
     * @param externalReferencesExist Whether it's possible that (once a standin
     * is created) references will be made to the object that don't go via the
     * standin. Normally this will be <code>true</code>. Ignored if no standin
     * needs to be created.
     * @return <code>o</code>, if <code>o</code> is a standin; or the previously
     * returned standin for <code>o</code>, if it still exists; or a freshly
     * created composition-based standin which has <code>o</code> as its
     * referent.
     * @throws IllegalArgumentException If <code>o</code> is not an (implicitly
     * or explicitly) noncopiable object
     */
    @SuppressWarnings("unchecked")
    <T> Standin<T> findStandinForObject(T o, boolean externalReferencesExist)
            throws IllegalArgumentException {
        /* Technically speaking, the semantics of this method is not typesafe,
           as there's no way to know that <code>o</code> is a <code>T</code>
           rather than a subclass of a <code>T</code>. So just use an "I know
           what I'm doing" type erasure cast; it's the caller's fault if things
           go wrong. */
        if (o instanceof Standin) {
            return (Standin) o;
        }
        Standin<T> wrapper = (Standin) wrapperStandins.get(o);
        if (wrapper != null) {
            return wrapper;
        }

        ClassKnowledge<T> objectKnowledge
                = (ClassKnowledge) knowledgeForActualClass(o);
        if (!(objectKnowledge instanceof NonCopiableKnowledge)) {
            throw new IllegalArgumentException(
                    "Attempted to create a standin wrapper for "
                    + "a non-noncopiable object of " + o.getClass());
        }

        wrapper = objectKnowledge.getStandinFactory(STANDIN_WRAPPER).
                castAndWrapObject(o);
        Standin<T> asyncAddedWrapper = (Standin) wrapperStandins.putIfAbsent(
                o, wrapper);
        if (asyncAddedWrapper == null) {
            if (externalReferencesExist) {
                nonofflinableStandins.add(wrapper);
            }

            return wrapper;
        }
        /* Looks like a parallel call to this method created one already. */
        return asyncAddedWrapper;
    }

    /**
     * Creates a set of migration actions relating to the specified object. The
     * object can be a local object or remote reference; can be given as the
     * object itself (i.e. a short reference) or as a long reference; and may or
     * may not have come into contact with the migration or distributed
     * communication systems in the past. However, note that not all objects can
     * be migrated (in particular, an object cannot safely be migrated unless
     * the distributed communication system knows about all references to it).
     * <p>
     * The created <code>MigrationActions</code> object itself has only a weak
     * reference to the original object, and will not prevent the object from
     * being deallocated if no more references to it (on any system involved in
     * the distributed communication) remain. There are also no concurrency
     * issues involved with <i>creating</i> the <code>MigrationActions</code>
     * object (although calls to its methods may block while waiting for other
     * uses of the object to end).
     * <p>
     * As an object cannot be safely migrated unless the communicator is aware
     * of all references to it, the resulting migration actions object is likely
     * to be unusable if given an arbitrary object to operate on, as all the
     * operations will throw exceptions due to an inability to determine whether
     * migrating the object is safe. Objects that have been specifically created
     * as migratable (e.g. via <code>createMigratably</code>) will not have this
     * problem.
     *
     * @param <T> The actual type of the object on which the migration-related
     * actions will be performed.
     * @param t The object to perform migration-related actions on.
     * @return A set of methods for performing migration-related actions on that
     * object.
     * @throws IllegalStateException If <code>stopCommunication()</code> was
     * called earlier, and the process of stopping communication has already run
     * past the point at which <code>MigrationActions</code> objects would be
     * meaningful
     */
    public <T> MigrationActions<T> getMigrationActionsFor(T t)
            throws IllegalStateException {
        try {
            return new MigrationActions<>(findLocationManagerForObject(t));
        } catch (AutocloseableLockWrapper.CannotLockException ex) {
            throw new IllegalStateException(
                    "communicator is shutting down");
        }
    }

    /**
     * Finds the location manager corresponding to the given object. The object
     * can be given directly, or indirectly via a long reference. If the object
     * does not currently have a location manager, one will be created.
     * <p>
     * This can also be used to determine an object's ID (which is stored in its
     * location manager, and not anywhere else).
     *
     * @param <T> Type information that the caller wishes to transfer from
     * <code>o</code> onto the returned location manager. Can be left as
     * <code>Object</code> (or <code>?</code>) if no type bound on the location
     * manager is necessary.
     * @param o A reference (long or short) to the object whose manager is
     * desired.
     * @return The location manager for that object (possibly a newly created
     * one).
     * @throws AutocloseableLockWrapper.CannotLockException If the communicator
     * has shut down past the stage at which location tracking is possible
     * @throws IllegalArgumentException If <code>o</code> appears to be a long
     * reference, but one that was not constructed using a known technique.
     */
    <T> LocationManager<T> findLocationManagerForObject(T o)
            throws AutocloseableLockWrapper.CannotLockException {
        Standin<T> standin = findStandinForObject(o, true);

        /* A standin might or might not be a long reference, and might or
           might not already have a location manager. The standin storage is
           the canonical place for recording the location manager. Of course,
           that means we have to lock it against changes while we retrieve the
           location manager in question, in case the location manager gets
           deallocated while we're doing this.

           Incidentally, the "while (true) ... continue ..." here is the first
           piece of Java code I've seen that would actually be clearer with a
           goto. */
        while (true) {
            StandinStorage<T> oldStorage
                    = standin.getStorage(UNRESTRICTED);

            synchronized (oldStorage) {
                if (standin.getStorage(UNRESTRICTED)
                        != oldStorage) {
                    continue;
                }
                InvokeByCode<T> lm = oldStorage
                        .getMethodsForwardedTo(standin);
                if (lm != null) {
                    if (lm instanceof LocationManager) {
                        return (LocationManager<T>) lm;
                    } else {
                        throw new IllegalArgumentException(
                                "Unknown type of forwarding handler for " + o
                                + ": " + lm);
                    }
                }
            }

            LocationManager<T> lm = new LocationManager<>(standin,
                    o instanceof NonMigratable
                    || nonofflinableStandins.contains(standin), this);

            if (oldStorage instanceof TrivialStandinStorage) {
                /* Let the standin know that we're our location mananger. */
                standin.safeSetStorage(new LooseTightStandinReference.TightStorage<>(lm),
                        oldStorage, UNRESTRICTED);
                lm.loosen();
            } else {
                throw new IllegalArgumentException(
                        "Trying to create a location manager for a standin "
                        + "that has nontrivial storage");
            }

            return lm;
        }
    }

    /**
     * Finds the global thread object with the given ID. If the thread was
     * created on another communicator, this will return a long reference to the
     * <code>Thread</code> object on the communicator where it was created.
     * (Note that this may require network activity to create the long reference
     * on the desired machine.)
     *
     * @param threadID The thread ID to look up. Can be <code>null</code>, in
     * which case this message will use the ID of the current thread.
     * @return The discovered thread ID, or <code>null</code> if it can't be
     * found.
     */
    Thread findThreadWithID(GlobalID threadID) {
        final GlobalID finalThreadID
                = threadID == null ? getCurrentThreadID(getMyAddress())
                        : threadID;
        ActiveThreadInfo threadInfo = activeThreadIDs.get(finalThreadID);
        if (threadInfo == null) {
            if (getCurrentThreadID(getMyAddress()).equals(finalThreadID)) {
                return currentThread();
            } else {
                return null;
            }
        }

        if (threadInfo.projectedFrom == null) {
            return threadInfo.threadProjection;
        }

        return runRemotely(() -> getCommunicator()
                .findThreadWithID(finalThreadID), threadInfo.projectedFrom);
    }

    /**
     * Returns the <code>Thread</code> object for the currently executing
     * thread. This method abstracts the notion of a "thread" across systems,
     * i.e. if you call this method, then call it again remotely via
     * <code>runRemotely()</code>, both calls will return the same object.
     *
     * @return The currently executing thread.
     */
    static Thread getCurrentThread() {
        // TODO: What if it's starting or stopping?
        DistributedCommunicator communicator = getCommunicator();
        if (communicator == null) {
            return Thread.currentThread();
        }

        Thread rv = communicator.findThreadWithID(null);
        if (rv == null) {
            throw new DistributedError(new NullPointerException(),
                    "My global thread has gone missing");
        }
        return rv;
    }

    /**
     * Returns the thread projection tracker for the current thread. If it does
     * not exist yet, it will be created. Thread projection trackers are not
     * used while the communicator is shutting down (because the communicator is
     * assumed to not care about thread-local state elsewhere if it's exiting
     * itself).
     *
     * @return This thread's projection tracker, or <code>null</code> if the
     * communicator is in a shutdown sequence and thus no longer creating them.
     */
    ThreadProjectionTracker getTPTForCurrentThread() {
        ThreadProjectionTracker tpt = projectionTrackers.get();
        if (tpt == null) {
            try {
                tpt = new ThreadProjectionTracker(
                        getCurrentThreadID(getMyAddress()), this);
                projectionTrackers.set(tpt);
                allProjectionTrackers.add(tpt);
            } catch (AutocloseableLockWrapper.CannotLockException ex) {
                return null;
            }
        }
        return tpt;
    }

    /**
     * Information about an active thread. That is, a thread for which some part
     * of its call stack currently exists on this machine.
     */
    private static class ActiveThreadInfo {

        /**
         * The <code>Thread</code> object that represents the projection of the
         * global thread onto this machine.
         */
        private final Thread threadProjection;

        /**
         * The address of the communicator immediately below the base of the
         * projection on the stack. Can be <code>null</code>, if we're at the
         * base.
         */
        private final CommunicationAddress projectedFrom;

        /**
         * The message queue for the thread. Used to pass messages to the thread
         * (they'll have been received by a networking thread, but should run on
         * the thread itself).
         */
        private final BlockingQueue<MessageEnvelope> messageQueue
                = new SynchronousQueue<>();

        /**
         * Whether this thread projection is currently in the act of sending the
         * reply that will cause it to cease to exist. Used to avoid race
         * conditions. Any access to this must be synchronised on the
         * <code>ActiveThreadInfo</code> object; that means that taking the
         * monitor of this object can be used to delay a shutdown.
         */
        private boolean shuttingDown = false;

        /**
         * Whether this thread projection's residual state may be important. If
         * <code>true</code>, it's treated as potentially important (although it
         * may subsequently turn out not to be). If <code>false</code>, it's
         * known to be unimportant (typically because the communicator is
         * shutting down, and thus there will be no way to query the residual
         * state).
         * <p>
         * Access to this variable is synchronised by accessing it only from one
         * thread (specifically, <code>threadProjection</code>). To avoid race
         * conditions, it must not be accessed from other threads.
         */
        private boolean residualStateNecessary = true;

        /**
         * Creates a new structure to hold information about a given global
         * thread.
         *
         * @param threadProjection The projection of the global thread onto this
         * machine.
         * @param projectedFrom The address of the communicator that's
         * immediately below this one on the stack (i.e. the deepest stack entry
         * of the owning communicator is directly above some stack entry from
         * the specified communicator). Can be <code>null</code>, if we're at
         * the base of the stack.
         */
        private ActiveThreadInfo(Thread threadProjection,
                CommunicationAddress projectedFrom) {
            this.threadProjection = threadProjection;
            this.projectedFrom = projectedFrom;
        }

        /**
         * A message that a distributed communicator sends to itself to inform a
         * thread that its residual state is not required. (The "send-to-self"
         * behaviour is not done over the network, but directly via the thread's
         * message queue, and is used to ensure that this message is correctly
         * ordered with respect to other messages.) This will set the "residual
         * state necessary" flag for the thread to <code>false</code>.
         */
        private static class LocalThreadShutdownMessage
                implements DistributedMessage {

            @Override
            public void process(MessageEnvelope envelope,
                    DistributedCommunicator communicator,
                    boolean requestTracking) {
                GlobalID threadID = getCurrentThreadID(communicator.getMyAddress());
                communicator.activeThreadIDs.get(threadID).residualStateNecessary = false;
            }

            /**
             * Returns <code>true</code>, even though this message does not send
             * a reply. This message is inserted into the message-handling loop
             * as though it were synchronous, in order to get it into the right
             * place to cause a break out of that loop. As the message is sent
             * from a communicator to itself, no separate reply or blocking
             * needs to be implemented, but it needs to be called at an
             * appropriate location that it could be.
             *
             * @return <code>true</code>.
             */
            @Override
            public boolean sendsReply() {
                return true;
            }

            /**
             * Always returns <code>false</code>. This message needs to be
             * processed by a specific thread.
             *
             * @return <code>false</code>.
             */
            @Override
            public boolean lightweightSafe() {
                return false;
            }

            /**
             * Always returns <code>null</code>. This message is only sent once
             * per thread.
             *
             * @return <code>false</code>.
             */
            @Override
            public Duration periodic() {
                return null;
            }
        }
    }
}
