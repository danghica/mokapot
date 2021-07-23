package xyz.acygn.mokapot;

import java.io.DataOutput;
import java.lang.reflect.Method;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isFinal;
import xyz.acygn.mokapot.wireformat.ObjectDescription;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * As much knowledge as we can determine about classes that extend a given
 * interface. We can't really deduce much, but anything that can be deduced here
 * is relevant.
 *
 * @author Alex Smith
 * @param <T> The interface that this object collects knowledge about.
 */
class InterfaceKnowledge<T> extends ClassKnowledge<T> {

    /**
     * Whether it's possible that this interface could be satisfied by the
     * object resulting from a lambda expression.
     */
    private final boolean maybeLambda;

    /**
     * Creates an object storing knowledge about the given interface.
     *
     * @param about The interface to gather knowledge about.
     * @throws IllegalArgumentException If <code>about</code> is not an
     * interface.
     */
    InterfaceKnowledge(Class<T> about)
            throws IllegalArgumentException {
        super(about);
        if (!about.isInterface()) {
            throw new IllegalArgumentException(about + " is not an interface");
        }

        int methodCount = 0;
        for (Method m : about.getMethods()) {
            if (!isFinal(m.getModifiers())
                    && isAbstract(m.getModifiers())) {
                methodCount++;
            }
        }

        maybeLambda = methodCount <= 1;
    }

    /**
     * Guesses whether objects conforming to this interface would unmarshal
     * reliably. If the interface requires implementing at least two methods
     * (thus making it impossible to implement using a lambda object), assumes
     * that it would most likely unmarshal reliably (because it can't be either
     * of the two most common hard cases: an array, or a lambda). If the
     * interface is being used on a known class (as opposed to the type of a
     * field, which thus contains an object of unknown class), we don't need to
     * guess; we can just use the information from the superclass (and thus can
     * safely return true, as the caller will have more information than we do).
     * Otherwise, errs on the safe side, returning <code>false</code>.
     *
     * @param asField Whether this interface is being used as a field, as
     * opposed to on a known class
     * @return Whether the interface requires implementing at least two methods
     */
    @Override
    public boolean unmarshalsReliably(boolean asField) {
        return !asField || !maybeLambda;
    }

    /**
     * Throws an exception. We can't determine how we would marshal an object
     * based only on one interface it supports; we'd need to know its concrete
     * class.
     *
     * @return Never returns.
     * @throws UnsupportedOperationException Always
     */
    @Override
    public boolean unmarshalsAsCopy() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "One interface isn't enough information to unmarshal an object");
    }

    /**
     * Throws an exception. We can't determine how we would describe an object
     * based only on one interface it supports; we'd need to know its concrete
     * class.
     *
     * @param value Ignored.
     * @param nullable Ignored.
     * @return Never returns.
     * @throws UnsupportedOperationException Always
     */
    @Override
    ObjectDescription.Size descriptionSizeOfObject(
            T value, boolean nullable)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "One interface isn't enough information to describe an object");
    }

    /**
     * Throws an exception. We can't determine how we would describe an object
     * based only on one interface it supports; we'd need to know its concrete
     * class.
     *
     * @param sink Ignored.
     * @param fieldValue Ignored.
     * @param nullable Ignored.
     * @throws UnsupportedOperationException Always
     */
    @Override
    void writeFieldDescriptionTo(DataOutput sink, Object fieldValue,
            boolean nullable) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "One interface isn't enough information to describe an object");
    }

    /**
     * Throws an exception. There's no way we should get here, because a
     * description is meant to record the actual type of an object, and an
     * interface type cannot be an an actual type.
     *
     * @param description Ignored.
     * @param nullable Ignored.
     * @return Never returns normally.
     * @throws UnsupportedOperationException Always
     */
    @Override
    T reproduce(ReadableDescription description, boolean nullable)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "A description cannot have interface type because "
                + "the object's actual type is recorded");
    }
}
