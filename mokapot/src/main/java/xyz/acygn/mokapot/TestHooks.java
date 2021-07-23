package xyz.acygn.mokapot;

import java.io.IOException;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * Routines for testing the internals of the distributed communication library.
 * You can request an implementation of this interface using
 * <code>DistributedCommunicator#getTestHooks()</code>, as long as doing so was
 * enabled sufficiently early.
 * <p>
 * The methods here are mostly involved with converting Java objects into the
 * byte sequences that represent those objects, and vice versa.
 *
 * @author Alex Smith
 * @see DistributedCommunicator#getTestHooks()
 */
public interface TestHooks {

    /**
     * Create a description of the given object, not including the class it
     * belongs to. (A description is a list of the object's fields, storing
     * copiable data in serialised form but noncopiable data as Java references.
     * Note that this is a shallow copy, not a reference copy, i.e. even if the
     * object would normally be described as a single reference, it'll be
     * described to depth 1.) This will not use a nullable format (that is, no
     * space will be reserved as a representation of <code>null</code>).
     *
     * @param obj The object to describe. Must not be <code>null</code>.
     * @return A read-only description of that object, whose read pointer is
     * rewound to the start.
     */
    ReadableDescription describe(Object obj);

    /**
     * Create a description of a hypothetical field containing the given object,
     * including its class. (This will have identical semantics to a reference
     * copy of the object, and may well actually simply be a reference copy of
     * the object, although it will be a shallow copy in cases where the object
     * is <code>Copiable</code>. Or to put it another way, the object itself is
     * not described unless it would naturally be inlined into a parent object
     * containing it; rather, the reference to the object is described.)
     *
     * @param obj The object to describe.
     * @param declaredType The declared type of the hypothetical field in
     * question. Can be <code>null</code> if no such field actually exists. This
     * information must be available when reproducing the description.
     * @return A read-only description of that object, whose read pointer is
     * rewound to the start.
     */
    ReadableDescription describeField(Object obj, Class<?> declaredType);

    /**
     * Produces a long reference to a given object. Only works on objects to
     * which long references would naturally be formed as a result of
     * unmarshalling.
     * <p>
     * The long reference will be created as an inheritance-based standin that
     * does not have stored-in-self status (and will use the specified object as
     * the storage). Note that "referent" here refers to the referent of the
     * long reference; the standin will have no referent (because a standin's
     * referent is used only when the standin is storing its data itself).
     *
     * @param <T> The declared type of the object to create a long reference to.
     * @param referent The object to create a long reference to.
     * @return A long reference to the object.
     * @throws IllegalArgumentException If the object is not of a class that is
     * marshalled via long references
     */
    @SuppressWarnings(value = "unchecked")
    <T> T makeLongReference(T referent) throws IllegalArgumentException;

    /**
     * Reads a class name description from the copiable portion of a
     * description, then reads an object of that class from that description.
     * The way in which the object is read will depend on the class name that is
     * read.
     *
     * @param description The description to read from.
     * @param parent The object that will contain the object being read; used to
     * resolve self-references.
     * @param expected The class which was expected, when marshalling this
     * object, to be the most likely class that the unmarshal process would
     * believe the object to have. This is normally the declared type of the
     * field in which the resulting object will be stored.
     * @return The object that was read.
     * @throws IOException If corruption of the description is detected (note
     * that not all corruption can be detected); or if there is an I/O error
     * trying to read data from <code>description</code>
     */
    Object readClassAndObject(ReadableDescription description, Object parent,
            Class<?> expected) throws IOException;

    /**
     * Creates and starts a secondary communicator with the given name. The same
     * name should not be used more than once in calls to this method, but the
     * name can otherwise be an arbitrary string.
     * <p>
     * There are several restrictions when running multiple communicators on the
     * same Java virtual machine. One is that each thread on the Java virtual
     * machine must interact with only one communicator; if the thread was
     * started by a communicator (e.g. because that communicator created it as a
     * projection of a thread on a remote machine to handle a remote request),
     * it may interact only with that communicator, otherwise it may interact
     * only with the main communicator. The other restriction is that no
     * <code>static</code> data may be written by one thread and read by another
     * unless either a) they share a communicator or b) the data has primitive
     * or <code>Copiable</code> type. (In other words, there must be no way to
     * send data from one communicator to another "inside" the JVM, effectively
     * dividing the JVM into a different compartment for each communicator. All
     * communication between these must be via the communicators themselves.)
     * <p>
     * Note that the calling thread is interacting with the main communicator
     * via calling this method, and as such, cannot actually interact with the
     * created communicator; therefore, there would be no point in returning it.
     * Instead, the communication address of the new communicator is returned;
     * the new communicator can be controlled via <code>runRemotely</code> calls
     * made via its address. (In particular, that communicator can be stopped
     * via using <code>runRemotely</code> on its
     * <code>asyncStopCommunication</code> method.)
     *
     * @param name The unique name of the new communicator; can be arbitrary,
     * but names should not be reused.
     * @param debugMonitor The debug monitor to use with the new communicator.
     * (There are no currently guarantees about what thread this might run on;
     * it's best to do nothing but print from it.) May be <code>null</code> if a
     * debug monitor is not required (but most applications of secondary
     * communicators are for testing purposes, and thus will need one).
     * @return The communication address of the created communicator.
     * @throws IOException If there was a failure to establish communications
     * @see
     * DistributedCommunicator#runRemotely(xyz.acygn.mokapot.CopiableRunnable,
     * xyz.acygn.mokapot.CommunicationAddress)
     */
    CommunicationAddress createSecondaryCommunicator(
            String name, DebugMonitor debugMonitor)
            throws IOException;

    /**
     * Changes whether slow debugging operations are enabled for the
     * communicators on this Java virtual machine. Enabling slow debugging
     * operations can increase the accuracy of internal errors in case something
     * goes wrong, but can have very bad computational complexities (e.g.
     * quadratic or exponential when performing a task that should be linear).
     * <p>
     * Note: many operations that have slow debugging versions are not
     * internally tied to any particular communicator. As such, this is a
     * JVM-global setting, not a per-communicator setting.
     *
     * @param enableSlowDebugOperations Whether to enable the slow debug
     * operations (<code>true</code> to enable).
     */
    void setEnableSlowDebugOperations(boolean enableSlowDebugOperations);
}
