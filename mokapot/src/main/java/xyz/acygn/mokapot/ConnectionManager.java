package xyz.acygn.mokapot;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import static xyz.acygn.mokapot.ClassKnowledge.knowledgeForClass;
import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;
import xyz.acygn.mokapot.util.DataByteBuffer;
import xyz.acygn.mokapot.util.DeterministicAutocloseable;
import xyz.acygn.mokapot.util.Expirable;
import xyz.acygn.mokapot.util.ExpirableAlternatives;
import xyz.acygn.mokapot.util.ExpirableMap;
import xyz.acygn.mokapot.util.Holder;
import xyz.acygn.mokapot.util.ServerSocketLike;
import xyz.acygn.mokapot.util.SocketLike;
import xyz.acygn.mokapot.wireformat.FakeDescriptionStream;
import xyz.acygn.mokapot.wireformat.ObjectDescription;

/**
 * A thread that deals with incoming connections and with tracking connections
 * between one distributed communicator and another.
 *
 * @author Alex Smith
 */
class ConnectionManager {

    /**
     * A connection startup code requesting that the connection is intended to
     * be persistent and used to send messages.
     */
    final static int PERSISTENT_CODE = 0x1;

    /**
     * A connection startup code requesting that the connection is a one-off
     * connection to ask the recipient their address.
     */
    final static int ADDRESS_LOOKUP_CODE = 0x2;

    /**
     * A connection startup code sent by the manager to itself to shut itself
     * down.
     */
    final static int CONNECTION_MANAGER_SHUTDOWN_CODE = 0x3;

    /**
     * Converts a <code>MessageEnvelope</code> into the sequence of bytes that
     * represents it on the network.
     * <p>
     * Because a <code>MessageEnvelope</code> contains a
     * <code>MarshalledDescription</code>, which has special garbage collection
     * properties, this conversion can typically only be done once. However, the
     * resulting stream of bytes is just a stream of bytes, and thus can validly
     * be sent multiple times as long as it's only received (or only processed)
     * once.
     *
     * @param envelope The <code>MessageEnvelope</code> to convert.
     * @return <code>MESSAGE_CODE</code> followed by the serialised form of that
     * <code>MessageEnvelope</code>
     * @throws IOException If something goes wrong in the conversion
     */
    static byte[] encodeMessage(MessageEnvelope envelope) throws IOException {
        ClassKnowledge<MessageEnvelope> knowledge = knowledgeForClass(MessageEnvelope.class);
        ObjectDescription.Size descriptionSize = knowledge.descriptionSize(() -> envelope, false);
        assert (descriptionSize.getObjectCount() == 0);
        byte[] dataBytes = new byte[descriptionSize.getByteCount() + 1];
        DataByteBuffer dbb = new DataByteBuffer(dataBytes, false);
        dbb.writeByte(Connection.MESSAGE_CODE);
        knowledge.writeFieldDescriptionTo(dbb, envelope, false);
        return Arrays.copyOf(dataBytes, dbb.getWrittenLength());
    }

    /**
     * The distributed communicator that this manager is managing connections
     * for.
     */
    private final DistributedCommunicator communicator;

    /**
     * The socket on which we listen for new connections. Can be
     * <code>null</code>, if we aren't listening.
     */
    private final ServerSocketLike listenSocket;

    /**
     * The bytes that form the described form of this connection's communication
     * address.
     */
    private final byte[] addressBytes;

    /**
     * The set of all connections being managed by this connection manager.
     * <p>
     * Outer keys are remote addresses, inner keys are connection IDs.
     */
    private final ExpirableMap<CommunicationAddress, ExpirableAlternatives<GlobalID, Connection>> connections
            = new ExpirableMap<>(DistributedCommunicator.CONNECTION_TIMEOUT,
                    TimeUnit.SECONDS);

    /**
     * The keepalive that holds the distributed communicator alive while its
     * acceptor is running.
     */
    private DeterministicAutocloseable acceptorKeepalive;

    /**
     * The keepalive that holds the distributed communicator alive while it's
     * had any connections in its connection pool. (This is because a thread is
     * used to expire them, and we need to shut down the thread.)
     */
    private DeterministicAutocloseable connectionsKeepalive;

    /**
     * Creates a new connection manager.
     *
     * @param communicator The communicator that this manager manages
     * connections for.
     * @throws IOException If something goes wrong creating the socket to listen
     * on
     */
    ConnectionManager(DistributedCommunicator communicator) throws IOException {
        this.communicator = communicator;
        ServerSocketLike newListenSocket;

        try {
            newListenSocket = communicator.getEndpoint().newListenSocket();
        } catch (UnsupportedOperationException ex) {
            /* Maybe it can't listen. */
            newListenSocket = null;
        }
        this.listenSocket = newListenSocket;

        addressBytes = new Marshalling(communicator).describeToByteArray(
                communicator.getMyAddress());

        try {
            acceptorKeepalive = communicator.maybeGetKeepaliveLock(
                    ShutdownStage.ACCEPTOR, "acceptor");
        } catch (AutocloseableLockWrapper.CannotLockException ex) {
            /* The connection manager is only meant to be created once, just
               after all the locks have been dropped, so something's gone
               badly wrong if we can't take the lock here. */
            throw new RuntimeException(ex);
        }
    }

    /**
     * If necessary, starts this connection manager's listen loop.
     *
     * This should be called exactly once, immediately after the constructor.
     * (It's a separate method to avoid creation of threads referencing
     * partially constructed objects.)
     */
    void startListenLoop() {
        if (listenSocket != null) {
            new Thread(this::listenLoop,
                    "Connection acceptor thread, address = "
                    + communicator.getMyAddress()).start();
        }
    }

    /**
     * Shuts down all connections going via this connection manager.
     */
    synchronized void shutdownAllConnections() {
        connections.clear();
        if (connectionsKeepalive != null) {
            connectionsKeepalive.close();
            connectionsKeepalive = null;
        }
    }

    /**
     * Ends the listen loop (if necessary), and drops the keepalive on the
     * communicator once the loop has ended.
     * <p>
     * This is called during shutdown of the distributed communicator, and might
     * not drop the keepalive immediately because the listen loop is ended
     * asynchronously (i.e. this method can return before it performs the
     * desired actions, subsequently carrying them out in the background).
     */
    synchronized void endListenLoop() {
        if (listenSocket == null) {
            if (acceptorKeepalive != null) {
                acceptorKeepalive.close();
                acceptorKeepalive = null;
            }
        } else if (acceptorKeepalive != null) {
            try {
                SocketLike stopSocket;
                try {
                    stopSocket = communicator.getEndpoint().newConnection(
                            communicator.getMyAddress());
                } catch (CommunicationEndpoint.IncompatibleEndpointException ex) {
                    stopSocket = communicator.getMyAddress().connectHere(
                            communicator.getMyAddress());
                }
                OutputStream os = stopSocket.getOutputStream();
                os.write(CONNECTION_MANAGER_SHUTDOWN_CODE);
                os.flush();
            } catch (IOException | CommunicationEndpoint.IncompatibleEndpointException ex) {
                /* The endpoint can't send to, or isn't compatible with, itself? */
                throw new DistributedError(ex,
                        "endpoint " + communicator.getEndpoint()
                        + " cannot send to itself");
            }
        }
    }

    /**
     * Implements listening for connections. This method is intended to be run
     * as the body of a thread of its own.
     * <p>
     * Multiple types of incoming connections are supported, based on the first
     * byte read from them. Most obvious is the "I want to create a permanent
     * connection" starting 0x1. However, there's also a "what is your address?"
     * connection starting 0x2, which will be answered and then immediately
     * dropped. 0x3 tells the listen loop to exit, and is typically used by the
     * connection manager itself to shut down its own listen loop (which is more
     * reliable than trying to use <code>interrupt()</code>, which could hit
     * anywhere).
     * <p>
     * This method should only be called if <code>listenSocket != null</code>.
     */
    private void listenLoop() {
        int failCount = 0;
        while (true) {
            SocketLike socket;
            try {
                socket = listenSocket.accept();
                failCount = 0;
                communicator.getEndpoint().initialVerifySocket(socket);
            } catch (IOException ex) {
                /* TODO: check if ex is an auth failure specifically, if so we
                   don't want to increment failCount and may want to use a
                   different way to notify of the exception, because
                   unauthorised users could cause that failure mode intentionally */
                communicator.asyncExceptionHandler(ex);
                failCount++;
                /* Has something gone wrong with our listening ability? */
                if (failCount == 10) {
                    communicator.sendWarning(
                            "Communicator listen loop has 10 unexpected "
                            + "failures in a row, ending it");
                    return;
                }
                continue;
            }

            try {
                /* Handle whatever the request on the incoming socket is.

                   Note: we assume that the other end of the connection will be
                   polite and actually send something. We authed it, so we can
                   assume that it'll behave in an appropriate manner. */
                InputStream is = socket.getInputStream();
                int command = is.read();
                switch (command) {
                    case PERSISTENT_CODE:
                        FakeDescriptionStream ids
                                = new FakeDescriptionStream(is);
                        /* The other end of the connection sends its address,
                           and a unique ID for the connection. */
                        CommunicationAddress remoteAddress
                                = (CommunicationAddress) Marshalling.rCAOStatic(
                                        ids, null, CommunicationAddress.class);
                        GlobalID connectionID
                                = (GlobalID) Marshalling.rCAOStatic(ids, null,
                                        GlobalID.class);
                        try {
                            /* socket came from a ServerSocketLike, so must
                               necessarily need a listen loop */
                            registerConnection(new Connection(
                                    socket, connectionID,
                                    communicator, remoteAddress, true));
                        } catch (AutocloseableLockWrapper.CannotLockException ex) {
                            socket.getOutputStream().write(
                                    Connection.CANNOT_CONNECT_CODE);
                        }
                        /* Don't close the socket! */
                        continue;
                    case ADDRESS_LOOKUP_CODE:
                        OutputStream os = socket.getOutputStream();
                        os.write(addressBytes);
                        break;
                    case CONNECTION_MANAGER_SHUTDOWN_CODE:
                        listenSocket.close();
                        socket.close();
                        synchronized (this) {
                            if (acceptorKeepalive != null) {
                                acceptorKeepalive.close();
                                acceptorKeepalive = null;
                            }
                        }
                        return;
                    case -1:
                        throw new EOFException(
                                "EOF on inbound socket after connect");
                    default:
                        communicator.sendWarning(
                                "Unexpected remote command " + command
                                + " during connect, from "
                                + socket.getAddressDescription()
                                + ", on " + communicator.getMyAddress());
                }
                socket.close();
            } catch (IOException | DistributedError | ClassCastException ex) {
                try {
                    socket.close();
                } catch (IOException ex1) {
                    /* just report the initial exception, the failure to close
                       may have been caused by it */
                }
                communicator.asyncExceptionHandler(ex);
            }
        }
    }

    /**
     * Sends the given message to the given remote communicator. There does not
     * need to be an existing connection to the communicator; one will be
     * created if required.
     *
     * @param envelope The envelope containing the message to send.
     * @param target The address of the communicator to send it to.
     * @throws IOException If no existing connection functions, and there was a
     * failure to create a new one
     * @throws CommunicationEndpoint.IncompatibleEndpointException If the local
     * communicator's endpoint is incompatible with the remote communicator's
     * endpoint (e.g. they have no protocols in common)
     */
    void sendMessageTo(MessageEnvelope envelope,
            CommunicationAddress target) throws IOException,
            CommunicationEndpoint.IncompatibleEndpointException {
        byte[] encoded = encodeMessage(envelope);

        /* Can we send it using an existing connection? */
        Holder<Boolean> sent = new Holder<>(Boolean.FALSE);
        connections.runMethodOn(target,
                (alt) -> alt.callOnSomething((c) -> {
                    try {
                        c.sendMessage(encoded);
                        sent.setValue(Boolean.TRUE);
                    } catch (IOException ex) {
                        /* Treat the connection as though it were shut down,
                           and try a different connection. */
                        throw Expirable.ExpiredException.SINGLETON;
                    }
                }), ExpirableAlternatives::new);

        /* No, we'll have to create a new one. Note that we need to make sure
           that the message is sent successfully before we add the new
           connection to our set of possible connections; if not, we face a
           potential thread leak and quadratic slowdown, due to trying an
           ever-increasing number of failing connections with each new message
           attempt. */
        if (!sent.getValue()) {
            SocketLike socket;
            try {
                socket = communicator.getEndpoint().newConnection(target);
            } catch (CommunicationEndpoint.IncompatibleEndpointException ex) {
                try {
                    socket = target.connectHere(target);
                } catch (CommunicationEndpoint.IncompatibleEndpointException ex1) {
                    throw ex;
                }
            }
            try {
                OutputStream os = socket.getOutputStream();
                GlobalID id = new GlobalID(communicator.getMyAddress());
                boolean secondary = socket instanceof SecondaryEndpoint.PairedStreamSocketLike;
                if (!secondary) {
                    os.write(PERSISTENT_CODE);
                    os.write(addressBytes);
                    os.write(new Marshalling(communicator).describeToByteArray(id));
                    os.flush();
                }
                Connection c = new Connection(
                        socket, id, communicator, target, !secondary);
                if (socket instanceof SecondaryEndpoint.PairedStreamSocketLike) {
                    Connection otherC = ((SecondaryEndpoint.PairedStreamSocketLike) socket).getLinkedConnection();
                    c.setPartner(otherC);
                    otherC.setPartner(c);
                }
                try {
                    c.sendMessage(encoded);
                    registerConnection(c);
                } catch (Expirable.ExpiredException ex) {
                    /* This can only happen due to the remote side of the
                       system immediately rejecting the connection. */
                    throw new IOException("remote system rejected the message");
                } catch (IOException ex) {
                    try {
                        /* Don't let the connection survive unregistered, it'd
                           probably leak something. */
                        c.expire();
                    } catch (Expirable.ExpiredException ex1) {
                        /* OK, it wasn't going to live anyway. */
                    }
                    throw (ex);
                }
            } catch (AutocloseableLockWrapper.CannotLockException ex) {
                /* We shouldn't be creating connections after we've already
                   shut everything connection-requiring down... */
                socket.close();
                throw new DistributedError(ex, "new sendMessage connection");
            } catch (IOException ex) {
                socket.close();
                throw ex;
            }
        }
    }

    /**
     * Adds a new connection to the pool of connections being managed by this
     * connection manager. The connection will be added to the set of
     * connections considered for sending messages. Additionally, the
     * connection's listen loop will be started, if it needs one.
     *
     * @param connection The connection to register.
     * @throws AutocloseableLockWrapper.CannotLockException If the connection
     * manager is shutting down and does not accept new connections
     */
    void registerConnection(Connection connection)
            throws AutocloseableLockWrapper.CannotLockException {
        synchronized (this) {
            /* Take care not to leak the thread inside the ExpirableMap used
               to hold the connections. */
            if (connectionsKeepalive == null) {
                connectionsKeepalive = communicator.maybeGetKeepaliveLock(
                        ShutdownStage.CONNECTION, "connection timeout thread");
            }
        }

        connections.runMethodOn(connection.getRemoteAddress(),
                (alt) -> alt.add(connection.getConnectionID(), connection),
                () -> {
                    ExpirableAlternatives<GlobalID, Connection> ea;
                    ea = new ExpirableAlternatives<>();
                    try {
                        ea.add(connection.getConnectionID(), connection);
                    } catch (Expirable.ExpiredException ex) {
                        /* it can't have expired already; we only just created
                           it, and it doesn't expire over time until it's placed
                           into an ExpirableMap */
                        throw new RuntimeException(ex);
                    }
                    return ea;
                });
        connection.startListenLoop();
    }
}
