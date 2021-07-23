package xyz.acygn.mokapot.util;

import static java.lang.Thread.MIN_PRIORITY;
import static java.lang.Thread.NORM_PRIORITY;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static xyz.acygn.mokapot.util.BackgroundGarbageCollection.Operation.FINALIZE;
import static xyz.acygn.mokapot.util.BackgroundGarbageCollection.Operation.GARBAGE_COLLECT;

/**
 * A utility class for performing garbage collection and finalisation on a
 * background thread. This is used for "non-blocking" garbage collection and
 * finalisation requests. The intended use case is when we want to ensure that
 * something in particular gets garbage collected and/or finalised, and that the
 * collection/finalisation itself will be observable, but there might be a large
 * number of such requests coming in for different purposes in a short space of
 * time, and we don't want to run a separate garbage collection cycle for each.
 * <p>
 * This class also supports implementations of some other garbage collection
 * operations (such as an alternative finalisation mechanism for when Java's
 * full finalisation machinery is not required, and an ability for objects to
 * hold themselves alive).
 *
 * @author Alex Smith
 */
public class BackgroundGarbageCollection {

    /**
     * A garbage-collection-related operation that can be done in the
     * background.
     */
    public static enum Operation {
        /**
         * No operation. (Not very useful as an argument to
         * <code>perform</code>, but used internally to record the fact that no
         * operation is currently ongoing.)
         */
        NO_OPERATION(0),
        /**
         * A garbage collection operation.
         */
        GARBAGE_COLLECT(1),
        /**
         * A finalisation operation.
         */
        FINALIZE(2),
        /**
         * A garbage collection operation, followed by a finalisation.
         */
        GC_THEN_FINALIZE(3);

        /**
         * A number associated with the operation. We can't store enum values in
         * an atomic directly, so we use integers as substitutes. This number is
         * a bitfield; x|y represents performing both operation x and operation
         * y.
         */
        private final int operationCode;

        /**
         * Enumeration constant initialiser.
         *
         * @param operationCode The operation code to construct the constant
         * with.
         */
        private Operation(int operationCode) {
            this.operationCode = operationCode;
        }

        /**
         * Returns a number representing this operation.
         *
         * @return The operation's code.
         */
        final int getOperationCode() {
            return operationCode;
        }
    }

    /**
     * The keepalive map. Keys are object identities to keep alive; values are
     * the number of times they've been kept alive.
     */
    private static final Map<ObjectIdentity<?>, Integer> keepaliveMap
            = new HashMap<>();

    /**
     * Prevents a given object being deallocated until <code>endKeepalive</code>
     * is called on the object. Starts and ends of keepalives are paired, i.e.
     * if you start to keep an object alive twice, you must end the keepalive
     * twice before it can be deallocated.
     *
     * @param o The object to hold alive.
     */
    public static synchronized void startKeepalive(Object o) {
        keepaliveMap.merge(new ObjectIdentity<>(o), 1, (x, y) -> x + y);
    }

    /**
     * Ends an effect that prevents an object being deallocated. This needs to
     * be called once for each time such an effect was created. (This only
     * refers to effects created via <code>startKeepalive</code>, not standard
     * Java garbage collection mechanisms that might hold an object alive.)
     *
     * @param o The object that's being held alive.
     * @throws IllegalArgumentException If there is no existing effect that
     * prevents <code>o</code> being deallocated.
     */
    public static synchronized void endKeepalive(Object o)
            throws IllegalArgumentException {
        ObjectIdentity<?> oi = new ObjectIdentity<>(o);
        if (keepaliveMap.remove(oi, 1)) {
            /* It was in there exactly once. */

        } else {
            int oldCount = keepaliveMap.getOrDefault(oi, 0);
            if (oldCount <= 0) {
                throw new IllegalArgumentException(
                        "Attempting to end a keepalive that wasn't started");
            }
            keepaliveMap.replace(oi, oldCount - 1);
        }
    }

    /**
     * The background garbage collection thread.
     */
    private static final Thread gcThread
            = new Thread(BackgroundGarbageCollection::threadBody,
                    "BackgroundGarbageCollection thread");

    static {
        gcThread.setDaemon(true);
        gcThread.setPriority((MIN_PRIORITY + NORM_PRIORITY) / 2);
        gcThread.start();
    }

    /**
     * The set of pending operations to be carried out by the background garbage
     * collection thread. An operation is pending if a request to perform it has
     * arrived since the operation was most recently started.
     */
    private static final AtomicInteger operationsRequired
            = new AtomicInteger(0);

    /**
     * The queue on which a <code>FinalReference</code> appears once its
     * referent is deallocated.
     */
    private static final ReferenceQueue<Object> finaliserQueue
            = new ReferenceQueue<>();

    /**
     * Causes a given method to be run when a given object is deallocated.
     * (Note: the method in question will be held alive until the object in
     * question is deallocated, and thus will cause a keepalive loop if it
     * references the object itself. So you should ensure that <code>o</code> is
     * not accessible via <code>finaliser</code>.)
     * <p>
     * The method will run on the background garbage collection thread. It might
     * not run <i>immediately</i> upon the deallocation of the object, i.e. it
     * will run at some point in the future. It might not run at all if the
     * program exits before it has a chance to (i.e. the end of the program
     * won't be delayed simply to allow the finaliser to run).
     *
     * @param o The object which, when deallocated, will cause
     * <code>finaliser</code> to run.
     * @param finaliser The method that should be run once <code>o</code> has
     * been deallocated.
     */
    public static void addFinaliser(Object o, Runnable finaliser) {
        startKeepalive(new FinalReference(o, finaliser));
    }

    /**
     * A place we can store objects without the stores being optimised out.
     */
    private static volatile Object fenced = null;

    /**
     * Performs a volatile read of the given reference. This will force the
     * referenced object to remain allocated until the call is made, forming a
     * sort of fence against deallocation.
     *
     * @param o An object that must stay allocated until this method call.
     */
    public synchronized static void volatileAccess(Object o) {
        fenced = o;
        fenced = null;
    }

    /**
     * Ensures that the given garbage collection operation will be fully
     * performed at some point in the future. If the operation in question is
     * already ongoing, it will therefore be rerun after it ends. However, if
     * multiple similar requests can be coalesced into a single request, they
     * will be (e.g. making two calls to this method to request a garbage
     * collection while a third garbage collection is ongoing will only run one
     * rather than two more garbage collections after that).
     *
     * @param operation The operation to perform.
     */
    public static void perform(Operation operation) {
        int code = operation.getOperationCode();

        operationsRequired.getAndAccumulate(code, (x, y) -> x | y);
        gcThread.interrupt();
        /* in case it's currently blocked */
    }

    /**
     * Implementation of the background thread. Repeatedly performs pending
     * operations, un-pending them in the process, until there are no more
     * pending operations.
     */
    private static void threadBody() {
        final int gcCode = GARBAGE_COLLECT.getOperationCode();
        final int fCode = FINALIZE.getOperationCode();

        while (true) {
            /* Remove a GC operation from the set of pending operations, if
               there is one. If we can, perform it. */
            int oldCode = operationsRequired.getAndUpdate((x) -> x & ~gcCode);
            if ((oldCode & gcCode) != 0) {
                System.gc();
            }

            /* If a finalize operation is the <i>only</i> pending operation,
               perform it and remove it from the set. (This ensures that
               GC_THEN_FINALIZE has the intended semantics; if both are pending
               at the same time, the garbage collection will always happen
               first, and if we start the finalisation and then get a new
               pending operation, we still obeyed the contracts of any previous
               finalisation requests, and the new ones will then run according
               to their own semantics.) */
            if (operationsRequired.compareAndSet(fCode, 0)) {
                System.runFinalization();
            }

            try {
                /* Process a reference from the reference queue, or block until
                   one is available.  */
                Reference<?> o = finaliserQueue.remove();

                FinalReference oCast = (FinalReference) o;
                endKeepalive(o);
                oCast.finaliser.run();

            } catch (InterruptedException ex) {
                /* We got a request to do a GC operation; do that now (via
                   falling through to the end of the loop and thus returning to
                   the start), and we'll go back to processing the finaliser
                   queue afterwards. */
            }
        }
    }

    /**
     * Inaccessible constructor. This is a utility class not meant to be
     * instantiated.
     */
    private BackgroundGarbageCollection() {
    }

    /**
     * A phantom reference together with a finaliser for the object it's
     * referencing.
     */
    private static class FinalReference extends PhantomReference<Object> {

        /**
         * The code that should be run upon removing this object from the
         * reference queue.
         */
        private final Runnable finaliser;

        /**
         * Constructs a final reference for a given referent and finaliser.
         *
         * @param referent The object which, when it is deallocated, causes the
         * final reference to be enqueued.
         * @param finaliser A Runnable which the thread which dequeues the final
         * reference should run upon dequeueing it.
         */
        private FinalReference(Object referent, Runnable finaliser) {
            super(referent, finaliserQueue);
            this.finaliser = finaliser;
        }
    }
}
