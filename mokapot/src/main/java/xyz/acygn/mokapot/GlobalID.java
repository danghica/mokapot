package xyz.acygn.mokapot;

import java.util.Objects;
import xyz.acygn.mokapot.markers.Copiable;
import xyz.acygn.mokapot.util.ObjectUtils;
import xyz.acygn.mokapot.util.ResettableThreadLocal;

/**
 * An identifier that's unique across all machines involved in the distributed
 * system. New unique global IDs are produced via recording the communication
 * address of the VM they were created on, and a counter that ensures that IDs
 * generated on the same VM differ. (A timestamp is also stored, to deal with
 * situations in which a VM is restarted, thus causing the value of the counter
 * to be forgotten.)
 *
 * @author Alex Smith
 */
final class GlobalID implements Copiable {

    /**
     * The global ID for the current thread. Lazily initialised, because the
     * information needed to initialise it won't be available until the first
     * call to the getter method. Until that point, the value inside the
     * ThreadLocal will be <code>null</code>.
     *
     * @see #getCurrentThreadID()
     */
    private static final ResettableThreadLocal<GlobalID> CURRENT_THREAD_ID
            = new ResettableThreadLocal<>();

    /**
     * Returns the global thread ID for the currently executing thread. If the
     * currently executing thread does not have an ID yet (i.e. it was created
     * freshly on this machine, by something other than the distributed
     * communication code), a new ID will be created.
     *
     * @param address A communication address for a communicator running on the
     * current Java virtual machine. This is used to prevent two threads on
     * different Java virtual machines being created with the same thread
     * identifier.
     * @return The global thread ID of the currently executing thread.
     */
    public static GlobalID getCurrentThreadID(CommunicationAddress address) {
        GlobalID threadID = CURRENT_THREAD_ID.get();
        if (threadID == null) {
            threadID = new GlobalID(address);
            CURRENT_THREAD_ID.set(threadID);
        }

        return threadID;
    }

    /**
     * Sets the global thread ID for the currently executing thread. This should
     * only be run immediately after newly creating the thread, in order to
     * specify which global thread it is part of (and is used on threads which
     * are extensions of pre-existing global threads).
     *
     * @param id The new global thread ID for the current thread.
     */
    static void setCurrentThreadID(GlobalID id) {
        if (CURRENT_THREAD_ID.get() != null) {
            throw new IllegalStateException(
                    "Attempting to set a thread's global ID after creation");
        }
        CURRENT_THREAD_ID.set(id);
        ResettableThreadLocal.setName("global thread, ID " + id.toString());
    }

    /**
     * The hashCode of this object. Cached, because a) it's immutable (both
     * GlobalID and CommunicationAddress are immutable, or supposed to be); b)
     * it's fairly expensive to calculate; c) hash lookups on GlobalID are
     * fairly frequent.
     */
    private final int hashCache;

    /**
     * A counter that distinguishes between IDs generated on the same VM.
     */
    private final long counter;
    /**
     * The next value to be used for the counter.
     *
     * @see #counter
     */
    private static long nextCounter = 1;

    /**
     * A method of preventing clashes in nextCounter in situations where a
     * virtual machine was restarted (thus losing the value of nextCounter).
     */
    private final long generationTime;

    /**
     * The virtual machine's communication address, used to distinguish between
     * virtual machines.
     */
    private final CommunicationAddress address;

    /**
     * Creates a new, globally unique, global ID. This constructor is designed
     * for use during distributed communications, as the information it uses to
     * ensure global uniqueness is most easily available from the distributed
     * communicator.
     *
     * @param address The communication address of the current machine. Used to
     * distinguish machines uniquely.
     */
    GlobalID(CommunicationAddress address) {
        this.address = address;
        this.counter = nextCounter;
        nextCounter++;
        this.generationTime = System.currentTimeMillis();

        int hash = 5;
        hash = 97 * hash + (int) this.counter;
        hash = 97 * hash + (int) this.generationTime;
        hash = 97 * hash + Objects.hashCode(address);
        this.hashCache = hash;
    }

    /**
     * Value used to speed up comparison of GlobalIDs. Two equal IDs will always
     * return the same value. Two unequal IDs might return the same value, but
     * will normally return different values.
     *
     * @return A value such that <code>a.equals(b)</code> implies
     * <code>a.hashCode() == b.hashCode()</code>
     */
    @Override
    public int hashCode() {
        return hashCache;
    }

    /**
     * Field-wise comparison of this object and another object. If the other
     * object is not a GlobalID, returns false.
     *
     * @param other The object to compare with.
     * @return <code>true</code> if <code>obj</code> is this object, or
     * field-wise identical to it; otherwise <code>false</code>.
     */
    @Override
    public boolean equals(Object other) {
        return ObjectUtils.equals(this, other, (t) -> t.hashCache,
                (t) -> t.counter, (t) -> t.generationTime, (t) -> t.address);
    }

    /**
     * Produces a string representation of this global ID.
     *
     * @return A string representation of the ID.
     */
    @Override
    public String toString() {
        return counter + "," + generationTime + "@" + address;
    }
}
