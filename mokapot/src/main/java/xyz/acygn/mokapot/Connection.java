package xyz.acygn.mokapot;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import static xyz.acygn.mokapot.ClassKnowledge.knowledgeForClass;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;
import xyz.acygn.mokapot.util.DeterministicAutocloseable;
import xyz.acygn.mokapot.util.Expirable;
import xyz.acygn.mokapot.util.ResettableThreadLocal;
import xyz.acygn.mokapot.util.SocketLike;
import xyz.acygn.mokapot.wireformat.FakeDescriptionStream;

/**
 * A connection between two distributed communicators.
 *
 * For any given connection, there's a <code>Connection</code> object at both
 * ends of the connection (with "local"/"remote" fields swapped accordingly).
 * Connections are symmetrical, using both halves of the socket to be able to
 * both send and receive.
 * <p>
 * Connections are expirable; expiring them causes them to no longer be able to
 * <i>send</i> messages, and to tell the remote end of the connection to stop
 * sending messages along the connection, but they will continue processing
 * remote messages until the remote end of the connection signals that it's done
 * with the connection.
 *
 * @author Alex Smith
 */
class Connection implements Expirable {

    /**
     * A connection code requesting that the recipient process a message.
     */
    final static int MESSAGE_CODE = 0x1;

    /**
     * A connection code indicating that the connection that sent the code is
     * shutting down.
     */
    final static int SHUTDOWN_CODE = 0x3;

    /**
     * A connection code indicating that the connection could not be established
     * because the communicator that sent the code is shutting down.
     */
    final static int CANNOT_CONNECT_CODE = 0x4;

    /**
     * The socket (or equivalent) being used for the connection.
     */
    private final SocketLike socket;

    /**
     * A buffered wrapper around <code>socket</code>'s input stream.
     */
    private final InputStream bufferedSocketInputStream;

    /**
     * A unique identifier for this connection.
     */
    private final GlobalID connectionID;

    /**
     * The communicator at the local end of the connection.
     */
    private final DistributedCommunicator localCommunicator;

    /**
     * The communication address of the remote end of the connection.
     */
    private final CommunicationAddress remoteAddress;

    /**
     * Whether this connection has been expired locally and/or remotely. A value
     * of 1 indicates a local expiration; of 2 indicates a remote expiration (or
     * remote brokenness, which is treated the same way); of 3 indicates both.
     * (These are stored in the same atomic value in order to allow for atomic
     * changes to both at once.)
     * <p>
     * Anything that sets this from 0 to 1, or 2 to 3, must tell the local end
     * that we've expired.
     * <p>
     * Anything that sets this to 3 (from anything other than 3) must shut down
     * the connection and close the keepalive.
     */
    private final AtomicInteger expiredWhere = new AtomicInteger(0);

    /**
     * The connection object for the remote distributed communicator. Normally
     * <code>null</code> because it's on a different JVM. In the secondary
     * communicator case, both communicators are on the same JVM, so we can set
     * this field to make the objects aware of each other.
     * <p>
     * If this field is not <code>null</code>, the connection will not need
     * listening threads; rather, the two <code>Connection</code> objects at
     * each end of the connection will be able to pump each other manually.
     */
    private Connection partner = null;

    /**
     * A Lock that prevents two threads trying to drive the message pump at the
     * same time.
     */
    private final Lock messageReadLock = new ReentrantLock();

    /**
     * A Lock that prevents two threads trying to send messages at the same
     * time.
     */
    private final Lock messageWriteLock = new ReentrantLock();

    /**
     * The keepalive on the communicator, to prevent it from exiting while we
     * might still have messages to receive.
     * <p>
     * Lock release timing: this lock is released at the first moment that the
     * local end of the connection is expired, and also the remote end is
     * expired-or-broken.
     */
    private final DeterministicAutocloseable keepaliveLock;

    /**
     * Whether a listen loop is needed. This depends on the type of socket-like
     * being used to create the connection.
     */
    private final boolean needsListenLoop;

    /**
     * Creates a new object to represent a connection. The connection itself
     * must have been established (networking-wise) before calling this
     * constructor; the <code>Connection</code> object itself will just wait for
     * instructions relating to it.
     * <p>
     * The connection will not start communications immediately upon being
     * created; you need to call <code>startListenLoop()</code> for that. This
     * is typically done by <code>CommunicationManager#registerConnection</code>
     * when it's added to the map of connections that exist.
     *
     * @param socket The connected socket that the connection uses to
     * communicate.
     * @param connectionID A globally-unique identifier for the connection.
     * @param localCommunicator The communicator at the local end of the
     * connection.
     * @param remoteAddress The communication address at the remote end of the
     * connection.
     * @param needsListenLoop Whether a listen loop will be needed. (If this is
     * <code>false</code>, then <code>startListenLoop()</code> becomes a no-op.)
     *
     * @throws AutocloseableLockWrapper.CannotLockException If the given
     * communicator is late in the process of shutting down, and thus not
     * accepting new connections
     * @throws IOException If the socket could not be converted to streams
     */
    Connection(SocketLike socket, GlobalID connectionID,
            DistributedCommunicator localCommunicator,
            CommunicationAddress remoteAddress, boolean needsListenLoop)
            throws AutocloseableLockWrapper.CannotLockException, IOException {
        this.socket = socket;
        this.bufferedSocketInputStream
                = new BufferedInputStream(socket.getInputStream());
        this.connectionID = connectionID;
        this.localCommunicator = localCommunicator;
        this.remoteAddress = remoteAddress;
        this.keepaliveLock = localCommunicator.maybeGetKeepaliveLock(
                ShutdownStage.CONNECTION,
                "connection " + connectionID + " from " + remoteAddress);
        this.needsListenLoop = needsListenLoop;
    }

    /**
     * Returns a fixed, globally unique identifier for this connection.
     * <p>
     * The connection identifier is the same at each end of the connection, i.e.
     * this identifies the connection itself, not a specific end of it.
     *
     * @return The connection's ID.
     */
    public GlobalID getConnectionID() {
        return connectionID;
    }

    /**
     * Returns the communication address for the far end of the connection.
     *
     * @return The remote communicator's address.
     */
    public CommunicationAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Sets the <code>Connection</code> object that represents the other end of
     * this connection. Used by secondary communicators to create an entirely
     * local connection.
     * <p>
     * This method should be called symmetrically, i.e. called on each of the
     * two objects with the other as a parameter. It should, in this case, be
     * called before the first call to <code>sendMessage()</code>. (This can't
     * be made a constructor parameter because it creates a circular structure.)
     *
     * @param partner The other end of this connection.
     */
    void setPartner(Connection partner) {
        this.partner = partner;
    }

    /**
     * Sends the given sequence of bytes via this connection. The sequence must
     * consist of exactly one command (such as <code>MESSAGE_CODE</code> or
     * <code>SHUTDOWN_CODE</code>), plus all its associated data.
     *
     * @param dataBytes The bytes of the message to send.
     * @throws ExpiredException If this connection has been locally expired,
     * causing us to promise to the remote side that we wouldn't send along it
     * @throws IOException If something goes wrong sending the message
     */
    void sendMessage(byte[] dataBytes) throws ExpiredException, IOException {
        try (DeterministicAutocloseable ac
                = new AutocloseableLockWrapper(messageWriteLock, "send message")) {
            if ((expiredWhere.get() & 1) == 1) {
                throw ExpiredException.SINGLETON;
            }

            socket.getOutputStream().write(dataBytes);
        }

        socket.getOutputStream().flush();
        if (partner != null) {
            partner.handleOneMessage();
        }
    }

    /**
     * Marks the other end of the connection as expired or broken. If the local
     * end of the connection has also expired, closes the connection.
     */
    private void setRemoteExpiredOrBroken() {
        int oldState = expiredWhere.getAndUpdate((s) -> s | 2);

        if ((oldState & 2) == 2) {
            /* We had this bit set already; nothing to do. */
            return;
        }
        if (oldState == 0) {
            try {
                expire();
            } catch (ExpiredException ex) {
                /* Looks like it was expired concurrently; no harm done. */
            }
        } else {
            /* We must have just set the value from 1 to 3 (the only remaining
               possibility); shut down the connection. */
            keepaliveLock.close();
            try {
                socket.close();
            } catch (IOException ex1) {
                localCommunicator.asyncExceptionHandler(ex1);
            }
        }
    }

    /**
     * Tells the other end of the connection that it's closing, and stops any
     * further sending of messages. If the other end of the connection has also
     * expired, closes the connection.
     *
     * @throws xyz.acygn.mokapot.util.Expirable.ExpiredException
     */
    @Override
    public void expire() throws ExpiredException {
        int oldState;

        /* If we aren't expired yet, we need to send the expiry message
           regardless, so that the other end of the connection knows to break
           out of its listen loop, even if that end of the connection is the end
           that chose to break the connection. */
        try (DeterministicAutocloseable ac
                = new AutocloseableLockWrapper(
                        messageWriteLock, "send expiry message")) {
            /* Update the expiry state while the write lock is held, so that we
               don't end up sending the shutdown command in the middle of an
               unrelated write. */
            oldState = expiredWhere.getAndUpdate((s) -> s | 1);
            if ((oldState & 1) == 1) {
                /* We were expired already. */
                throw ExpiredException.SINGLETON;
            }

            try {
                socket.getOutputStream().write(SHUTDOWN_CODE);
                socket.getOutputStream().flush();

                if (partner != null) {
                    partner.handleOneMessage();
                }
            } catch (IOException ex) {
                localCommunicator.asyncExceptionHandler(ex);
            }
        }

        if (oldState == 2) {
            /* When both ends are expired, we close the socket and release
               the keepalive lock. (The keepalive lock is closed first, because
               closing the socket may throw an exception.) */
            keepaliveLock.close();
            try {
                socket.close();
            } catch (IOException ex) {
                localCommunicator.asyncExceptionHandler(ex);
            }
        }
    }

    /**
     * Starts a thread to call <code>handleOneMessage</code> in a loop. This
     * method does nothing if the listen loop is not required (because
     * <code>handleOneMessage</code> is being called synchronously).
     * <p>
     * This method must not be called once the connection has had a chance to
     * expire.
     */
    void startListenLoop() {
        if (needsListenLoop) {
            new PooledThread(this::autoHandleMessages).start(localCommunicator);
        }
    }

    /**
     * Calls <code>handleOneMessage</code> in a loop until something goes wrong
     * or the connection closes.
     */
    private void autoHandleMessages() {
        ResettableThreadLocal.setName(
                "Connection read thread " + connectionID + ", from address "
                + socket.getAddressDescription());

        /* loop as long as the remote side hasn't expired or broken */
        while ((expiredWhere.get() & 2) == 0) {
            handleOneMessage();
        }
        /* once the remote side has expired, we don't expect any more incoming
           messages, so we can exit the thread */
    }

    /**
     * Reads and handles one message from the read side of the socket.
     * <p>
     * This message can be, and quite possibly will be, called from arbitrary
     * threads at arbitrary times (including weird threads like the garbage
     * collector, and threads associated with the wrong communicator). However,
     * when called from places other than the thread created by
     * <code>autoHandleMessages</code>, there will always be a message ready to
     * read stored in a local buffer (thus there's no need to worry about
     * indefinite blocking except from the place that can handle it).
     * <p>
     * The message that terminates the connection (indicating a remote expiry)
     * has a special encoding, just a single 0x3 byte, to make it easier to
     * recognise and shut down the stream in response.
     */
    void handleOneMessage() {
        try (DeterministicAutocloseable ac
                = new AutocloseableLockWrapper(messageReadLock, "read message")) {
            InputStream is = bufferedSocketInputStream;
            int command = is.read();
            switch (command) {
                case MESSAGE_CODE:
                    /* handle message; the main case, code below */
                    break;
                case CANNOT_CONNECT_CODE:
                    /* connection never really existed; immediately shut down
                       both sides of it */
                    if (expiredWhere.getAndSet(3) != 3) {
                        keepaliveLock.close();
                        socket.close();
                    }
                    return;
                case SHUTDOWN_CODE:
                    /* remote connection is expired */
                    setRemoteExpiredOrBroken();
                    return;
                default:
                    localCommunicator.sendWarning(
                            "Unexpected remote command " + command
                            + " after connect, from "
                            + socket.getAddressDescription()
                            + " = " + remoteAddress
                            + ", on " + localCommunicator.getMyAddress());
                    setRemoteExpiredOrBroken();
                    return;
                case -1:
                    throw new EOFException();
            }
            FakeDescriptionStream fds = new FakeDescriptionStream(is);
            MessageEnvelope envelope
                    = knowledgeForClass(MessageEnvelope.class)
                            .reproduce(fds, false);
            localCommunicator.handleArrivingMessage(envelope);
        } catch (IOException | UnsupportedOperationException ex) {
            localCommunicator.asyncExceptionHandler(ex);
            setRemoteExpiredOrBroken();
        }
    }
}
