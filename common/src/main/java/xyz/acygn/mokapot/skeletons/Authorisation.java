package xyz.acygn.mokapot.skeletons;

/**
 * A permission and/or capability to perform otherwise unsafe operations.
 * <p>
 * Some classes, such as those implementing <code>Standin</code>, have
 * invariants on their fields but don't know what they are (because Standin is
 * generic whereas this library makes a rather more specific use of it).
 * However, those invariants being maintained is important to the integrity of
 * the program. As such, methods that change fields in a way that might break
 * those invariants take an <code>Authorisation</code> argument, which informs
 * them (in a JVM-enforceable way) that the invariants have already been checked
 * and that the resulting operation is safe. (They will typically call
 * <code>Authorisation#verify</code> in order to do the check.)
 * <p>
 * In some cases, the invariants being enforced (e.g. that a <code>final</code>
 * field will not be altered) don't need to be manually checked, because they're
 * enforced by the JVM. An Authorisation object can also optionally serve as a
 * <i>capability</i>, a permission that consists of executable code that lifts a
 * restriction. This has the benefit that it's impossible to forget to check it;
 * attempting to use the capability will check that it exists as a side effect
 * (otherwise you just get a null pointer exception or the like).
 * <p>
 * The <code>Authorisation</code> class also has a rather different purpose; it
 * serves as a class that can be mentioned in an argument list in order to give
 * a method a globally unique name (as the classes of the arguments, and the
 * packages those classes belong to, are effectively part of the method name).
 * In this case, the object itself can be <code>null</code>, using the class
 * purely for its namespacing purposes (and a <code>null</code> authorisation is
 * effectively an authorisation with no permissions or capabilities, as
 * attempting to verify any of the permissions or capabilities in question will
 * give you a <code>NullPointerException</code>).
 * <p>
 * Authorisation objects may do things like verifying the call stack to ensure
 * that they're not misused, and thus should not be used in "user" code (i.e.
 * code that isn't trying to implement standins or to change internal fields
 * like the standin storage). This class is thus normally best interpreted as
 * private to this package and its parent package, even though it needs to be
 * public for technical reasons.
 *
 * @author Alex Smith
 */
public final class Authorisation {

    /**
     * Private constructor. Unlike most classes with only private constructors,
     * this class <i>is</i> meant to be instantiated. However, an Authorisation
     * effectively gives permission and/or capability to classes to do things
     * that arbitrary classes aren't allowed to do, and thus can only be
     * instantiated by classes that prove that they can already do things that
     * violate Java's invariants (such as calling a private constructor).
     */
    private Authorisation() {
    }

    /**
     * Verifies that this object is non-null and allows the access being
     * requested. At present, no access checks are made beyond the Authorisation
     * object itself existing.
     * <p>
     * Because calls to this method are injected into generated code, it's
     * important that its call pattern is as simple as possible in order to keep
     * the generated code simple and small; as such, it has no return value, no
     * parameters, and throws no checked exceptions. Should this method need in
     * future to determine the reason it's being called, it would need to do
     * that via inspection of the call stack.
     *
     * @throws SecurityException If there is a reason to deny the access being
     * requested; at present, this never happens
     */
    public void verify() throws SecurityException {
    }

    /**
     * Object representing the capability to create an object without using a
     * constructor. (This object contains a method that you can invoke to
     * actually create an object.) Might be <code>null</code>, if the
     * Authorisation does not have this capability.
     */
    public final ClassInstantiator classInstantiator = null;

    /**
     * Returns the capability to create an object without using a constructor.
     *
     * @return The capability in question.
     * @throws SecurityException If this authorisation does not have the
     * capability in question
     */
    public ClassInstantiator getClassInstantiator() throws SecurityException {
        if (classInstantiator == null) {
            throw new SecurityException("no capability to instantiate classes");
        }

        return classInstantiator;
    }

    /**
     * Object representing the capability to call <code>Object#clone</code> from
     * the wrong package. Might be <code>null</code>, if the Authorisation does
     * not have this capability.
     */
    public final ObjectCloner objectCloner = null;

    /**
     * Returns the capability to call <code>Object#clone</code> from the wrong
     * package.
     *
     * @return The capability in question.
     * @throws SecurityException If this authorisation does not have the
     * capability in question
     */
    public ObjectCloner getObjectCloner() throws SecurityException {
        if (objectCloner == null) {
            throw new SecurityException("no capability to clone objects");
        }

        return objectCloner;
    }

    /**
     * Interface representing the capability of creating an object without the
     * use of its constructor.
     */
    @FunctionalInterface
    public static interface ClassInstantiator {

        /**
         * Creates a new object of a given class, without calling its
         * constructor.
         *
         * @param <T> The class to instantiate.
         * @param ofClass <code>T.class</code>, given explicitly due to Java's
         * type erasure rules.
         * @return A new object of class <code>T</code>, for which no
         * constructor has been called.
         */
        <T> T instantiate(Class<T> ofClass);
    }

    /**
     * Interface representing the capability of calling
     * <code>Object#clone</code>, despite the fact that this method is
     * <code>protected</code>.
     */
    public static interface ObjectCloner {

        /**
         * Calls <code>Object#clone</code> on the given object. This is a
         * virtual call, i.e. if the object overrides <code>Object#clone</code>,
         * the overriding method will be called.
         *
         * @param original The object to clone.
         * @return The cloned object.
         * @see Object#clone()
         * @throws CloneNotSupportedException If <code>Object#clone</code>
         * throws a <code>CloneNotSupportedException</code> (typically because
         * <code>original</code> is not <code>Cloneable</code>)
         */
        Object cloneObject(Object original) throws CloneNotSupportedException;
    }
}
