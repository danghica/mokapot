package xyz.acygn.mokapot.util;

import static java.lang.System.identityHashCode;

/**
 * An object that represents another object's identity; that is, the value of
 * the object's identity is the object itself. In other words, comparing two
 * object identities for equality will succeed only if the objects are the same
 * object (whereas comparing two objects for equality can succeed if they are
 * different objects with the same value).
 *
 * You can think of this like the <code>&</code> operator in C; value equality
 * of identities is the same as reference equality of the referenced object,
 * copying an identity doesn't copy the underlying object, and so on. Likewise,
 * you can think of this class as representing the value of a reference.
 *
 * Unlike in C, however, the object will not be garbage collected as long as the
 * object identity exists (thus ensuring that the identity will remain unique
 * and not reused for some other object).
 *
 * The purpose of this class is to allow maps to be created keyed on the
 * identities of objects, rather than the object itself (or equivalently, to
 * create a map that uses reference equality, not value equality, to compare
 * keys).
 *
 * <code>null</code> also has an identity (which is distinct from that of all
 * actual objects).
 *
 * @author Alex Smith
 * @param <T> A class which the object being referenced belongs to.
 */
public class ObjectIdentity<T> {

    /**
     * The object of which this class represents the identity.
     */
    private final T forObject;

    /**
     * Creates an object identity for the given object.
     *
     * @param forObject The object for which the identity should be created.
     */
    public ObjectIdentity(T forObject) {
        this.forObject = forObject;
    }

    /**
     * Returns the hash code that the object corresponding to this identity
     * would have, if its hashCode method were not overridden. This value is
     * guaranteed to be deterministic for any given object.
     *
     * @return The "default" hash code for the object.
     * @see System#identityHashCode(java.lang.Object)
     */
    @Override
    public int hashCode() {
        return identityHashCode(forObject);
    }

    /**
     * Checks to see if the given object is an object identity for the same
     * object as this object identity. Crucially, this is a reference equality
     * for the referenced objects (i.e. "changing the referent of one identity
     * will change the referent of the other identity"), rather than a value
     * equality for the referenced objects ("the referent of this identity and
     * that identity have the same value"); the latter allows an object to
     * compare equal to a copy of itself, the former doesn't. (It is, however, a
     * value equality when interpreted as comparing <code>ObjectIdentity</code>
     * objects; two different <code>ObjectIdentity</code> objects compare equal
     * if they refer to the same object.)
     *
     * @param obj An object to compare to.
     * @return true if <code>obj</code> is an object identity referring to the
     * same object as this object identity; false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        /* note: comparison on .dereference() is == not the usual .equals(),
           so we can't abbreviate this further */
        return ObjectUtils.equals(this, obj, (t, u)
                -> t.dereference() == u.dereference());
    }

    /**
     * Returns the object which has this identity.
     *
     * @return The object specified when constructing this object identity.
     */
    public T dereference() {
        return forObject;
    }

    /**
     * Returns a string representation of this object identity.
     *
     * @return A string representation of this object.
     */
    @Override
    public String toString() {
        return "[ObjectIdentity: " + forObject.toString() + "]";
    }
}
