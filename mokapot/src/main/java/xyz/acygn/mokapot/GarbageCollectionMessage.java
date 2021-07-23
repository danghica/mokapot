package xyz.acygn.mokapot;

import java.time.Duration;
import xyz.acygn.mokapot.util.BackgroundGarbageCollection;

/**
 * A request to perform garbage collection and/or finalisation. These are sent
 * to a virtual machine as a whole (not to a specific thread), and are used when
 * one system is aware of a situation that requires extra garbage collection
 * effort (such as a communicator shutting down), in order to make the system
 * where the garbage actually is aware that it needs to perform extra work.
 *
 * @author Alex Smith
 */
class GarbageCollectionMessage extends AsynchronousMessage {

    /**
     * The garbage collection operation to perform.
     */
    private final BackgroundGarbageCollection.Operation operation;

    /**
     * Creates a request to perform the given garbage collection operation.
     *
     * @param operation The operation to perform.
     */
    GarbageCollectionMessage(BackgroundGarbageCollection.Operation operation) {
        this.operation = operation;
    }

    /**
     * Performs the appropriate garbage collection operation. This is just a
     * wrapper for <code>BackgroundGarbageCollection#perform</code>, which
     * handles the real work.
     *
     * @param communicator Ignored. The Java garbage collector is shared across
     * all distributed communicators on a virtual machine.
     *
     * @see
     * BackgroundGarbageCollection#perform(xyz.acygn.mokapot.util.BackgroundGarbageCollection.Operation)
     */
    @Override
    public void process(DistributedCommunicator communicator) {
        BackgroundGarbageCollection.perform(operation);
    }

    /**
     * Produces a string representation of this message. There isn't a whole
     * amount of detail in a GarbageCollectionMessage, so this is fairly simple.
     *
     * @return A GarbageCollectionMessage.
     */
    @Override
    public String toString() {
        return "perform VM maintenance operation: " + operation;
    }

    /**
     * Always returns true. Because this message simply gives instructions to
     * the background garbage collection thread, it can run very quickly, and is
     * simple enough to also decode quickly.
     *
     * @return <code>true</code>.
     */
    @Override
    public boolean lightweightSafe() {
        return true;
    }

    @Override
    public boolean isUnimportant() {
        return true;
    }

    @Override
    public Duration periodic() {
        return Duration.ofMillis(100);
    }
}
